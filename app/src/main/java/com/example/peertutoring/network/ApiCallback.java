package com.example.peertutoring.network;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Retrofit callback reduced to two cases: {@link #onSuccess} or {@link #onError}. Both run on the
 * main thread, so they can touch the UI directly. For responses without a body (204) the body is null.
 */
public abstract class ApiCallback<T> implements Callback<T> {

    public abstract void onSuccess(T body);

    public abstract void onError(ApiError error);

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful()) {
            onSuccess(response.body());
        } else {
            onError(ApiError.fromResponse(response));
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (!call.isCanceled()) {
            onError(ApiError.fromThrowable(t));
        }
    }
}
