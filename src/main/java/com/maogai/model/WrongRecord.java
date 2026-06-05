package com.maogai.model;

public class WrongRecord {
    private int id;
    private int questionId;
    private String bank;
    private String examBank;
    private long timestamp;

    public WrongRecord() {}

    public WrongRecord(int id, int questionId, long timestamp) {
        this(id, questionId, "chapter", timestamp);
    }

    public WrongRecord(int id, int questionId, String bank, long timestamp) {
        this(id, questionId, bank, "", timestamp);
    }

    public WrongRecord(int id, int questionId, String bank, String examBank, long timestamp) {
        this.id = id;
        this.questionId = questionId;
        this.bank = bank;
        this.examBank = examBank;
        this.timestamp = timestamp;
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
}
