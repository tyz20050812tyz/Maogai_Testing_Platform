package com.maogai.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.maogai.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final Gson gson = new Gson();

    private String serviceType = "mock";
    private String customApiUrl = "";
    private String customApiKey = "";
    private String customModel = "qwen3-max-preview";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private static final Map<String, String> KNOWLEDGE_BASE = new LinkedHashMap<>();

    static {
        KNOWLEDGE_BASE.put("毛泽东思想", "毛泽东思想是马克思列宁主义在中国的运用和发展，是被实践证明了的关于中国革命和建设的正确理论原则和经验总结，是中国共产党集体智慧的结晶。");
        KNOWLEDGE_BASE.put("马克思主义中国化", "马克思主义中国化时代化，就是把马克思主义基本原理同中国具体实际相结合、同中华优秀传统文化相结合，用马克思主义立场观点方法研究和解决中国实际问题。");
        KNOWLEDGE_BASE.put("新民主主义", "新民主主义革命是无产阶级领导的、人民大众的、反对帝国主义、封建主义和官僚资本主义的革命。");
        KNOWLEDGE_BASE.put("社会主义改造", "我国社会主义改造主要包括对农业、手工业和资本主义工商业的社会主义改造，1956年底基本完成，标志着社会主义基本制度在我国确立。");
        KNOWLEDGE_BASE.put("邓小平理论", "邓小平理论围绕什么是社会主义、怎样建设社会主义这个根本问题展开，其精髓是解放思想、实事求是。");
        KNOWLEDGE_BASE.put("三个代表", "三个代表重要思想强调中国共产党始终代表中国先进生产力的发展要求，代表中国先进文化的前进方向，代表中国最广大人民的根本利益。");
        KNOWLEDGE_BASE.put("科学发展观", "科学发展观第一要义是发展，核心立场是以人为本，基本要求是全面协调可持续，根本方法是统筹兼顾。");
        KNOWLEDGE_BASE.put("习近平新时代", "习近平新时代中国特色社会主义思想的核心要义是坚持和发展中国特色社会主义。");
    }

    public AIService() {
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream is = AIService.class.getClassLoader()
                .getResourceAsStream("ai-config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                serviceType = props.getProperty("ai.service.type", "mock").trim();
                customApiUrl = props.getProperty("ai.custom.api.url", "").trim();
                customApiKey = resolveSecret(props.getProperty("ai.custom.api.key", ""));
                customModel = props.getProperty("ai.custom.model", "qwen3-max-preview").trim();
            }
        } catch (IOException e) {
            log.warn("加载 AI 配置失败，使用 mock 模式", e);
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
        if (!value.isEmpty()) {
            return value;
        }
        String envKey = System.getenv("DASHSCOPE_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }
        return System.getProperty("DASHSCOPE_API_KEY", "");
    }

    public String ask(String question) {
        if ("mock".equalsIgnoreCase(serviceType)) {
            return mockAsk(question);
        }
        return customApiAsk(question);
    }

    private String mockAsk(String question) {
        for (Map.Entry<String, String> entry : KNOWLEDGE_BASE.entrySet()) {
            if (question != null && question.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "这个问题属于《毛泽东思想和中国特色社会主义理论体系概论》的复习范围。建议先定位到相关章节，再结合教材中的基本概念、历史背景、理论意义和现实意义进行理解。";
    }

    private String customApiAsk(String question) {
        if (customApiUrl.isEmpty()) {
            return "尚未配置 Qwen API 地址，请检查 ai-config.properties。";
        }
        if (customApiKey == null || customApiKey.trim().isEmpty()) {
            return "尚未配置 DASHSCOPE_API_KEY。请在 Tomcat/IntelliJ 环境变量中设置新的 Qwen API Key。";
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", customModel);
        payload.addProperty("temperature", 0.3);
        payload.addProperty("top_p", 0.8);

        JsonArray messages = new JsonArray();
        messages.add(message("system", "你是《毛泽东思想和中国特色社会主义理论体系概论》课程复习助手。回答要准确、简明，适合学生复习考试；遇到不确定内容要提示回到教材核对。"));
        messages.add(message("user", question));
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(customApiUrl))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + customApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Qwen API 调用失败: status={}, body={}", response.statusCode(), response.body());
                return "Qwen API 调用失败，状态码：" + response.statusCode() + "。请检查 API Key、模型名和网络。";
            }
            return parseChatCompletion(response.body());
        } catch (Exception e) {
            log.error("Qwen API 调用异常", e);
            return "调用 Qwen 失败：" + e.getMessage();
        }
    }

    private JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content == null ? "" : content);
        return message;
    }

    private String parseChatCompletion(String body) {
        JsonObject json = gson.fromJson(body, JsonObject.class);
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            return "Qwen 返回为空，请稍后重试。";
        }
        JsonObject first = choices.get(0).getAsJsonObject();
        JsonObject message = first.getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            return "Qwen 返回格式异常，请检查接口配置。";
        }
        return message.get("content").getAsString();
    }

    public List<Question> parseQuestionText(String text, int defaultChapter) {
        List<Question> result = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] blocks = normalized.split("\\n(?=\\s*\\d+[.．、])");
        if (blocks.length <= 1 && normalized.contains("\n\n")) {
            blocks = normalized.split("\\n\\s*\\n");
        }

        for (String block : blocks) {
            Question question = parseSingleBlock(block.trim(), defaultChapter);
            if (question != null) {
                result.add(question);
            }
        }
        return result;
    }

    public String explainWrongAnswer(Question question, String userAnswer) {
        if (question == null) {
            return "题目不存在，暂时无法解释错因。";
        }
        if ("mock".equalsIgnoreCase(serviceType)) {
            return mockWrongAnswerExplanation(question, userAnswer);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("请用适合大学生复习《毛概》的方式解释这道选择题为什么答错。");
        prompt.append("要求：1. 先指出学生选了什么、正确答案是什么；");
        prompt.append("2. 分析正确选项为什么对；3. 简要说明学生选项为什么容易混淆；");
        prompt.append("4. 结尾给一句记忆提示。不要编造教材外信息。\n\n");
        prompt.append("题干：").append(question.getQuestion()).append("\n");
        prompt.append("选项：\n");
        if (question.getOptions() != null) {
            for (String option : question.getOptions()) {
                prompt.append(option).append("\n");
            }
        }
        prompt.append("学生答案：").append(userAnswer == null ? "" : userAnswer).append("\n");
        prompt.append("正确答案：").append(question.getAnswer()).append("\n");
        prompt.append("原解析：").append(question.getExplanation() == null ? "" : question.getExplanation());
        return customApiAsk(prompt.toString());
    }

    private String mockWrongAnswerExplanation(Question question, String userAnswer) {
        StringBuilder sb = new StringBuilder();
        sb.append("你选的是 ").append(userAnswer == null || userAnswer.isBlank() ? "空" : userAnswer)
                .append("，正确答案是 ").append(question.getAnswer()).append("。\n");
        sb.append("这道题要抓住题干里的关键词：").append(question.getQuestion()).append("\n");
        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            sb.append("原解析已经给出关键依据：").append(question.getExplanation()).append("\n");
        }
        sb.append("复习时建议把“时间、会议、理论成果、历史地位”这类固定搭配一起记，政治选择题很爱考这种对应关系。");
        return sb.toString();
    }

    private Question parseSingleBlock(String block, int defaultChapter) {
        if (block == null || block.length() < 5) return null;

        String[] lines = block.split("\\n");
        List<String> contentLines = new ArrayList<>();
        String answerLine = null;
        String explainLine = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("答案") || trimmed.startsWith("正确答案")) {
                answerLine = trimmed;
            } else if (trimmed.startsWith("解析") || trimmed.startsWith("解释")) {
                explainLine = trimmed;
            } else {
                contentLines.add(trimmed);
            }
        }

        if (contentLines.isEmpty() || answerLine == null) return null;

        Question q = new Question();
        q.setChapter(defaultChapter);
        String questionText = contentLines.get(0).replaceFirst("^\\d+[.．、]\\s*", "");
        q.setQuestion(questionText);

        List<String> options = new ArrayList<>();
        for (int i = 1; i < contentLines.size(); i++) {
            String line = contentLines.get(i);
            if (line.matches("^[A-H][.．、\\s].*")) {
                options.add(line);
            }
        }

        String answer = answerLine.replaceFirst("^.*?[：:]\\s*", "").trim().toUpperCase();
        if (options.isEmpty()) {
            q.setType("judge");
            q.setOptions(List.of("A. 对", "B. 错"));
            if ("对".equals(answer) || "正确".equals(answer) || "TRUE".equals(answer)) {
                answer = "A";
            } else if ("错".equals(answer) || "错误".equals(answer) || "FALSE".equals(answer)) {
                answer = "B";
            }
        } else if (answer.length() > 1) {
            q.setType("multiple");
            q.setOptions(options);
        } else {
            q.setType("single");
            q.setOptions(options);
        }

        q.setAnswer(answer);
        q.setExplanation(explainLine == null ? "" : explainLine.replaceFirst("^.*?[：:]\\s*", "").trim());
        return q;
    }

    public String parseQuestionImage(String base64Image) {
        log.info("收到图片题库解析请求，大小 {} 字符", base64Image != null ? base64Image.length() : 0);
        return "";
    }
}
