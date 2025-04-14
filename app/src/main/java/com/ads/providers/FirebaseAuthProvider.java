package com.ads.providers;

import android.content.Context;
import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;


import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class FirebaseAuthProvider {
    private static final String TAG = "FirebaseAuthProvider";
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String[] SCOPES = { MESSAGING_SCOPE };


    private GoogleCredentials credentials;
    private String cachedToken = null;
    private long tokenExpireTime = 0;

    private static FirebaseAuthProvider instance;

    public static synchronized FirebaseAuthProvider getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthProvider(context);
        }
        return instance;
    }

    private FirebaseAuthProvider(Context context) {
        try {
            InputStream serviceAccount = context.getAssets().open("serviceAccountKey.json");
            credentials = GoogleCredentials.fromStream(serviceAccount)
                    .createScoped(Arrays.asList(SCOPES));
            Log.d(TAG, "Credenciales de Firebase cargadas con éxito desde assets");
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar credenciales de Firebase", e);
        }
    }

    public synchronized String getAccessToken() {
        // Verificar si el token no está expirado (con 5 minutos de buffer)
        long currentTime = System.currentTimeMillis();
        if (cachedToken != null && tokenExpireTime > currentTime + TimeUnit.MINUTES.toMillis(5)) {
            return cachedToken;
        }

        // Token expirado o no establecido, refrescarlo
        try {
            if (credentials == null) {
                Log.e(TAG, "No se pudieron inicializar las credenciales");
                return null;
            }

            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            if (token != null) {
                cachedToken = token.getTokenValue();
                tokenExpireTime = token.getExpirationTime().getTime();
                return cachedToken;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error refrescando token de Firebase", e);
        }

        return null;
    }
}