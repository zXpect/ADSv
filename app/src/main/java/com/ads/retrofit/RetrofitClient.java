package com.ads.retrofit;

import com.ads.providers.FirebaseAuthProvider;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static Retrofit retrofitObject = null;

    public static Retrofit getClient(String url, FirebaseAuthProvider authProvider) {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new FCMAuthInterceptor(authProvider))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static Retrofit getClientObject(String url, FirebaseAuthProvider authProvider) {
        if (retrofitObject == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new FCMAuthInterceptor(authProvider))
                    .build();

            retrofitObject = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitObject;
    }
}