package com.project.ads.helpers;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Reflejo diagonal suave que recorre una tarjeta (servicio en curso): una franja
 * blanca muy tenue cruza de izquierda a derecha en 1,6 s y descansa 1,4 s. Va como
 * foreground de la vista, recortada a su contorno (setClipToOutline).
 */
public class SheenDrawable extends Drawable implements Animatable {

    private static final long SWEEP_MS = 1600;
    private static final long REST_MS = 1400;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mMatrix = new Matrix();
    private final ValueAnimator mAnimator = ValueAnimator.ofFloat(0f, 1f);
    private float mProgress = -1f;   // -1: sin franja visible

    public SheenDrawable() {
        mAnimator.setDuration(SWEEP_MS + REST_MS);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue() * (SWEEP_MS + REST_MS) / SWEEP_MS;
            mProgress = t <= 1f ? t : -1f;
            invalidateSelf();
        });
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        // Franja del 45 % del ancho: transparente → blanco 14 % → transparente
        float width = bounds.width() * 0.45f;
        mPaint.setShader(new LinearGradient(0, 0, width, 0,
                new int[]{0x00FFFFFF, 0x24FFFFFF, 0x00FFFFFF}, new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mProgress < 0f) return;
        Rect b = getBounds();
        float width = b.width() * 0.45f;
        // Recorre desde fuera por la izquierda hasta fuera por la derecha, inclinada 20°
        float x = b.left - width + (b.width() + width) * mProgress;
        mMatrix.setTranslate(x, 0);
        mMatrix.postSkew(-0.35f, 0f, x, b.centerY());
        mPaint.getShader().setLocalMatrix(mMatrix);
        canvas.drawRect(b, mPaint);
    }

    @Override
    public void start() {
        if (!mAnimator.isStarted()) mAnimator.start();
    }

    @Override
    public void stop() {
        mAnimator.cancel();
        mProgress = -1f;
        invalidateSelf();
    }

    @Override
    public boolean isRunning() {
        return mAnimator.isRunning();
    }

    @Override public void setAlpha(int alpha) { mPaint.setAlpha(alpha); }
    @Override public void setColorFilter(@Nullable ColorFilter cf) { mPaint.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
