package com.project.ads.helpers;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.Marker;
import com.project.ads.R;

/**
 * Animaciones cortas de la app (150-350 ms). Los animadores respetan la escala
 * de animación del sistema; con «Quitar animaciones» activo se muestra directo
 * el estado final.
 */
public final class Motion {

    private Motion() {
    }

    /** false si el usuario quitó las animaciones en los ajustes del teléfono. */
    public static boolean enabled(Context context) {
        return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }

    /** Entrada de un ícono de resultado: crece desde 0,6 con un leve sobrepaso. */
    public static void popIn(View view) {
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.setAlpha(0f);
        view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(280)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();
    }

    /** Ícono de resultado; si es el check, se dibuja el trazo (avd_ads_check). */
    public static void showResultIcon(ImageView view, int iconRes) {
        if (iconRes == R.drawable.ic_ads_check && enabled(view.getContext())) {
            Drawable check = ContextCompat.getDrawable(view.getContext(), R.drawable.avd_ads_check);
            view.setImageDrawable(check);
            if (check instanceof Animatable) ((Animatable) check).start();
        } else {
            view.setImageResource(iconRes);
        }
        popIn(view);
    }

    /** Pulso breve al elegir un valor (estrellas). */
    public static void bump(View view) {
        view.animate().cancel();
        view.setScaleX(1.25f);
        view.setScaleY(1.25f);
        view.animate().scaleX(1f).scaleY(1f)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator(3f))
                .start();
    }

    /** Fundido de entrada de un marcador nuevo del mapa. */
    public static ValueAnimator fadeIn(Marker marker) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(250);
        marker.setAlpha(0f);
        animator.addUpdateListener(a -> {
            try {
                marker.setAlpha((float) a.getAnimatedValue());
            } catch (IllegalArgumentException e) {
                // El marcador se quitó del mapa mientras se animaba
                a.cancel();
            }
        });
        animator.start();
        return animator;
    }

    /** El pin cae desde un poco más arriba y rebota en su punta (ancla anchorV). */
    public static ValueAnimator drop(Marker marker, float anchorV) {
        ValueAnimator animator = ValueAnimator.ofFloat(0.35f, 0f);
        animator.setDuration(350);
        animator.setInterpolator(new BounceInterpolator());
        animator.addUpdateListener(a -> {
            try {
                marker.setAnchor(0.5f, anchorV + (float) a.getAnimatedValue());
            } catch (IllegalArgumentException e) {
                a.cancel();
            }
        });
        animator.start();
        return animator;
    }
}
