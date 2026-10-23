package com.example.peertutoring.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Creates the one Retrofit instance used for every backend call. */
public final class ApiClient {

    private static ApiService service;

    private ApiClient() {
    }

    public static synchronized ApiService get() {
        if (service == null) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor())
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            service = new Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
        }
        return service;
    }
}
