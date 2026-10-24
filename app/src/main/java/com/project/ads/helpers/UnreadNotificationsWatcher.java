package com.project.ads.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.NotificationHistoryProvider;

/**
 * Cuántas de las últimas notificaciones están sin leer (read distinto de true).
 * Lee la misma consulta que NotificationsActivity; la usan la campana de los
 * inicios y la fila «Notificaciones» de Mi cuenta. Escucha solo entre start y stop.
 */
public class UnreadNotificationsWatcher {

    private static final String TAG = "UnreadNotifications";

    public interface Listener {
        void onUnreadCount(int count);
    }

    private final Listener mListener;
    private Query mQuery;
    private ValueEventListener mValueListener;

    public UnreadNotificationsWatcher(Listener listener) {
        mListener = listener;
    }

    /** Llamar en onStart. */
    public void start() {
        String uid = new AuthProvider().getId();
        if (uid == null || mValueListener != null) return;
        mQuery = new NotificationHistoryProvider().getNotifications(uid);
        mValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unread = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean read = child.child("read").getValue(Boolean.class);
                    if (read == null || !read) unread++;
                }
                mListener.onUnreadCount(unread);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Sin permiso o sin red: sin conteo
                Log.w(TAG, "No se pudo leer el historial: " + error.getMessage());
                mListener.onUnreadCount(0);
            }
        };
        mQuery.addValueEventListener(mValueListener);
    }

    /** Llamar en onStop. */
    public void stop() {
        if (mQuery != null && mValueListener != null) mQuery.removeEventListener(mValueListener);
        mQuery = null;
        mValueListener = null;
    }

    /** Texto de una insignia de conteo: "1".."9" y "9+" desde 10; vacío si no hay. */
    public static String badgeLabel(int count) {
        if (count <= 0) return "";
        return count > 9 ? "9+" : String.valueOf(count);
    }
}
