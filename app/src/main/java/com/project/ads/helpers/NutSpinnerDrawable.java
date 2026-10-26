package com.project.ads.helpers;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Tuerca naranja que gira (una vuelta cada 1,4 s) mientras se dibuja. Es el indicador
 * de «deslizar para actualizar» (RefreshNut), igual a la tuerca de carga de la app.
 * Gira sola: cada cuadro pide el siguiente mientras la vista esté visible.
 */
public class NutSpinnerDrawable extends Drawable {

    private static final long TURN_MS = 1400;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mNut = new Path();

    public NutSpinnerDrawable(int color, float strokePx) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(strokePx);
        mPaint.setColor(color);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        // Hexágono y agujero, como ic_ads_nut, al 32 % del lado del círculo
        float cx = bounds.exactCenterX(), cy = bounds.exactCenterY();
        float r = Math.min(bounds.width(), bounds.height()) * 0.32f;
        mNut.reset();
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(60 * k);
            float x = cx + (float) (r * Math.cos(a)), y = cy + (float) (r * Math.sin(a));
            if (k == 0) mNut.moveTo(x, y); else mNut.lineTo(x, y);
        }
        mNut.close();
        mNut.addCircle(cx, cy, r * 0.38f, Path.Direction.CW);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        float angle = (SystemClock.uptimeMillis() % TURN_MS) * 360f / TURN_MS;
        canvas.save();
        canvas.rotate(angle, b.exactCenterX(), b.exactCenterY());
        canvas.drawPath(mNut, mPaint);
        canvas.restore();
        invalidateSelf();
    }

    @Override public void setAlpha(int alpha) { mPaint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(@Nullable ColorFilter cf) { mPaint.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
