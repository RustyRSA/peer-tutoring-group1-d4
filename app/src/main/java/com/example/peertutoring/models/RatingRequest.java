package com.example.peertutoring.models;

/** Body of POST /ratings. A null comment is left out of the JSON. */
public class RatingRequest {

    private final String sessionId;
    private final int stars;       // 1 to 5
    private final String comment;  // optional

    public RatingRequest(String sessionId, int stars, String comment) {
        this.sessionId = sessionId;
        this.stars = stars;
        this.comment = comment;
    }
}
