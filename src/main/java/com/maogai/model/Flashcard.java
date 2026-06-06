package com.maogai.model;

public class Flashcard {
    private int id;
    private int chapter;
    private String front;
    private String back;
    private String tag;

    public Flashcard() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChapter() { return chapter; }
    public void setChapter(int chapter) { this.chapter = chapter; }

    public String getFront() { return front; }
    public void setFront(String front) { this.front = front; }

    public String getBack() { return back; }
    public void setBack(String back) { this.back = back; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
