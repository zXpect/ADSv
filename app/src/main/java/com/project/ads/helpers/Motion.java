package com.project.ads.helpers;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.BitmapDescriptor;
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

    /** Fundido corto de un texto que acaba de cambiar. */
    public static void fadeIn(View view) {
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
    }

    /** Muestra una tarjeta oculta subiendo con un fundido; si ya se ve, no hace nada. */
    public static void reveal(View view) {
        if (view.getVisibility() == View.VISIBLE) return;
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(24 * view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1f).translationY(0f)
                .setDuration(240)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    /** Se llena desde el borde inicial: barras (horizontal) o líneas (vertical) de avance. */
    public static void fill(View view, boolean horizontal) {
        view.setPivotX(0f);
        view.setPivotY(0f);
        if (horizontal) view.setScaleX(0f); else view.setScaleY(0f);
        view.animate().scaleX(1f).scaleY(1f)
                .setDuration(320)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    /**
     * Entrada escalonada de las filas la primera vez que la lista tiene datos.
     * Llamar después de notificar al adapter; las actualizaciones siguientes
     * (Firebase, paginación) no se vuelven a animar.
     */
    public static void animateListOnce(RecyclerView list) {
        if (list.getTag(R.id.tag_list_animated) != null) return;
        RecyclerView.Adapter<?> adapter = list.getAdapter();
        if (adapter == null || adapter.getItemCount() == 0) return;
        list.setTag(R.id.tag_list_animated, Boolean.TRUE);
        animateList(list);
    }

    /** Entrada escalonada de las filas en el próximo layout (p. ej. al cambiar de pestaña). */
    public static void animateList(RecyclerView list) {
        if (!enabled(list.getContext())) return;
        list.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(list.getContext(), R.anim.ads_list_layout));
        list.scheduleLayoutAnimation();
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

    /**
     * Pin seleccionado: crece desde el tamaño normal hasta pasarse un poco y se
     * asienta (0,8 → 1,12 → 0,96 → 1), mientras da un saltito sobre su punta.
     */
    public static ValueAnimator popPin(Marker marker, BitmapDescriptor[] frames, float anchorV) {
        ValueAnimator animator = ValueAnimator.ofFloat(MapPinHelper.POP_FROM, 1.12f, 0.96f, 1f);
        animator.setDuration(340);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            try {
                marker.setIcon(MapPinHelper.frameFor(frames, (float) a.getAnimatedValue()));
                // Salto: sube hasta 0,12 del alto y vuelve, en media sinusoide
                float hop = 0.12f * (float) Math.sin(Math.PI * a.getAnimatedFraction());
                marker.setAnchor(0.5f, anchorV + hop);
            } catch (IllegalArgumentException e) {
                a.cancel();
            }
        });
        animator.start();
        return animator;
    }

    /** Pin que deja de estar seleccionado: se encoge al tamaño normal y luego llama a onEnd. */
    public static ValueAnimator shrinkPin(Marker marker, BitmapDescriptor[] frames, float anchorV,
                                          Runnable onEnd) {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, MapPinHelper.POP_FROM);
        animator.setDuration(160);
        animator.addUpdateListener(a -> {
            try {
                marker.setIcon(MapPinHelper.frameFor(frames, (float) a.getAnimatedValue()));
                marker.setAnchor(0.5f, anchorV);
            } catch (IllegalArgumentException e) {
                a.cancel();
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                onEnd.run();
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
