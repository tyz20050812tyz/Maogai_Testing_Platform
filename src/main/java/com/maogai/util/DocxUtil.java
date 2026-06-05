package com.maogai.util;

import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Docx文档解析工具 - 将docx转换为HTML
 */
public class DocxUtil {

    /**
     * 将docx文件转换为HTML字符串
     */
    public static String docxToHtml(String docxPath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"chapter-content\">\n");

        try (InputStream is = getDocxStream(docxPath);
             XWPFDocument document = new XWPFDocument(is)) {

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String style = paragraph.getStyle();
                String text = paragraph.getText();

                if (text == null || text.trim().isEmpty()) {
                    html.append("<br/>\n");
                    continue;
                }

                text = escapeHtml(text);

                // 根据样式渲染
                if (style != null) {
                    if (style.startsWith("Heading1") || style.contains("Heading1")) {
                        html.append("<h1>").append(text).append("</h1>\n");
                    } else if (style.startsWith("Heading2") || style.contains("Heading2")) {
                        html.append("<h2>").append(text).append("</h2>\n");
                    } else if (style.startsWith("Heading3") || style.contains("Heading3")) {
                        html.append("<h3>").append(text).append("</h3>\n");
                    } else if (style.startsWith("Heading")) {
                        html.append("<h4>").append(text).append("</h4>\n");
                    } else {
                        html.append("<p>").append(text).append("</p>\n");
                    }
                } else {
                    // 通过内容检测标题
                    if (text.startsWith("第") && (text.contains("章") || text.contains("节"))) {
                        html.append("<h2>").append(text).append("</h2>\n");
                    } else if (text.length() < 50 && text.endsWith("：") || text.endsWith(":")) {
                        html.append("<h3>").append(text).append("</h3>\n");
                    } else {
                        html.append("<p>").append(text).append("</p>\n");
                    }
                }
            }

            // 处理表格
            List<XWPFTable> tables = document.getTables();
            for (XWPFTable table : tables) {
                html.append("<table class=\"content-table\">\n");
                List<XWPFTableRow> rows = table.getRows();
                for (XWPFTableRow row : rows) {
                    html.append("<tr>");
                    List<XWPFTableCell> cells = row.getTableCells();
                    for (XWPFTableCell cell : cells) {
                        html.append("<td>").append(escapeHtml(cell.getText())).append("</td>");
                    }
                    html.append("</tr>\n");
                }
                html.append("</table>\n");
            }
        }

        html.append("</div>");
        return html.toString();
    }

    /**
     * 获取docx文件输入流 - 优先类路径，再文件系统
     */
    private static InputStream getDocxStream(String path) throws IOException {
        // 尝试类路径
        InputStream is = DocxUtil.class.getClassLoader().getResourceAsStream(path);
        if (is != null) return is;
        // 尝试文件系统
        return new FileInputStream(path);
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
