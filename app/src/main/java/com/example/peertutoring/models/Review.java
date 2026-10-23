package com.example.peertutoring.models;

/** One review of a tutor, as returned by GET /ratings/tutor/:uid. */
public class Review {

    private int stars;
    private String comment;
    private String studentName;
    private String createdAt;

    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public String getStudentName() { return studentName; }
    public String getCreatedAt() { return createdAt; }
}
