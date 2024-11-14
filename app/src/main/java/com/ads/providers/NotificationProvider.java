package com.ads.providers;

import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import org.json.JSONObject;
import org.json.JSONException;
import android.content.Context;
import java.util.Map;
import java.util.HashMap;

public class NotificationProvider {
    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "TU_SERVER_KEY_DE_FIREBASE"; // Obtén esto de la consola de Firebase
    private final Context context;

    public NotificationProvider(Context context) {
        this.context = context;
    }

    public Task<Void> sendNotificationToWorker(String workerToken, String title, String body, Map<String, Object> requestData) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);

            JSONObject data = new JSONObject();
            for (Map.Entry<String, Object> entry : requestData.entrySet()) {
                data.put(entry.getKey(), String.valueOf(entry.getValue()));
            }

            JSONObject mainObj = new JSONObject();
            mainObj.put("to", workerToken);
            mainObj.put("notification", notification);
            mainObj.put("data", data);
            mainObj.put("priority", "high");

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    FCM_API,
                    mainObj,
                    response -> {
                        Log.d("FCM", "Notification sent successfully");
                        taskCompletionSource.setResult(null);
                    },
                    error -> {
                        Log.e("FCM", "Error sending notification: " + error.getMessage());
                        taskCompletionSource.setException(error);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "key=" + SERVER_KEY);
                    return headers;
                }
            };

            // Añadir a la cola de peticiones
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e("FCM", "Error creating JSON: " + e.getMessage());
            taskCompletionSource.setException(e);
        }

        return taskCompletionSource.getTask();
    }
}