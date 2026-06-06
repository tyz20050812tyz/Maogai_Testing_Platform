package com.maogai.servlet;

import com.maogai.model.Question;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        QuestionService service = ServiceFactory.getQuestionService();
        WrongBookService wrongBookService = ServiceFactory.getWrongBookService();
        UserService userService = ServiceFactory.getUserService();
        String userKey = userService.currentUserKey(req);
        String bank = service.normalizeBank(req.getParameter("bank"));
        String examBank = service.normalizeExamBank(req.getParameter("examBank"));

        if ("/exam-banks".equals(pathInfo)) {
            writeJson(resp, service.getExamBanks());

        } else if ("/mastery".equals(pathInfo)) {
            writeJson(resp, ServiceFactory.getAnswerStatsService().getChapterMastery(userKey));

        } else if ("/list".equals(pathInfo)) {
            String chapterStr = req.getParameter("chapter");
            String type = req.getParameter("type");
            Integer chapter = (chapterStr != null && !chapterStr.isEmpty()) ? Integer.parseInt(chapterStr) : null;
            List<Question> list = service.filter(bank, examBank, chapter, type);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Question q : list) {
                Map<String, Object> map = questionToMap(q, bank, examBank);
                map.put("inWrongBook", wrongBookService.isInWrongBook(userKey, bank, examBank, q.getId()));
                result.add(map);
            }
            writeJson(resp, result);

        } else if ("/random".equals(pathInfo)) {
            int count = 10;
            String countStr = req.getParameter("count");
            String chapterStr = req.getParameter("chapter");
            if (countStr != null && !countStr.isEmpty()) {
                count = Integer.parseInt(countStr);
            }
            List<Question> list;
            if (chapterStr != null && !chapterStr.isEmpty()) {
                list = service.randomPickByChapter(bank, examBank, Integer.parseInt(chapterStr), count);
            } else {
                list = service.randomPick(bank, examBank, count);
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Question q : list) {
                result.add(questionToMap(q, bank, examBank));
            }
            writeJson(resp, result);

        } else if ("/stats".equals(pathInfo)) {
            Map<String, Object> stats = req.getParameter("examBank") == null
                    ? service.getStats(bank)
                    : service.getStats(bank, examBank);
            stats.put("wrongCount", wrongBookService.getCount(userKey));
            writeJson(resp, stats);

        } else if ("/question".equals(pathInfo)) {
            String idStr = req.getParameter("id");
            if (idStr != null) {
                Question q = service.getById(bank, examBank, Integer.parseInt(idStr));
                if (q != null) {
                    Map<String, Object> map = questionToMap(q, bank, examBank);
                    map.put("nextId", service.getNextId(bank, examBank, q.getId()));
                    map.put("prevId", service.getPrevId(bank, examBank, q.getId()));
                    map.put("inWrongBook", wrongBookService.isInWrongBook(userKey, bank, examBank, q.getId()));
                    writeJson(resp, map);
                } else {
                    writeError(resp, "题目不存在");
                }
            } else {
                writeError(resp, "缺少参数 id");
            }

        } else {
            writeError(resp, "未知接口");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        QuestionService service = ServiceFactory.getQuestionService();
        WrongBookService wrongBookService = ServiceFactory.getWrongBookService();
        UserService userService = ServiceFactory.getUserService();
        String userKey = userService.currentUserKey(req);

        if ("/add".equals(pathInfo)) {
            String body = readBody(req);
            Map<String, Object> params = JsonUtil.fromJson(body, Map.class);
            String bank = service.normalizeBank((String) params.get("bank"));
            String examBank = service.normalizeExamBank((String) params.get("examBank"));
            Question question = JsonUtil.fromJson(body, Question.class);
            if (question == null) {
                writeError(resp, "无效的题目数据");
                return;
            }
            Question saved = service.addQuestion(bank, examBank, question);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "添加成功");
            result.put("question", saved);
            writeJson(resp, result);

        } else if ("/check".equals(pathInfo)) {
            String body = readBody(req);
            Map<String, Object> params = JsonUtil.fromJson(body, Map.class);
            int questionId = ((Double) params.get("questionId")).intValue();
            String bank = service.normalizeBank((String) params.get("bank"));
            String examBank = service.normalizeExamBank((String) params.get("examBank"));
            String userAnswer = (String) params.get("answer");

            Question question = service.getById(bank, examBank, questionId);
            if (question == null) {
                writeError(resp, "题目不存在");
                return;
            }

            boolean correct = question.getAnswer().equalsIgnoreCase(userAnswer.trim());
            Map<String, Object> result = new HashMap<>();
            result.put("correct", correct);
            result.put("userAnswer", userAnswer);
            result.put("correctAnswer", question.getAnswer());
            result.put("explanation", question.getExplanation());
            result.put("bank", bank);
            result.put("examBank", examBank);
            ServiceFactory.getAnswerStatsService().record(userKey, bank, examBank, question, userAnswer, correct);

            if (!correct) {
                wrongBookService.addWrong(userKey, bank, examBank, questionId);
            }

            writeJson(resp, result);

        } else if ("/explain".equals(pathInfo)) {
            String body = readBody(req);
            Map<String, Object> params = JsonUtil.fromJson(body, Map.class);
            int questionId = ((Double) params.get("questionId")).intValue();
            String bank = service.normalizeBank((String) params.get("bank"));
            String examBank = service.normalizeExamBank((String) params.get("examBank"));
            String userAnswer = (String) params.get("answer");
            Question question = service.getById(bank, examBank, questionId);
            if (question == null) {
                writeError(resp, "题目不存在");
                return;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("explanation", ServiceFactory.getAIService().explainWrongAnswer(question, userAnswer));
            writeJson(resp, result);

        } else if ("/wrong".equals(pathInfo)) {
            String body = readBody(req);
            Map<String, Object> params = JsonUtil.fromJson(body, Map.class);
            int questionId = ((Double) params.get("questionId")).intValue();
            String bank = service.normalizeBank((String) params.get("bank"));
            String examBank = service.normalizeExamBank((String) params.get("examBank"));
            wrongBookService.addWrong(userKey, bank, examBank, questionId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已加入错题本");
            writeJson(resp, result);

        } else if ("/remove-wrong".equals(pathInfo)) {
            String body = readBody(req);
            Map<String, Object> params = JsonUtil.fromJson(body, Map.class);
            int questionId = ((Double) params.get("questionId")).intValue();
            String bank = service.normalizeBank((String) params.get("bank"));
            String examBank = service.normalizeExamBank((String) params.get("examBank"));
            wrongBookService.removeWrong(userKey, bank, examBank, questionId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已从错题本移除");
            writeJson(resp, result);

        } else {
            writeError(resp, "未知接口");
        }
    }

    private Map<String, Object> questionToMap(Question q, String bank, String examBank) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", q.getId());
        map.put("bank", bank);
        if (QuestionService.BANK_EXAM.equals(bank)) {
            map.put("examBank", examBank);
        }
        map.put("chapter", q.getChapter());
        map.put("type", q.getType());
        map.put("question", q.getQuestion());
        map.put("options", q.getOptions());
        map.put("answer", q.getAnswer());
        map.put("explanation", q.getExplanation());
        return map;
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
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
