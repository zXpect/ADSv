package com.project.ads.helpers;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.project.ads.R;
import com.project.ads.activities.NotificationsActivity;

/**
 * Campana de notificaciones de los inicios (view_notifications_bell, mockups 06 y 13):
 * abre Notificaciones y muestra el punto si alguna de las últimas notificaciones está
 * sin leer (UnreadNotificationsWatcher). Mientras haya alguna pendiente, la campana se
 * balancea cada pocos segundos (Motion.ringBell) y el botón se resalta: círculo naranja
 * claro con borde naranja y campana naranja. Escucha y anima solo entre start y stop.
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
    // Fondo propio del botón y del punto (mutate: el dibujo lo comparten los botones de volver)
    private final GradientDrawable mButtonBg;
    private final GradientDrawable mDotBg;
    private Boolean mShownHighlight;   // resaltado que se muestra hoy (null: aún nada)
    private int mFill, mStroke, mIcon;
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
        mButtonBg = (GradientDrawable) mButton.getBackground().mutate();
        mDotBg = (GradientDrawable) mDot.getBackground().mutate();
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
        showHighlight(unread);
        mButton.setContentDescription(mActivity.getString(unread
                ? R.string.home_notifications_unread_desc : R.string.home_notifications_desc));
    }

    /**
     * Con pendientes, el círculo pasa a naranja claro con borde naranja de 2 dp y la campana
     * a naranja; sin pendientes vuelve al botón normal. El cambio se anima (300 ms) salvo la
     * primera vez que se pinta.
     */
    private void showHighlight(boolean on) {
        if (mShownHighlight != null && mShownHighlight == on) return;
        boolean animate = mShownHighlight != null;
        mShownHighlight = on;
        int fill = color(on ? R.color.ads_chip_bg : R.color.ads_card);
        int stroke = color(on ? R.color.ads_brand : R.color.ads_line);
        int icon = color(on ? R.color.ads_brand_text : R.color.ads_charcoal);
        int strokeWidth = mActivity.getResources().getDimensionPixelSize(
                on ? R.dimen.stroke_focus : R.dimen.stroke_control);
        int dotBorder = mActivity.getResources().getDimensionPixelSize(R.dimen.stroke_focus);
        int fromFill = animate ? mFill : fill, fromStroke = animate ? mStroke : stroke, fromIcon = animate ? mIcon : icon;
        mFill = fill;
        mStroke = stroke;
        mIcon = icon;
        Motion.colorTo(mButton, fromFill, fill, animate, c -> {
            mButtonBg.setColor(c);
            mDotBg.setStroke(dotBorder, c);   // el borde del punto sigue el fondo del botón
        });
        Motion.colorTo(mButton, fromStroke, stroke, animate, c -> mButtonBg.setStroke(strokeWidth, c));
        Motion.colorTo(mBell, fromIcon, icon, animate, c -> ImageViewCompat.setImageTintList(mBell, ColorStateList.valueOf(c)));
        if (animate && on) Motion.bump(mButton);
    }

    private int color(int res) {
        return ContextCompat.getColor(mActivity, res);
    }
}
