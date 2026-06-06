package com.maogai.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final String SESSION_SMS_PHONE = "smsRegisterPhone";
    private static final String SESSION_SMS_CODE = "smsRegisterCode";
    private static final String SESSION_SMS_EXPIRES = "smsRegisterExpires";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Random random = new Random();
    private final Gson gson = new Gson();

    private String host = "https://gyytz.market.alicloudapi.com";
    private String path = "/sms/smsSend";
    private String appKey = "";
    private String appSecret = "";
    private String appCode = "";
    private String smsSignId = "2e65b1bb3d054466b82f0c9d125465e2";
    private String templateId = "908e94ccf08b4476ba6c876d13f084ad";
    private int minute = 5;

    public SmsService() {
        loadConfig();
    }

    public SmsSendResult sendRegisterCode(HttpServletRequest req, String phone) {
        String code = String.valueOf(100000 + random.nextInt(900000));
        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_SMS_PHONE, phone);
        session.setAttribute(SESSION_SMS_CODE, code);
        session.setAttribute(SESSION_SMS_EXPIRES, System.currentTimeMillis() + minute * 60_000L);

        if (appCode == null || appCode.trim().isEmpty()) {
            log.warn("SMS AppCode is empty, using mock verification code {}", code);
            return new SmsSendResult(true, "短信配置未填写，已进入本地测试模式。验证码：" + code, true, code);
        }

        HttpRequest request = buildRequest(phone, code);
        log.info("========== SMS API 请求 ==========");
        log.info("目标手机号: {}", phone);
        log.info("验证码: {}", code);
        log.info("请求URL: {}", request.uri());
        log.info("请求方法: {}", request.method());
        log.info("AppCode(前8位): {}...", appCode.length() > 8 ? appCode.substring(0, 8) : appCode);
        log.info("==================================");

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body();
            log.info("========== SMS API 响应 ==========");
            log.info("HTTP状态码: {}", response.statusCode());
            log.info("响应Body: {}", body);

            // 国阳云API在HTTP 200时也可能返回业务错误，需要解析body里的code字段
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                try {
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    String respCode = json.has("code") ? json.get("code").getAsString() : "";
                    String respMsg = json.has("msg") ? json.get("msg").getAsString() : "";
                    log.info("业务Code: {}, 业务Msg: {}", respCode, respMsg);
                    log.info("==================================");
                    if ("0".equals(respCode)) {
                        return new SmsSendResult(true, "验证码已发送，请查看手机短信。", false, null);
                    }
                    log.warn("SMS业务错误 code={} msg={}", respCode, respMsg);
                } catch (Exception parseEx) {
                    log.warn("解析SMS响应JSON失败: {}", parseEx.toString());
                    log.info("==================================");
                }
            } else {
                log.info("==================================");
            }
        } catch (Exception e) {
            log.error("SMS API 调用异常: {}", e.toString(), e);
        }

        // 真实API失败时回退到mock模式，确保注册流程不中断
        log.warn("SMS API 调用失败，回退到mock模式。验证码：{}", code);
        return new SmsSendResult(true, "短信服务暂不可用，已进入本地测试模式。验证码：" + code, true, code);
    }

    public boolean verifyRegisterCode(HttpServletRequest req, String phone, String code) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        Object savedPhone = session.getAttribute(SESSION_SMS_PHONE);
        Object savedCode = session.getAttribute(SESSION_SMS_CODE);
        Object expires = session.getAttribute(SESSION_SMS_EXPIRES);
        if (savedPhone == null || savedCode == null || expires == null) return false;
        if (!String.valueOf(savedPhone).equals(phone)) return false;
        if (!String.valueOf(savedCode).equals(code == null ? "" : code.trim())) return false;
        if (((Number) expires).longValue() < System.currentTimeMillis()) return false;

        session.removeAttribute(SESSION_SMS_PHONE);
        session.removeAttribute(SESSION_SMS_CODE);
        session.removeAttribute(SESSION_SMS_EXPIRES);
        return true;
    }

    private HttpRequest buildRequest(String phone, String code) {
        Map<String, String> querys = new LinkedHashMap<>();
        querys.put("mobile", phone);
        querys.put("param", "**code**:" + code + ",**minute**:" + minute);
        querys.put("smsSignId", smsSignId);
        querys.put("templateId", templateId);

        return HttpRequest.newBuilder()
                .uri(URI.create(host + path + "?" + toQueryString(querys)))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "APPCODE " + appCode)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
    }

    private String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(urlEncode(key)).append('=').append(urlEncode(value));
        });
        return sb.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void loadConfig() {
        try (InputStream is = SmsService.class.getClassLoader().getResourceAsStream("sms-config.properties")) {
            if (is == null) return;
            Properties props = new Properties();
            props.load(is);
            host = props.getProperty("sms.host", host).trim();
            path = props.getProperty("sms.path", path).trim();
            appKey = resolveSecret(props.getProperty("sms.app.key", ""));
            appSecret = resolveSecret(props.getProperty("sms.app.secret", ""));
            appCode = resolveSecret(props.getProperty("sms.app.code", ""));
            smsSignId = props.getProperty("sms.smsSignId", smsSignId).trim();
            templateId = props.getProperty("sms.templateId", templateId).trim();
            minute = Integer.parseInt(props.getProperty("sms.minute", String.valueOf(minute)).trim());
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to load SMS config, fallback to mock mode", e);
            appCode = "";
        }
    }

    private String resolveSecret(String configured) {
        String value = configured == null ? "" : configured.trim();
        if (value.startsWith("env:")) {
            return System.getenv(value.substring(4));
        }
        if (value.startsWith("system:")) {
            return System.getProperty(value.substring(7));
        }
        return value;
    }

    public String getAppKey() { return appKey; }
    public String getAppSecret() { return appSecret; }

    public static class SmsSendResult {
        private final boolean success;
        private final String message;
        private final boolean mock;
        private final String code;

        public SmsSendResult(boolean success, String message, boolean mock, String code) {
            this.success = success;
            this.message = message;
            this.mock = mock;
            this.code = code;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public boolean isMock() { return mock; }
        public String getCode() { return code; }
    }
}
