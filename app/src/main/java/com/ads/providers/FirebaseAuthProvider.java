package com.ads.providers;

import android.content.Context;
import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ServiceAccountCredentials;

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

            // Validate service account details
            if (credentials instanceof ServiceAccountCredentials) {
                ServiceAccountCredentials serviceAccountCreds = (ServiceAccountCredentials) credentials;
                Log.d(TAG, "Service Account Email: " + serviceAccountCreds.getClientEmail());
                Log.d(TAG, "Project ID: " + serviceAccountCreds.getProjectId());
            }

            Log.d(TAG, "Credenciales de Firebase cargadas con éxito desde assets");
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar credenciales de Firebase - Verifique que serviceAccountKey.json existe en assets/", e);
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado al cargar credenciales", e);
        }
    }

    public synchronized String getAccessToken() {
        Log.d(TAG, "Obteniendo token de autenticación: Token " + (cachedToken != null ? "cacheado" : "nulo"));

        // Verificar si el token no está expirado (con 5 minutos de buffer)
        long currentTime = System.currentTimeMillis();
        if (cachedToken != null && tokenExpireTime > currentTime + TimeUnit.MINUTES.toMillis(5)) {
            Log.d(TAG, "Usando token cacheado válido");
            return cachedToken;
        }

        // Token expirado o no establecido, refrescarlo
        try {
            if (credentials == null) {
                Log.e(TAG, "Error crítico: Credenciales no inicializadas - revisar serviceAccountKey.json");
                return null;
            }

            Log.d(TAG, "Refrescando token de autenticación...");
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();

            if (token != null) {
                cachedToken = token.getTokenValue();
                tokenExpireTime = token.getExpirationTime().getTime();
                Log.d(TAG, "Token obtenido exitosamente. Expira en: " +
                        (tokenExpireTime - currentTime) / 1000 + " segundos");
                return cachedToken;
            } else {
                Log.e(TAG, "Error crítico: Token obtenido es null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error refrescando token de Firebase", e);

            // Información adicional para debug
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Invalid JWT Signature")) {
                    Log.e(TAG, "SOLUCIÓN: Regenerar service account key en Firebase Console");
                } else if (e.getMessage().contains("invalid_grant")) {
                    Log.e(TAG, "SOLUCIÓN: Verificar que el service account key sea válido y no esté corrupto");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado al obtener token", e);
        }

        Log.e(TAG, "Error crítico: No se pudo obtener el token de autenticación");
        return null;
    }

    /**
     * Fuerza la invalidación del token cacheado
     */
    public synchronized void invalidateToken() {
        Log.d(TAG, "Invalidando token cacheado");
        cachedToken = null;
        tokenExpireTime = 0;
    }

    /**
     * Verifica si las credenciales están correctamente inicializadas
     */
    public boolean isInitialized() {
        return credentials != null;
    }
}