package com.maogai.servlet;

import com.maogai.service.OutlineService;
import com.maogai.service.QuestionService;
import com.maogai.service.ServiceFactory;
import com.maogai.service.WrongBookService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 首页Servlet - 渲染首页仪表盘
 */
public class IndexServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        QuestionService questionService = ServiceFactory.getQuestionService();
        OutlineService outlineService = ServiceFactory.getOutlineService();
        WrongBookService wrongBookService = ServiceFactory.getWrongBookService();

        Map<String, Object> stats = questionService.getStats();
        req.setAttribute("stats", stats);
        req.setAttribute("chapterList", outlineService.getChapterList());
        req.setAttribute("wrongCount", wrongBookService.getCount());

        resp.setContentType("text/html;charset=UTF-8");
        req.getRequestDispatcher("/WEB-INF/views/index.jsp").forward(req, resp);
    }
}
