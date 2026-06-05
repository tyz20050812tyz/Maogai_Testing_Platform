package com.maogai.servlet;

import com.maogai.service.ImportService;
import com.maogai.service.ServiceFactory;
import com.maogai.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 题目导入API
 * POST /api/import/upload
 *   - multipart: file=题库文件, chapter=1
 *   - 文本模式: text=题库文本, chapter=1
 */
@MultipartConfig(
    maxFileSize = 10485760,
    maxRequestSize = 20971520,
    fileSizeThreshold = 0
)
public class ImportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 返回导入页面
        resp.setContentType("text/html;charset=UTF-8");
        req.getRequestDispatcher("/WEB-INF/views/import.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        ImportService importService = ServiceFactory.getImportService();

        if ("/upload".equals(pathInfo)) {
            String contentType = req.getContentType();

            if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
                // 文件上传模式
                handleFileUpload(req, resp, importService);
            } else {
                // JSON文本模式
                handleTextUpload(req, resp, importService);
            }
        } else {
            writeError(resp, "未知接口");
        }
    }

    /**
     * 处理文件上传
     */
    private void handleFileUpload(HttpServletRequest req, HttpServletResponse resp,
                                   ImportService importService) throws IOException, ServletException {
        int defaultChapter = 1;
        String chapterParam = req.getParameter("chapter");
        if (chapterParam != null && !chapterParam.isEmpty()) {
            defaultChapter = Integer.parseInt(chapterParam);
        }
        String bank = ServiceFactory.getQuestionService().normalizeBank(req.getParameter("bank"));
        String examBank = ServiceFactory.getQuestionService().normalizeExamBank(req.getParameter("examBank"));

        Part filePart = req.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            writeError(resp, "未选择文件");
            return;
        }

        String fileName = getFileName(filePart);
        Map<String, Object> result;

        // 判断是否为图片文件
        if (isImageFile(fileName)) {
            result = importService.importImage(filePart, bank, examBank);
        } else {
            result = importService.importFile(filePart, defaultChapter, bank, examBank);
        }

        writeJson(resp, result);
    }

    /**
     * 处理文本粘贴上传
     */
    private void handleTextUpload(HttpServletRequest req, HttpServletResponse resp,
                                   ImportService importService) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line).append("\n");
        }
        String body = sb.toString();

        Map<String, Object> params;
        try {
            params = JsonUtil.fromJson(body, Map.class);
        } catch (Exception e) {
            // 纯文本模式
            Map<String, Object> result = importService.importText(body, 1);
            writeJson(resp, result);
            return;
        }

        String text = (String) params.get("text");
        int chapter = params.get("chapter") != null
                ? ((Double) params.get("chapter")).intValue() : 1;
        String bank = ServiceFactory.getQuestionService().normalizeBank((String) params.get("bank"));
        String examBank = ServiceFactory.getQuestionService().normalizeExamBank((String) params.get("examBank"));

        Map<String, Object> result = importService.importText(text, chapter, bank, examBank);
        writeJson(resp, result);
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.endsWith(".bmp") || lower.endsWith(".webp");
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
