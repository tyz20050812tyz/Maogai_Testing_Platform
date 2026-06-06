package com.maogai.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 页面路由Servlet - 处理JSP页面映射
 */
public class PageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("text/html;charset=UTF-8");

        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendRedirect(req.getContextPath() + "/index");
            return;
        }

        String jspPath = mapToJsp(pathInfo);
        req.getRequestDispatcher(jspPath).forward(req, resp);
    }

    private String mapToJsp(String pathInfo) {
        switch (pathInfo) {
            case "/outline":
                return "/WEB-INF/views/outline.jsp";
            case "/reader":
                return "/WEB-INF/views/reader.jsp";
            case "/quiz":
                return "/WEB-INF/views/quiz.jsp";
            case "/random":
                return "/WEB-INF/views/random_quiz.jsp";
            case "/wrong":
                return "/WEB-INF/views/wrong_book.jsp";
            case "/ai":
                return "/WEB-INF/views/ai_chat.jsp";
            case "/import":
                return "/WEB-INF/views/import.jsp";
            case "/stats":
                return "/WEB-INF/views/stats.jsp";
            case "/flashcards":
                return "/WEB-INF/views/flashcards.jsp";
            case "/login":
                return "/WEB-INF/views/login.jsp";
            default:
                return "/WEB-INF/views/index.jsp";
        }
    }
}
