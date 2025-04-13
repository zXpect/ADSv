package com.ads.providers;

import android.util.Log;

import com.ads.models.FCMBody;
import com.ads.models.FCMResponse;
import com.ads.retrofit.IFCMApi;
import com.ads.retrofit.RetrofitClient;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONObject;
import org.json.JSONException;
import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

public class NotificationProvider {

    private String url = "https://fcm.googleapis.com";

    public NotificationProvider() {
    }

    public Call<FCMResponse> sendNotification(FCMBody body) {
        return RetrofitClient.getClientObject(url).create(IFCMApi.class).send(body);
    }
}