package com.example.peertutoring.network;

import com.example.peertutoring.models.AppNotification;
import com.example.peertutoring.models.ChatMessage;
import com.example.peertutoring.models.Conversation;
import com.example.peertutoring.models.CreateSessionRequest;
import com.example.peertutoring.models.CreateUserRequest;
import com.example.peertutoring.models.RatingRequest;
import com.example.peertutoring.models.Review;
import com.example.peertutoring.models.SendMessageRequest;
import com.example.peertutoring.models.StatusUpdateRequest;
import com.example.peertutoring.models.TutorSession;
import com.example.peertutoring.models.UpdateProfileRequest;
import com.example.peertutoring.models.UpdateSessionRequest;
import com.example.peertutoring.models.UserProfile;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** The backend routes the app calls. Every route needs the Firebase login token (added by AuthInterceptor). */
public interface ApiService {

    // ---- Users ----

    /** Creates the profile for the account that has just been signed up. */
    @POST("users")
    Call<UserProfile> createUser(@Body CreateUserRequest request);

    /** The signed-in user's own full profile. */
    @GET("users/me")
    Call<UserProfile> getMe();

    /** Saves changes to the signed-in user's own profile and returns the updated profile. */
    @PUT("users/me")
    Call<UserProfile> updateMe(@Body UpdateProfileRequest request);

    /**
     * Approved tutors, best rated first. The optional query matches a name or a subject; the
     * optional subject keeps only tutors who offer exactly that subject.
     */
    @GET("users/tutors")
    Call<List<UserProfile>> searchTutors(@Query("q") String query, @Query("subject") String subject);

    /** Another user's public profile (for example a tutor the student tapped on). */
    @GET("users/{uid}")
    Call<UserProfile> getUserById(@Path("uid") String uid);

    /** Admin only: every user, newest first. */
    @GET("users")
    Call<List<UserProfile>> getAllUsers();

    /** Admin only: approve or reject a tutor, suspend or reactivate an account. */
    @PUT("users/{uid}/status")
    Call<UserProfile> updateUserStatus(@Path("uid") String uid, @Body StatusUpdateRequest request);

    // ---- Messages ----

    /** The signed-in user's inbox: one row per person they have talked to, latest first. */
    @GET("messages")
    Call<List<Conversation>> getConversations();

    /** All messages between the signed-in user and one other user, oldest first. */
    @GET("messages/{uid}")
    Call<List<ChatMessage>> getThread(@Path("uid") String otherUid);

    /** Sends a message to another user. */
    @POST("messages")
    Call<ChatMessage> sendMessage(@Body SendMessageRequest request);

    // ---- Notifications ----

    /** The signed-in user's notifications, newest first. */
    @GET("notifications")
    Call<List<AppNotification>> getNotifications();

    /** Marks one notification as read (204, no body). */
    @PUT("notifications/{id}/read")
    Call<Void> markNotificationRead(@Path("id") String notificationId);

    /** Marks every notification as read (204, no body). */
    @PUT("notifications/read-all")
    Call<Void> markAllNotificationsRead();

    // ---- Ratings ----

    /** Rates the tutor of one completed session (once per session). */
    @POST("ratings")
    Call<Review> submitRating(@Body RatingRequest request);

    /** The latest reviews of one tutor, newest first. */
    @GET("ratings/tutor/{uid}")
    Call<List<Review>> getTutorReviews(@Path("uid") String tutorId);

    // ---- Sessions (bookings) ----

    /** A student asks an approved tutor for a session (status starts as "pending"). */
    @POST("sessions")
    Call<TutorSession> createSession(@Body CreateSessionRequest request);

    /** The signed-in user's sessions, as a student and as a tutor, newest first. */
    @GET("sessions")
    Call<List<TutorSession>> getSessions();

    /** A tutor accepts, declines or completes a session. */
    @PUT("sessions/{id}")
    Call<TutorSession> updateSession(@Path("id") String sessionId, @Body UpdateSessionRequest request);

    /** The student cancels (deletes) a session (204, no body). */
    @DELETE("sessions/{id}")
    Call<Void> cancelSession(@Path("id") String sessionId);
}
