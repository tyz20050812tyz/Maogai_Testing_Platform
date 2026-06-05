package com.maogai.model;

public class WrongRecord {
    private int id;
    private int questionId;
    private String bank;
    private long timestamp;

    public WrongRecord() {}

    public WrongRecord(int id, int questionId, long timestamp) {
        this(id, questionId, "chapter", timestamp);
    }

    public WrongRecord(int id, int questionId, String bank, long timestamp) {
        this.id = id;
        this.questionId = questionId;
        this.bank = bank;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
