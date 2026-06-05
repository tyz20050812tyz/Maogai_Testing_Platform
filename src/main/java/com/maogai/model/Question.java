package com.maogai.model;

import java.util.List;

public class Question {
    private int id;
    private int chapter;
    private String type;       // single, multiple, judge
    private String question;
    private List<String> options;
    private String answer;     // 单选: "A", 多选: "ABC", 判断: "对"/"错"
    private String explanation;

    public Question() {}

    public Question(int id, int chapter, String type, String question,
                    List<String> options, String answer, String explanation) {
        this.id = id;
        this.chapter = chapter;
        this.type = type;
        this.question = question;
        this.options = options;
        this.answer = answer;
        this.explanation = explanation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChapter() { return chapter; }
    public void setChapter(int chapter) { this.chapter = chapter; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
