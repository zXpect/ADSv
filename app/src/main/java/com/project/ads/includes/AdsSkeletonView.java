package com.project.ads.includes;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.project.ads.R;

/**
 * Tarjetas de relleno que laten mientras carga una lista (en lugar de la rueda de
 * carga). Se muestra y oculta con setVisibility, igual que el ProgressBar que reemplaza;
 * la animación solo corre mientras está visible.
 */
public class AdsSkeletonView extends LinearLayout {

    private static final int CARDS = 4;
    private ObjectAnimator mPulse;

    public AdsSkeletonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < CARDS; i++) {
            inflater.inflate(R.layout.item_skeleton_card, this, true);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        // Al refrescar con la lista ya llena basta la rueda del gesto: no se tapa la lista
        if (visibility == VISIBLE && siblingListHasItems()) visibility = GONE;
        super.setVisibility(visibility);
    }

    private boolean siblingListHasItems() {
        if (!(getParent() instanceof ViewGroup)) return false;
        ViewGroup parent = (ViewGroup) getParent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof RecyclerView && child.getVisibility() == VISIBLE) {
                RecyclerView.Adapter<?> adapter = ((RecyclerView) child).getAdapter();
                return adapter != null && adapter.getItemCount() > 0;
            }
        }
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePulse();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPulse();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updatePulse();
    }

    private void updatePulse() {
        if (isAttachedToWindow() && isShown()) {
            if (mPulse == null) {
                // Respeta la escala de animaciones del sistema (sin animación si está apagada)
                mPulse = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0.45f);
                mPulse.setDuration(850);
                mPulse.setRepeatMode(ValueAnimator.REVERSE);
                mPulse.setRepeatCount(ValueAnimator.INFINITE);
                mPulse.start();
            }
        } else {
            stopPulse();
        }
    }

    private void stopPulse() {
        if (mPulse != null) {
            mPulse.cancel();
            mPulse = null;
        }
        setAlpha(1f);
    }
}
