package com.project.ads.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

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

    private static final int MAX_ITEMS = 50;

    private final DatabaseReference mDatabase;

    public NotificationHistoryProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
    }

    /** Las últimas notificaciones, en orden ascendente de fecha (el adapter las invierte). */
    public Query getNotifications(String uid) {
        return mDatabase.child(uid).orderByChild("created_at").limitToLast(MAX_ITEMS);
    }

    public Task<Void> markAsRead(String uid, List<String> ids) {
        Map<String, Object> updates = new HashMap<>();
        for (String id : ids) {
            updates.put(id + "/read", true);
        }
        return mDatabase.child(uid).updateChildren(updates);
    }
}
