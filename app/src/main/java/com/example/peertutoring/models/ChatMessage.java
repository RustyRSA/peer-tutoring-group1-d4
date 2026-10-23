package com.example.peertutoring.models;

/** One message, as returned by the backend's /messages routes. */
public class ChatMessage {

    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String recipientName;
    private String text;
    private String createdAt;

    public String getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getRecipientId() { return recipientId; }
    public String getRecipientName() { return recipientName; }
    public String getText() { return text; }
    public String getCreatedAt() { return createdAt; }
}
