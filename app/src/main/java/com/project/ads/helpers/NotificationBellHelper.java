package com.project.ads.helpers;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.activities.NotificationsActivity;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.NotificationHistoryProvider;

/**
 * Campana de notificaciones de los inicios (view_notifications_bell, mockups 06 y 13):
 * abre Notificaciones y muestra el punto si alguna de las últimas notificaciones está
 * sin leer. Lee la misma lista que NotificationsActivity; escucha solo entre start y stop.
 */
public class NotificationBellHelper {

    private static final String TAG = "NotificationBell";

    private final Activity mActivity;
    private final View mButton;
    private final View mDot;
    private Query mQuery;
    private ValueEventListener mListener;

    public NotificationBellHelper(Activity activity) {
        mActivity = activity;
        mButton = activity.findViewById(R.id.btn_home_notifications);
        mDot = activity.findViewById(R.id.view_notifications_dot);
        mButton.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, NotificationsActivity.class)));
    }

    /** Llamar en onStart. */
    public void start() {
        String uid = new AuthProvider().getId();
        if (uid == null || mListener != null) return;
        mQuery = new NotificationHistoryProvider().getNotifications(uid);
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean unread = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean read = child.child("read").getValue(Boolean.class);
                    if (read == null || !read) {
                        unread = true;
                        break;
                    }
                }
                showUnread(unread);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Sin permiso o sin red: la campana sigue abriendo Notificaciones, sin punto
                Log.w(TAG, "No se pudo leer el historial: " + error.getMessage());
                showUnread(false);
            }
        };
        mQuery.addValueEventListener(mListener);
    }

    /** Llamar en onStop. */
    public void stop() {
        if (mQuery != null && mListener != null) mQuery.removeEventListener(mListener);
        mQuery = null;
        mListener = null;
    }

    private void showUnread(boolean unread) {
        mDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        mButton.setContentDescription(mActivity.getString(unread
                ? R.string.home_notifications_unread_desc : R.string.home_notifications_desc));
    }
}
