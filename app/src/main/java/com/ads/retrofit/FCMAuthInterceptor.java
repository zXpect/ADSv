package com.ads.retrofit;



import android.util.Log;

import androidx.annotation.NonNull;

import com.ads.providers.FirebaseAuthProvider;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class FCMAuthInterceptor implements Interceptor {
    private final FirebaseAuthProvider authProvider;
    private static final String TAG = "FirebaseAuthProvider";

    public FCMAuthInterceptor(FirebaseAuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        String token = authProvider.getAccessToken();
        Log.d(TAG, "Obteniendo token de autenticación: " + (token != null ? "Token obtenido con éxito" : "Token nulo"));

        if (token == null) {
            Log.e(TAG, "Error crítico: No se pudo obtener el token de autenticación");
            throw new IOException("Could not retrieve Firebase authentication token");
        }

        Request original = chain.request();
        Request request = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .method(original.method(), original.body())
                .build();

        Log.d(TAG, "URL de la solicitud: " + request.url());
        Log.d(TAG, "Método: " + request.method());
        Log.d(TAG, "Headers: Authorization: Bearer " + token.substring(0, Math.min(token.length(), 20)) + "...");

        return chain.proceed(request);
    }
}