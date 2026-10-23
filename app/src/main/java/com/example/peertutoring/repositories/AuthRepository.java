package com.example.peertutoring.repositories;

import android.content.Context;

import com.example.peertutoring.models.CreateUserRequest;
import com.example.peertutoring.models.StatusUpdateRequest;
import com.example.peertutoring.models.UpdateProfileRequest;
import com.example.peertutoring.models.UserProfile;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.utils.SessionManager;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * Everything to do with who the user is. Firebase Auth holds the account (email + password,
 * remembered between app starts); the backend holds the profile (name, role, status).
 * A user only counts as logged in once both exist.
 */
public class AuthRepository {

    /** Result of register / login / restoreSession. All callbacks run on the main thread. */
    public interface ProfileCallback {
        void onSuccess(UserProfile profile);

        void onError(ApiError error);
    }

    /** Result of an action that returns nothing (password reset). */
    public interface ResultCallback {
        void onSuccess();

        void onError(ApiError error);
    }

    public interface TutorsCallback {
        void onSuccess(List<UserProfile> tutors);
        void onError(ApiError error);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final SessionManager session;

    public AuthRepository(Context context) {
        session = new SessionManager(context.getApplicationContext());
    }

    /** True if Firebase still remembers a signed-in user. */
    public static boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    /**
     * Creates the Firebase account, then the backend profile. If the backend refuses the profile
     * the Firebase account is deleted again, so a failed sign-up never leaves a half-made account.
     */
    public void register(String email, String password, CreateUserRequest profile, ProfileCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser newUser = result.getUser();
                    ApiClient.get().createUser(profile).enqueue(new ApiCallback<UserProfile>() {
                        @Override
                        public void onSuccess(UserProfile created) {
                            session.saveUser(created);
                            callback.onSuccess(created);
                        }

                        @Override
                        public void onError(ApiError error) {
                            undoRegistration(newUser, error, callback);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(fromFirebase(e)));
    }

    /** Signs in with Firebase, then loads the profile from the backend. */
    public void login(String email, String password, ProfileCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> loadProfile(callback))
                .addOnFailureListener(e -> callback.onError(fromFirebase(e)));
    }

    /** For app start: Firebase already remembers the user, so only the profile needs loading. */
    public void restoreSession(ProfileCallback callback) {
        loadProfile(callback);
    }

    /** Sends Firebase's password-reset email. */
    public void sendPasswordReset(String email, ResultCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(fromFirebase(e)));
    }

    /** Signs out and forgets everything stored on the phone about the user. */
    public void logout() {
        auth.signOut();
        session.clear();
    }

    /**
     * GET /users/me. If it fails (suspended account, missing profile, server down) the user is
     * signed out again, so the app is never left half logged in.
     */
    private void loadProfile(ProfileCallback callback) {
        ApiClient.get().getMe().enqueue(new ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                session.saveUser(profile);
                callback.onSuccess(profile);
            }

            @Override
            public void onError(ApiError error) {
                logout();
                callback.onError(error);
            }
        });
    }

    private void undoRegistration(FirebaseUser user, ApiError error, ProfileCallback callback) {
        if (user == null) {
            auth.signOut();
            callback.onError(error);
            return;
        }
        user.delete().addOnCompleteListener(task -> {
            auth.signOut();
            callback.onError(error);
        });
    }

    /** Turns Firebase's exceptions into short messages for easier understanding. */
    private static ApiError fromFirebase(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return ApiError.of(ApiError.CODE_WEAK_PASSWORD, "Choose a stronger password (at least 6 characters).");
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return ApiError.of(ApiError.CODE_EMAIL_IN_USE, "An account with this email already exists.");
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException) {
            return ApiError.of(ApiError.CODE_INVALID_CREDENTIALS, "Incorrect email or password.");
        }
        if (e instanceof FirebaseNetworkException) {
            return ApiError.of(ApiError.CODE_NETWORK, "No internet connection. Please try again.");
        }
        String reason = e.getLocalizedMessage();
        return ApiError.of("AUTH_ERROR", reason != null ? reason : "Something went wrong. Please try again.");
    }

    /** Pushes the edited profile fields to the backend and stores the fresh copy on the phone. */
    public void updateProfile(UpdateProfileRequest request, ProfileCallback callback) {
        ApiClient.get().updateMe(request).enqueue(new ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile updatedProfile) {
                // Save the fresh profile to local storage
                session.saveUser(updatedProfile);
                callback.onSuccess(updatedProfile);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    /** Loads a specific user's public profile (e.g., when a student taps on a tutor). */
    public void getUserById(String uid, ProfileCallback callback) {
        ApiClient.get().getUserById(uid).enqueue(new ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                callback.onSuccess(profile);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    /** Approved tutors, best rated first. An empty query and no subject returns all of them. */
    public void searchTutors(String query, String subject, TutorsCallback callback) {
        String q = query == null || query.trim().isEmpty() ? null : query.trim();
        String s = subject == null || subject.trim().isEmpty() ? null : subject.trim();
        ApiClient.get().searchTutors(q, s).enqueue(new ApiCallback<List<UserProfile>>() {
            @Override
            public void onSuccess(List<UserProfile> tutors) {
                callback.onSuccess(tutors);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    /** Admin only: every user, newest first. */
    public void getAllUsers(TutorsCallback callback) {
        ApiClient.get().getAllUsers().enqueue(new ApiCallback<List<UserProfile>>() {
            @Override
            public void onSuccess(List<UserProfile> users) {
                callback.onSuccess(users);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    // Updates a user's status (approve tutor, suspend user, etc.).
    public void updateUserStatus(String uid, StatusUpdateRequest request, ProfileCallback callback) {
        ApiClient.get().updateUserStatus(uid, request).enqueue(new ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                callback.onSuccess(profile);
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }
}
