package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.Chapter;
import com.maogai.util.DocxUtil;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OutlineService {

    private static final Logger log = LoggerFactory.getLogger(OutlineService.class);
    private static final String CHAPTERS_PATH = "data/chapters.json";

    private List<Chapter> chapters;
    private final Map<Integer, String> contentCache = new HashMap<>();

    public OutlineService() {
        loadChapters();
    }

    /**
     * 加载章节索引
     */
    private void loadChapters() {
        try {
            chapters = JsonUtil.readList(CHAPTERS_PATH,
                    TypeToken.getParameterized(List.class, Chapter.class).getType());
            chapters.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
            log.info("加载了 {} 个章节", chapters.size());
        } catch (Exception e) {
            log.error("加载章节数据失败", e);
            chapters = new ArrayList<>();
        }
    }

    /**
     * 获取所有章节列表
     */
    public List<Map<String, Object>> getChapterList() {
        return chapters.stream().map(ch -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ch.getId());
            map.put("title", ch.getTitle());
            map.put("order", ch.getOrder());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取章节内容(从docx解析)
     */
    public Chapter getChapterContent(int chapterId) {
        Chapter chapter = chapters.stream()
                .filter(ch -> ch.getId() == chapterId)
                .findFirst()
                .orElse(null);
        if (chapter == null) return null;

        if (chapter.getContent() != null && !chapter.getContent().trim().isEmpty()) {
            return chapter;
        }

        // 检查缓存
        if (contentCache.containsKey(chapterId)) {
            chapter.setContent(contentCache.get(chapterId));
            return chapter;
        }

        // 解析docx
        if (chapter.getDocxPath() == null || chapter.getDocxPath().trim().isEmpty()) {
            chapter.setContent(generateFallbackContent(chapter));
            return chapter;
        }

        try {
            String html = DocxUtil.docxToHtml(chapter.getDocxPath());
            contentCache.put(chapterId, html);
            chapter.setContent(html);
        } catch (Exception e) {
            log.warn("解析docx失败: {}, 使用默认内容", chapter.getDocxPath());
            chapter.setContent(generateFallbackContent(chapter));
        }

        return chapter;
    }

    /**
     * 当docx不存在时，生成默认章节内容
     */
    private String generateFallbackContent(Chapter chapter) {
        return "<div class=\"chapter-content\">\n" +
               "    <h1>" + chapter.getTitle() + "</h1>\n" +
               "    <p class=\"notice\">请将对应的docx文档放入 resources/data/docs/ 目录下，\n" +
               "    或通过系统上传功能上传章节文档。</p>\n" +
               "    <p>文件名应为: " + chapter.getDocxPath() + "</p>\n" +
               "</div>";
    }

    /**
     * 刷新章节缓存
     */
    public void refresh() {
        contentCache.clear();
        loadChapters();
    }
}
