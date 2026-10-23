package com.example.peertutoring.network;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adds "Authorization: Bearer &lt;Firebase ID token&gt;" to every request. Firebase hands back the
 * cached token and refreshes it by itself when it is about to expire. Runs on OkHttp's background
 * thread, so waiting for the token here never blocks the screen.
 */
public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder request = chain.request().newBuilder();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            try {
                GetTokenResult result = Tasks.await(user.getIdToken(false));
                if (result.getToken() != null) {
                    request.header("Authorization", "Bearer " + result.getToken());
                }
            } catch (ExecutionException e) {
                throw new IOException("Could not get the login token", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while getting the login token", e);
            }
        }
        return chain.proceed(request.build());
    }
}
