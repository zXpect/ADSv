package com.ads.providers;

import android.content.Context;

import com.ads.models.FCMBody;
import com.ads.models.FCMResponse;
import com.ads.retrofit.IFCMApi;
import com.ads.retrofit.RetrofitClient;

import retrofit2.Call;

public class NotificationProvider {
    private static final String TAG = "NotificationProvider";
    private final String url = "https://fcm.googleapis.com";
    private final FirebaseAuthProvider authProvider;

    public NotificationProvider(Context context) {
        authProvider = FirebaseAuthProvider.getInstance(context);
    }

    public Call<FCMResponse> sendNotification(FCMBody body) {
        return RetrofitClient.getClientObject(url, authProvider)
                .create(IFCMApi.class)
                .send(body);
    }
}