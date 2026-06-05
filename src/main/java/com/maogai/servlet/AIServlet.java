package com.maogai.servlet;

import com.maogai.model.Question;
import com.maogai.service.AIService;
import com.maogai.service.QuestionService;
import com.maogai.service.ServiceFactory;
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

/**
 * AI服务API
 * POST /api/ai/ask   - 学习答疑  {question: "..."}
 * POST /api/ai/parse - 题库解析  {text: "...", chapter: 1} 或 {image: "base64..."}
 */
public class AIServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        AIService aiService = ServiceFactory.getAIService();
        QuestionService questionService = ServiceFactory.getQuestionService();

        if ("/ask".equals(pathInfo)) {
            Map<String, Object> params = parseBody(req);
            String question = (String) params.get("question");

            if (question == null || question.trim().isEmpty()) {
                writeError(resp, "问题不能为空");
                return;
            }

            String answer = aiService.ask(question);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("question", question);
            result.put("answer", answer);
            result.put("timestamp", System.currentTimeMillis());
            writeJson(resp, result);

        } else if ("/parse".equals(pathInfo)) {
            Map<String, Object> params = parseBody(req);
            String text = (String) params.get("text");
            String image = (String) params.get("image");
            int chapter = params.get("chapter") != null
                    ? ((Double) params.get("chapter")).intValue() : 1;

            if (image != null && !image.isEmpty()) {
                // 图片模式
                String ocrResult = aiService.parseQuestionImage(image);
                List<Question> parsed = aiService.parseQuestionText(ocrResult, chapter);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("mode", "image");
                result.put("rawText", ocrResult);
                result.put("count", parsed.size());
                result.put("questions", parsed);
                writeJson(resp, result);

            } else if (text != null && !text.isEmpty()) {
                // 文本模式
                List<Question> parsed = aiService.parseQuestionText(text, chapter);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("mode", "text");
                result.put("count", parsed.size());
                result.put("questions", parsed);
                writeJson(resp, result);

            } else {
                writeError(resp, "请提供文本(text)或图片(image)数据");
            }

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
        if (sb.length() == 0) return new HashMap<>();
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
