package com.maogai.servlet;

import com.maogai.service.QuestionService;
import com.maogai.service.ServiceFactory;
import com.maogai.service.UserService;
import com.maogai.service.WrongBookService;
import com.maogai.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WrongBookServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        WrongBookService service = ServiceFactory.getWrongBookService();
        String userKey = ServiceFactory.getUserService().currentUserKey(req);

        if ("/list".equals(pathInfo)) {
            List<Map<String, Object>> list = service.getWrongList(userKey);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", list.size());
            result.put("data", list);
            writeJson(resp, result);
        } else {
            writeError(resp, "未知接口");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        WrongBookService service = ServiceFactory.getWrongBookService();
        QuestionService questionService = ServiceFactory.getQuestionService();
        String userKey = ServiceFactory.getUserService().currentUserKey(req);

        if ("/add".equals(pathInfo)) {
            Map<String, Object> params = parseBody(req);
            int questionId = ((Double) params.get("questionId")).intValue();
            String bank = questionService.normalizeBank((String) params.get("bank"));
            String examBank = questionService.normalizeExamBank((String) params.get("examBank"));
            service.addWrong(userKey, bank, examBank, questionId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已加入错题本");
            writeJson(resp, result);

        } else if ("/remove".equals(pathInfo)) {
            Map<String, Object> params = parseBody(req);
            int questionId = ((Double) params.get("questionId")).intValue();
            String bank = questionService.normalizeBank((String) params.get("bank"));
            String examBank = questionService.normalizeExamBank((String) params.get("examBank"));
            boolean removed = service.removeWrong(userKey, bank, examBank, questionId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", removed);
            result.put("message", removed ? "已从错题本移除" : "移除失败");
            writeJson(resp, result);

        } else if ("/clear".equals(pathInfo)) {
            service.clear(userKey);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "错题本已清空");
            writeJson(resp, result);

        } else {
            writeError(resp, "未知接口");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return JsonUtil.fromJson(sb.toString(), Map.class);
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
