package com.example.peertutoring.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * One error shape for the whole app. Backend errors look like
 * {"error": "...", "code": "...", "fields": {"yearOfStudy": "..."}} and are copied across, so a
 * screen can show {@link #getMessage()} and put each field message under its input.
 */
public class ApiError {

    public static final String CODE_NETWORK = "NETWORK_ERROR";
    public static final String CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String CODE_EMAIL_IN_USE = "EMAIL_IN_USE";
    public static final String CODE_WEAK_PASSWORD = "WEAK_PASSWORD";

    private final int httpStatus;
    private final String code;
    private final String message;
    private final Map<String, String> fields;

    private ApiError(int httpStatus, String code, String message, Map<String, String> fields) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.fields = fields;
    }

    // An error that did not come from the backend (for example from Firebase Auth).
    public static ApiError of(String code, String message) {
        return new ApiError(0, code, message, new HashMap<>());
    }

    // The request never got an answer: server down, no connection or login token unavailable.
    public static ApiError fromThrowable(Throwable t) {
        return of(CODE_NETWORK, "Can't reach the server. Check your connection and that the backend is running.");
    }

    //Reads the backend's JSON error body from a non-2xx response.
    public static ApiError fromResponse(Response<?> response) {
        String message = "Something went wrong (" + response.code() + "). Please try again.";
        String code = "HTTP_" + response.code();
        Map<String, String> fields = new HashMap<>();

        try {
            ResponseBody body = response.errorBody();
            if (body != null) {
                JSONObject json = new JSONObject(body.string());
                message = json.optString("error", message);
                code = json.optString("code", code);
                JSONObject fieldErrors = json.optJSONObject("fields");
                if (fieldErrors != null) {
                    Iterator<String> keys = fieldErrors.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        fields.put(key, fieldErrors.optString(key));
                    }
                }
            }
        } catch (IOException | JSONException ignored) {
            // Not JSON (for example an HTML error page): keep the generic message
        }
        return new ApiError(response.code(), code, message, fields);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    // The message for one input field, or null if that field had no error.
    public String getFieldError(String fieldName) {
        return fields.get(fieldName);
    }
}
