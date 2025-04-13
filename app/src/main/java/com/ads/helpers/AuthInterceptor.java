package com.ads.helpers;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {

        FileInputStream serviceAccountStream = new FileInputStream("functions/serviceAccountKey.json");

        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream)
                .createScoped("https://www.googleapis.com/auth/firebase.messaging");

        credentials.refreshIfExpired();
        String token = credentials.getAccessToken().getTokenValue();

        Request newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        return chain.proceed(newRequest);
    }
}
