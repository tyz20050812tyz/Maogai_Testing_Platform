package com.maogai.servlet;

import com.maogai.model.UserAccount;
import com.maogai.service.ServiceFactory;
import com.maogai.service.SmsService;
import com.maogai.service.UserAccountService;
import com.maogai.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/login".equals(pathInfo)) {
            handleLogin(req, resp);
        } else if ("/send-code".equals(pathInfo)) {
            handleSendCode(req, resp);
        } else if ("/register".equals(pathInfo)) {
            handleRegister(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/logout".equals(pathInfo)) {
            ServiceFactory.getUserService().logout(req);
            resp.sendRedirect(req.getContextPath() + "/page/login");
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        UserAccount account = ServiceFactory.getUserAccountService().authenticate(username, password);
        if (account != null) {
            ServiceFactory.getUserService().login(req, account.getUsername(), account.getUserKey());
            redirectBack(req, resp);
        } else {
            req.getSession(true).setAttribute("authMessage", "用户名或密码错误");
            resp.sendRedirect(req.getContextPath() + "/page/login");
        }
    }

    private void handleSendCode(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserAccountService accountService = ServiceFactory.getUserAccountService();
        String phone = accountService.normalizePhone(req.getParameter("phone"));
        if (!accountService.isValidPhone(phone)) {
            writeJson(resp, false, "请输入正确的手机号", null);
            return;
        }

        SmsService.SmsSendResult result = ServiceFactory.getSmsService().sendRegisterCode(req, phone);
        Map<String, Object> extra = new HashMap<>();
        extra.put("mock", result.isMock());
        if (result.isMock()) {
            extra.put("code", result.getCode());
        }
        writeJson(resp, result.isSuccess(), result.getMessage(), extra);
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserAccountService accountService = ServiceFactory.getUserAccountService();
        String phone = accountService.normalizePhone(req.getParameter("phone"));
        String code = req.getParameter("code");
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (!accountService.isValidPhone(phone)) {
            req.getSession(true).setAttribute("authMessage", "请输入正确的手机号");
            resp.sendRedirect(req.getContextPath() + "/page/login");
            return;
        }
        if (!ServiceFactory.getSmsService().verifyRegisterCode(req, phone, code)) {
            req.getSession(true).setAttribute("authMessage", "验证码错误或已过期");
            resp.sendRedirect(req.getContextPath() + "/page/login");
            return;
        }

        try {
            UserAccount account = accountService.register(username, phone, password);
            ServiceFactory.getUserService().login(req, account.getUsername(), account.getUserKey());
            redirectBack(req, resp);
        } catch (IllegalArgumentException e) {
            req.getSession(true).setAttribute("authMessage", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/page/login");
        }
    }

    private void redirectBack(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String redirect = req.getParameter("redirect");
        if (!isSafeRedirect(req, redirect)) {
            redirect = req.getContextPath() + "/index";
        }
        resp.sendRedirect(redirect);
    }

    private boolean isSafeRedirect(HttpServletRequest req, String redirect) {
        if (redirect == null || redirect.trim().isEmpty()) {
            return false;
        }
        String contextPath = req.getContextPath();
        if (contextPath == null || contextPath.isEmpty()) {
            return redirect.startsWith("/") && !redirect.startsWith("//");
        }
        return redirect.equals(contextPath) || redirect.startsWith(contextPath + "/");
    }

    private void writeJson(HttpServletResponse resp, boolean success, String message, Map<String, Object> extra) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", message);
        if (extra != null) {
            result.putAll(extra);
        }
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(result));
    }
}
