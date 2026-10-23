package com.example.peertutoring.models;

/** One row of the inbox: the other person and the latest message between you. */
public class Conversation {

    private String conversationId;
    private String otherUid;
    private String otherName;
    private String lastMessage;
    private String lastSenderId;
    private String lastAt;

    public String getConversationId() { return conversationId; }
    public String getOtherUid() { return otherUid; }
    public String getOtherName() { return otherName; }
    public String getLastMessage() { return lastMessage; }
    public String getLastSenderId() { return lastSenderId; }
    public String getLastAt() { return lastAt; }
}
