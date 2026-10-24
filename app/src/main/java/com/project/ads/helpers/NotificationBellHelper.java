package com.project.ads.helpers;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.project.ads.R;
import com.project.ads.activities.NotificationsActivity;

/**
 * Campana de notificaciones de los inicios (view_notifications_bell, mockups 06 y 13):
 * abre Notificaciones y muestra el punto si alguna de las últimas notificaciones está
 * sin leer (UnreadNotificationsWatcher). Escucha solo entre start y stop.
 */
public class NotificationBellHelper {

    private final Activity mActivity;
    private final View mButton;
    private final View mDot;
    private final UnreadNotificationsWatcher mWatcher;

    public NotificationBellHelper(Activity activity) {
        mActivity = activity;
        mButton = activity.findViewById(R.id.btn_home_notifications);
        mDot = activity.findViewById(R.id.view_notifications_dot);
        mButton.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, NotificationsActivity.class)));
        mWatcher = new UnreadNotificationsWatcher(count -> showUnread(count > 0));
    }

    /** Llamar en onStart. */
    public void start() {
        mWatcher.start();
    }

    /** Llamar en onStop. */
    public void stop() {
        mWatcher.stop();
    }

    private void showUnread(boolean unread) {
        mDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        mButton.setContentDescription(mActivity.getString(unread
                ? R.string.home_notifications_unread_desc : R.string.home_notifications_desc));
    }
}
