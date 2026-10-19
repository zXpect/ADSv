package com.project.ads.providers;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Historial de notificaciones del usuario (mockups 40 y 53).
 *
 * Contrato acordado con el backend (diagnostico-rediseno §5.3):
 * notifications/<uid>/<id> = {title, body, type, request_id, read, created_at}
 * con created_at en milisegundos. El backend lo escribe al enviar cada push;
 * la regla debe dejar al dueño leer su nodo y escribir solo el campo "read".
 */
public class NotificationHistoryProvider {

    private static final String TAG = "NotificationHistory";

    private static final int MAX_ITEMS = 50;

    private final DatabaseReference mDatabase;

    public NotificationHistoryProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
    }

    /** Las últimas notificaciones, en orden ascendente de fecha (el adapter las invierte). */
    public Query getNotifications(String uid) {
        return mDatabase.child(uid).orderByChild("created_at").limitToLast(MAX_ITEMS);
    }

    /**
     * Marca como leídas las notificaciones sin leer de una solicitud, entre las
     * últimas 50. Se llama al abrir el detalle de la solicitud por cualquier camino.
     */
    public void markRequestAsRead(String uid, String requestId) {
        getNotifications(uid).get().addOnSuccessListener(snapshot -> {
            List<String> ids = new ArrayList<>();
            for (DataSnapshot child : snapshot.getChildren()) {
                Boolean read = child.child("read").getValue(Boolean.class);
                if (requestId.equals(child.child("request_id").getValue(String.class))
                        && (read == null || !read)) {
                    ids.add(child.getKey());
                }
            }
            if (!ids.isEmpty()) {
                markAsRead(uid, ids).addOnFailureListener(e ->
                        Log.w(TAG, "No se pudieron marcar las notificaciones de " + requestId + ": " + e.getMessage()));
            }
        }).addOnFailureListener(e -> Log.w(TAG, "No se pudo leer el historial: " + e.getMessage()));
    }

    public Task<Void> markAsRead(String uid, List<String> ids) {
        Map<String, Object> updates = new HashMap<>();
        for (String id : ids) {
            updates.put(id + "/read", true);
        }
        return mDatabase.child(uid).updateChildren(updates);
    }
}
