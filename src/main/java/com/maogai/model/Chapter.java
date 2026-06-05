package com.maogai.model;

public class Chapter {
    private int id;
    private String title;
    private String content;
    private String docxPath;
    private int order;

    public Chapter() {}

    public Chapter(int id, String title, String docxPath, int order) {
        this.id = id;
        this.title = title;
        this.docxPath = docxPath;
        this.order = order;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDocxPath() { return docxPath; }
    public void setDocxPath(String docxPath) { this.docxPath = docxPath; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
