package com.ads.providers;

import android.util.Log;

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

public class NotificationProvider {
    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private static String SERVER_KEY;
    private final Context context;
    private static final String NETLIFY_FUNCTION_URL = "https://adsv.netlify.app/.netlify/functions/sendNotification";

    public NotificationProvider(Context context) {
        this.context = context;
    }


    public Task<Void> sendNotificationViaNetlify(String workerToken, String title, String body, Map<String, Object> requestData) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        try {
            JSONObject json = new JSONObject();
            json.put("token", workerToken);
            json.put("title", title);
            json.put("body", body);

            JSONObject data = new JSONObject();
            for (Map.Entry<String, Object> entry : requestData.entrySet()) {
                data.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            json.put("data", data);

            Log.d("NotificationDebug", "URL de la función Netlify: " + NETLIFY_FUNCTION_URL);
            Log.d("NotificationDebug", "Payload de la notificación: " + json.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    NETLIFY_FUNCTION_URL + "/.netlify/functions/sendNotification", // Asegúrate de que esta ruta sea correcta
                    json,
                    response -> {
                        Log.d("NotificationDebug", "Respuesta de Netlify: " + response.toString());
                        taskCompletionSource.setResult(null);
                    },
                    error -> {
                        Log.e("NotificationDebug", "Error al enviar notificación: " + error.toString());

                        // Detalle del error de red
                        if (error.networkResponse != null) {
                            Log.e("NotificationDebug", "Código de error: " + error.networkResponse.statusCode);
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                Log.e("NotificationDebug", "Cuerpo de la respuesta: " + responseBody);
                            } catch (Exception e) {
                                Log.e("NotificationDebug", "No se pudo obtener el cuerpo del error");
                            }
                        } else {
                            Log.e("NotificationDebug", "Error de conexión o timeout");
                        }

                        taskCompletionSource.setException(error);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            // Configurar un timeout más largo para áreas con conexión lenta
            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000, // 30 segundos de timeout
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e("NotificationDebug", "Error creando el JSON: " + e.getMessage(), e);
            taskCompletionSource.setException(e);
        }

        return taskCompletionSource.getTask();
    }
}