package com.project.ads.helpers;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.project.ads.R;
import com.project.ads.activities.NotificationsActivity;

/**
 * Campana de notificaciones de los inicios (view_notifications_bell, mockups 06 y 13):
 * abre Notificaciones y muestra el punto si alguna de las últimas notificaciones está
 * sin leer (UnreadNotificationsWatcher). Mientras haya alguna pendiente, la campana se
 * balancea cada pocos segundos (Motion.ringBell). Escucha y anima solo entre start y stop.
 */
public class NotificationBellHelper {

    private final Activity mActivity;
    private final View mButton;
    private final View mDot;
    private final android.widget.ImageView mBell;
    private final UnreadNotificationsWatcher mWatcher;
    private static final long RING_EVERY_MS = 4000;
    private boolean mUnread;
    private boolean mStarted;
    private final Runnable mRingLoop = new Runnable() {
        @Override
        public void run() {
            if (!mUnread || !mStarted) return;
            Motion.ringBell(mBell);
            mBell.postDelayed(this, RING_EVERY_MS);
        }
    };

    public NotificationBellHelper(Activity activity) {
        mActivity = activity;
        mButton = activity.findViewById(R.id.btn_home_notifications);
        mDot = activity.findViewById(R.id.view_notifications_dot);
        mBell = activity.findViewById(R.id.iv_notifications_bell);
        mButton.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, NotificationsActivity.class)));
        mWatcher = new UnreadNotificationsWatcher(count -> showUnread(count > 0));
    }

    /** Llamar en onStart. */
    public void start() {
        mStarted = true;
        mWatcher.start();
        restartRinging();
    }

    /** Llamar en onStop. */
    public void stop() {
        mStarted = false;
        mWatcher.stop();
        mBell.removeCallbacks(mRingLoop);
        mBell.setRotation(0f);
    }

    /** Suena enseguida si hay pendientes y repite cada RING_EVERY_MS; si no, se queda quieta. */
    private void restartRinging() {
        mBell.removeCallbacks(mRingLoop);
        if (mUnread && mStarted) mBell.postDelayed(mRingLoop, 600);
        else mBell.setRotation(0f);
    }

    private void showUnread(boolean unread) {
        boolean changed = unread != mUnread;
        mUnread = unread;
        if (changed) restartRinging();
        mDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        mButton.setContentDescription(mActivity.getString(unread
                ? R.string.home_notifications_unread_desc : R.string.home_notifications_desc));
    }
}
