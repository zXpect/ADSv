package com.ads.providers;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class TokenProvider {
    public DatabaseReference mDatabase;

    public TokenProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Tokens");
    }

    public void create(String userId) {
        if (userId == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("TokenProvider", "Error obteniendo token FCM", task.getException());
                        return;
                    }

                    // Get the token
                    String token = task.getResult();

                    // Save it to database
                    saveToken(userId, token);
                });
    }

    // Método con un solo parámetro (token) - Usa el usuario actual
    public void saveToken(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("TokenProvider", "No hay usuario autenticado para guardar el token");
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        saveToken(userId, token);
    }

    // Método con dos parámetros (userId, token)
    public void saveToken(String userId, String token) {
        if (userId == null || token == null) {
            Log.e("TokenProvider", "UserId o token es nulo");
            return;
        }

        mDatabase.child(userId).child("token").setValue(token)
                .addOnSuccessListener(aVoid ->
                        Log.d("TokenProvider", "Token guardado exitosamente para usuario: " + userId))
                .addOnFailureListener(e ->
                        Log.e("TokenProvider", "Error al guardar token: " + e.getMessage()));
    }

    // Método para acceder al token de un usuario específico
    public DatabaseReference getToken(String userId) {
        return mDatabase.child(userId);
    }
}