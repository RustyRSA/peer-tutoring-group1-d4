package com.example.peertutoring.models;

/** One notification, as returned by the backend's /notifications routes. */
public class AppNotification {

    private String id;
    private String type;      // session_request, session_accepted, session_declined, ...
    private String title;
    private String body;
    private boolean read;
    private String createdAt;

    public String getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isRead() { return read; }
    public String getCreatedAt() { return createdAt; }

    public void setRead(boolean read) { this.read = read; }
}
