package com.maogai.model;

public class WrongRecord {
    private int id;
    private int questionId;
    private String bank;
    private String examBank;
    private long timestamp;
    private String userAnswer;
    private String aiExplanation;

    public WrongRecord() {}

    public WrongRecord(int id, int questionId, long timestamp) {
        this(id, questionId, "chapter", "", timestamp, null, null);
    }

    public WrongRecord(int id, int questionId, String bank, long timestamp) {
        this(id, questionId, bank, "", timestamp, null, null);
    }

    public WrongRecord(int id, int questionId, String bank, String examBank, long timestamp) {
        this(id, questionId, bank, examBank, timestamp, null, null);
    }

    public WrongRecord(int id, int questionId, String bank, String examBank, long timestamp,
                       String userAnswer, String aiExplanation) {
        this.id = id;
        this.questionId = questionId;
        this.bank = bank;
        this.examBank = examBank;
        this.timestamp = timestamp;
        this.userAnswer = userAnswer;
        this.aiExplanation = aiExplanation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public String getExamBank() { return examBank; }
    public void setExamBank(String examBank) { this.examBank = examBank; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
}
