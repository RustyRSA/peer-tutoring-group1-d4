package com.example.peertutoring.models;

/** Body of PUT /sessions/:id when a tutor answers a request: status is accepted, declined or completed. */
public class UpdateSessionRequest {

    private final String status;

    public UpdateSessionRequest(String status) {
        this.status = status;
    }
}
