package com.example.demo;

public class CustomNotification {
    private String id;
    private String title;
    private String content;
    private boolean isNew;
    private long timestamp;

    public CustomNotification(String id, String title, String content, boolean isNew, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.isNew = isNew;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isNew() { return isNew; }
    public long getTimestamp() { return timestamp; }

    public void setNew(boolean isNew) { this.isNew = isNew; }
}
