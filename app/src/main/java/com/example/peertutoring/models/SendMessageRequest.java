package com.example.peertutoring.models;

/** Body of POST /messages. */
public class SendMessageRequest {

    private final String recipientId;
    private final String text;

    public SendMessageRequest(String recipientId, String text) {
        this.recipientId = recipientId;
        this.text = text;
    }
}
