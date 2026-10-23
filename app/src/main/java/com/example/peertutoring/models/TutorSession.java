package com.example.peertutoring.models;

/** One booking, as returned by the backend's /sessions routes. Gson fills the fields by name. */
public class TutorSession {

    private String id;
    private String studentId;
    private String studentName;
    private String tutorId;
    private String tutorName;
    private String subject;
    private String topic;
    private String preferredTime;
    private String status;      // pending, accepted, declined, completed
    private String createdAt;
    private String updatedAt;
    private boolean rated;      // true once the student has rated this session

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getTutorId() { return tutorId; }
    public String getTutorName() { return tutorName; }
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public String getPreferredTime() { return preferredTime; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public boolean isRated() { return rated; }
}
