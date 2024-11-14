package com.ads.providers;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class AuthProvider {
    FirebaseAuth mAuth;
    private Context context;
    private static final String PREFS_NAME = "WorkerPrefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    public AuthProvider() {
        mAuth = FirebaseAuth.getInstance();
    }

    // Constructor sobrecargado para cuando necesitemos context
    public AuthProvider(Context context) {
        this();
        this.context = context;
    }

    public Task<AuthResult> register(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Cuando el registro es exitoso, actualizamos el token FCM
                    updateFCMToken();
                });
    }

    public Task<AuthResult> login(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Cuando el login es exitoso, actualizamos el token FCM
                    updateFCMToken();
                });
    }

    public void logOut() {
        if (context != null) {
            // Limpiamos el token guardado
            clearSavedFCMToken();
        }
        mAuth.signOut();
    }

    public String getId() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }

    public boolean exitSession() {
        return mAuth.getCurrentUser() != null;
    }

    // Nuevos métodos para manejar FCM tokens
    private void updateFCMToken() {
        if (context == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        String userId = getId();

                        if (userId != null) {
                            // Guardar token localmente
                            saveFCMToken(token);

                            // Actualizar token en la base de datos
                            WorkerProvider workerProvider = new WorkerProvider();
                            workerProvider.updateWorkerFCMToken(userId, token);
                        }
                    }
                });
    }

    public void saveFCMToken(String token) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_FCM_TOKEN, token);
            editor.apply();
        }
    }

    private void clearSavedFCMToken() {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_FCM_TOKEN);
            editor.apply();
        }
    }

    public String getSavedFCMToken() {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_FCM_TOKEN, null);
        }
        return null;
    }

    // Método para actualizar manualmente el token FCM
    public void refreshFCMToken() {
        updateFCMToken();
    }
}