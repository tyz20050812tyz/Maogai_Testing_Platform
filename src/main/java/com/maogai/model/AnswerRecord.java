package com.maogai.model;

public class AnswerRecord {
    private int id;
    private int questionId;
    private int chapter;
    private String bank;
    private String examBank;
    private String type;
    private String userAnswer;
    private String correctAnswer;
    private boolean correct;
    private long timestamp;

    public AnswerRecord() {}

    public AnswerRecord(int id, int questionId, int chapter, String bank, String examBank,
                        String type, String userAnswer, String correctAnswer,
                        boolean correct, long timestamp) {
        this.id = id;
        this.questionId = questionId;
        this.chapter = chapter;
        this.bank = bank;
        this.examBank = examBank;
        this.type = type;
        this.userAnswer = userAnswer;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public int getChapter() { return chapter; }
    public void setChapter(int chapter) { this.chapter = chapter; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public String getExamBank() { return examBank; }
    public void setExamBank(String examBank) { this.examBank = examBank; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
