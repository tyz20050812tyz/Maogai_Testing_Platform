package com.maogai.service;

import com.maogai.model.Question;
import com.maogai.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 题目导入服务 - 接收文本/图片上传，调用AI解析后批量入库
 */
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final AIService aiService;
    private final QuestionService questionService;

    public ImportService(AIService aiService, QuestionService questionService) {
        this.aiService = aiService;
        this.questionService = questionService;
    }

    /**
     * 处理文本导入（直接粘贴的题库文本）
     */
    public Map<String, Object> importText(String text, int defaultChapter) {
        return importText(text, defaultChapter, QuestionService.BANK_CHAPTER);
    }

    public Map<String, Object> importText(String text, int defaultChapter, String bank) {
        Map<String, Object> result = new HashMap<>();

        if (text == null || text.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "导入文本不能为空");
            return result;
        }

        // 检测是否为JSON格式
        if (text.trim().startsWith("[") || text.trim().startsWith("{")) {
            return importJson(text, defaultChapter, bank);
        }

        // 使用AI解析文本
        List<Question> parsed = aiService.parseQuestionText(text, defaultChapter);

        if (parsed.isEmpty()) {
            result.put("success", false);
            result.put("message", "未能识别出有效题目，请检查格式：\n"
                    + "1. 题目内容\nA. 选项A\nB. 选项B\n答案：B\n解析：...");
            return result;
        }

        int count = questionService.addQuestions(bank, parsed);
        result.put("success", true);
        result.put("message", "成功导入 " + count + " 道题目");
        result.put("count", count);
        result.put("questions", parsed);

        log.info("文本导入: 解析出 {} 道题目，成功入库 {} 道", parsed.size(), count);
        return result;
    }

    /**
     * 处理JSON格式导入
     */
    private Map<String, Object> importJson(String json, int defaultChapter) {
        return importJson(json, defaultChapter, QuestionService.BANK_CHAPTER);
    }

    private Map<String, Object> importJson(String json, int defaultChapter, String bank) {
        Map<String, Object> result = new HashMap<>();

        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.reflect.TypeToken<List<Question>> typeToken =
                    new com.google.gson.reflect.TypeToken<List<Question>>() {};

            List<Question> questions = gson.fromJson(json, typeToken.getType());

            if (questions == null || questions.isEmpty()) {
                result.put("success", false);
                result.put("message", "JSON格式正确但未包含题目数据");
                return result;
            }

            // 设置默认章节
            for (Question q : questions) {
                if (q.getChapter() == 0) {
                    q.setChapter(defaultChapter);
                }
            }

            int count = questionService.addQuestions(bank, questions);
            result.put("success", true);
            result.put("message", "成功从JSON导入 " + count + " 道题目");
            result.put("count", count);
        } catch (Exception e) {
            log.error("JSON导入失败", e);
            result.put("success", false);
            result.put("message", "JSON解析失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 处理图片上传导入
     */
    public Map<String, Object> importImage(Part filePart) {
        return importImage(filePart, QuestionService.BANK_CHAPTER);
    }

    public Map<String, Object> importImage(Part filePart, String bank) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 读取图片为base64
            String base64 = readPartAsBase64(filePart);
            if (base64 == null) {
                result.put("success", false);
                result.put("message", "读取图片文件失败");
                return result;
            }

            // AI识别图片中的题目
            String ocrResult = aiService.parseQuestionImage(base64);

            if (ocrResult == null || ocrResult.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "图片识别失败，请确保图片清晰且包含题目文字");
                return result;
            }

            // 解析识别结果
            List<Question> parsed = aiService.parseQuestionText(ocrResult, 1);

            if (parsed.isEmpty()) {
                result.put("success", false);
                result.put("message", "图片识别成功但未能提取有效题目，请检查图片内容");
                result.put("rawText", ocrResult);
                return result;
            }

            int count = questionService.addQuestions(bank, parsed);
            result.put("success", true);
            result.put("message", "图片识别成功，导入 " + count + " 道题目");
            result.put("count", count);
            result.put("rawText", ocrResult);
            result.put("questions", parsed);

            log.info("图片导入: OCR识别 {} 字，提取 {} 道题目", ocrResult.length(), count);
        } catch (Exception e) {
            log.error("图片导入失败", e);
            result.put("success", false);
            result.put("message", "图片处理失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 处理文件上传导入（JSON/TXT文件）
     */
    public Map<String, Object> importFile(Part filePart, int defaultChapter) {
        return importFile(filePart, defaultChapter, QuestionService.BANK_CHAPTER);
    }

    public Map<String, Object> importFile(Part filePart, int defaultChapter, String bank) {
        Map<String, Object> result = new HashMap<>();

        try {
            String content = readPartAsString(filePart);
            String fileName = getFileName(filePart);

            if (fileName != null && fileName.toLowerCase().endsWith(".json")) {
                return importJson(content, defaultChapter, bank);
            }

            // 文本文件使用AI解析
            List<Question> parsed = aiService.parseQuestionText(content, defaultChapter);

            if (parsed.isEmpty()) {
                result.put("success", false);
                result.put("message", "文件内容未能识别出有效题目");
                return result;
            }

            int count = questionService.addQuestions(bank, parsed);
            result.put("success", true);
            result.put("message", "成功从文件导入 " + count + " 道题目");
            result.put("count", count);
            result.put("questions", parsed);

            log.info("文件导入: {} -> 解析 {} 道题目", fileName, count);
        } catch (Exception e) {
            log.error("文件导入失败", e);
            result.put("success", false);
            result.put("message", "文件读取失败: " + e.getMessage());
        }

        return result;
    }

    private String readPartAsString(Part part) throws IOException {
        try (InputStream is = part.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    private String readPartAsBase64(Part part) throws IOException {
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        }
    }

    private String getFileName(Part part) {
        String disposition = part.getHeader("content-disposition");
        if (disposition != null) {
            for (String item : disposition.split(";")) {
                if (item.trim().startsWith("filename")) {
                    return item.substring(item.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return null;
    }
}
