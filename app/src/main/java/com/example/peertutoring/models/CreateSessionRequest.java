package com.example.peertutoring.models;

/** Body of POST /sessions: a student asks a tutor for a session. */
public class CreateSessionRequest {

    private final String tutorId;
    private final String subject;
    private final String topic;
    private final String preferredTime;

    public CreateSessionRequest(String tutorId, String subject, String topic, String preferredTime) {
        this.tutorId = tutorId;
        this.subject = subject;
        this.topic = topic;
        this.preferredTime = preferredTime;
    }
}
