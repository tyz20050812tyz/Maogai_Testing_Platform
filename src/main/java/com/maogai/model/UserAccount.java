package com.maogai.model;

public class UserAccount {
    private int id;
    private String username;
    private String phone;
    private String userKey;
    private long createdAt;
    private long lastLoginAt;

    public UserAccount() {}

    public UserAccount(int id, String username, String phone, String userKey, long createdAt, long lastLoginAt) {
        this.id = id;
        this.username = username;
        this.phone = phone;
        this.userKey = userKey;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(long lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
