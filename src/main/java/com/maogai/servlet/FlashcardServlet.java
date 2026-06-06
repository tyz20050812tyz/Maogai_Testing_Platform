package com.maogai.servlet;

import com.maogai.service.ServiceFactory;
import com.maogai.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FlashcardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/list".equals(pathInfo)) {
            Integer chapter = null;
            String chapterParam = req.getParameter("chapter");
            if (chapterParam != null && !chapterParam.isBlank()) {
                chapter = Integer.parseInt(chapterParam);
            }
            writeJson(resp, ServiceFactory.getFlashcardService().list(chapter));
        } else {
            writeError(resp, "未知接口");
        }
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(data));
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        writeJson(resp, error);
    }
}
