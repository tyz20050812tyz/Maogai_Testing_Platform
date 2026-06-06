package com.maogai.util;

import com.maogai.service.UserService;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // No setup required.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublicPath(path) || isLoggedIn(req)) {
            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/")) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"success\":false,\"message\":\"请先登录\"}");
            return;
        }

        String target = req.getRequestURI();
        if (req.getQueryString() != null && !req.getQueryString().isEmpty()) {
            target += "?" + req.getQueryString();
        }
        String redirect = req.getContextPath() + "/page/login?redirect=" +
                URLEncoder.encode(target, StandardCharsets.UTF_8);
        resp.sendRedirect(redirect);
    }

    @Override
    public void destroy() {
        // No cleanup required.
    }

    private boolean isLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return false;
        }
        Object userKey = session.getAttribute(UserService.SESSION_USER_KEY);
        return userKey != null && !UserService.GUEST_USER.equals(String.valueOf(userKey));
    }

    private boolean isPublicPath(String path) {
        return path.equals("/page/login")
                || path.startsWith("/auth/")
                || path.startsWith("/static/")
                || path.equals("/favicon.ico")
                || path.equals("/robots.txt");
    }
}
