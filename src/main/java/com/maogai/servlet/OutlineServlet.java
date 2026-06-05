package com.maogai.servlet;

import com.maogai.model.Chapter;
import com.maogai.service.OutlineService;
import com.maogai.service.ServiceFactory;
import com.maogai.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 章节大纲API
 * GET /api/outline/list       - 获取章节列表
 * GET /api/outline/reader?id=1 - 获取章节内容
 */
public class OutlineServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        OutlineService service = ServiceFactory.getOutlineService();

        if ("/list".equals(pathInfo)) {
            // 章节列表
            writeJson(resp, service.getChapterList());

        } else if ("/reader".equals(pathInfo)) {
            // 章节内容
            String idParam = req.getParameter("id");
            if (idParam == null) {
                writeError(resp, "缺少参数id");
                return;
            }
            try {
                int id = Integer.parseInt(idParam);
                Chapter chapter = service.getChapterContent(id);
                if (chapter == null) {
                    writeError(resp, "章节不存在");
                    return;
                }
                Map<String, Object> result = new HashMap<>();
                result.put("id", chapter.getId());
                result.put("title", chapter.getTitle());
                result.put("content", chapter.getContent());
                writeJson(resp, result);
            } catch (NumberFormatException e) {
                writeError(resp, "无效的章节ID");
            }

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
