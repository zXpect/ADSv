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
    private static final String TAG = "NotificationDebug";
    // URL correcta de la función Netlify
    private static final String NETLIFY_FUNCTION_URL = "https://adsv.netlify.app/.netlify/functions/sendNotification";
    private final Context context;

    public NotificationProvider(Context context) {
        this.context = context;
    }

    // Modificación sugerida para NotificationProvider.java
    public Task<Void> sendNotificationViaNetlify(String token, String title, String body, Map<String, Object> data) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        try {

            String url = "https://adsv.netlify.app/.netlify/functions/sendNotification";

            // Para depuración
            Log.d("NotificationProvider", "Enviando notificación a: " + url);
            Log.d("NotificationProvider", "Token: " + token);
            Log.d("NotificationProvider", "Datos: " + data.toString());

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("token", token);
                jsonBody.put("title", title);
                jsonBody.put("body", body);

                JSONObject dataJson = new JSONObject();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    dataJson.put(entry.getKey(), entry.getValue());
                }
                jsonBody.put("data", dataJson);

                Log.d("NotificationProvider", "JSON a enviar: " + jsonBody.toString());
            } catch (JSONException e) {
                Log.e("NotificationProvider", "Error creando JSON: ", e);
                taskCompletionSource.setException(e);
                return taskCompletionSource.getTask();
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        Log.d("NotificationProvider", "Respuesta recibida: " + response.toString());
                        taskCompletionSource.setResult(null);
                    },
                    error -> {
                        Log.e("NotificationProvider", "Error en la solicitud: " + error.toString());
                        // Mostrar más detalles del error si están disponibles
                        if (error.networkResponse != null) {
                            Log.e("NotificationProvider", "Código de estado: " + error.networkResponse.statusCode);
                            Log.e("NotificationProvider", "Datos: " + new String(error.networkResponse.data));
                        }
                        taskCompletionSource.setException(error);
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            // Aumentar el timeout si es necesario
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    30000,  // 30 segundos de timeout
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // Agregar a la cola de Volley
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(jsonObjectRequest);

        } catch (Exception e) {
            Log.e("NotificationProvider", "Error general: ", e);
            taskCompletionSource.setException(e);
        }

        return taskCompletionSource.getTask();
    }
}