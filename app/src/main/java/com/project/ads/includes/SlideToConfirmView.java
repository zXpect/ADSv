package com.project.ads.includes;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;

import com.project.ads.R;
import com.project.ads.helpers.Haptics;

/**
 * Barra de «deslizar para confirmar»: se arrastra la ficha hasta el final y entonces se
 * avisa una sola vez. Evita confirmar por un toque accidental. Con TalkBack basta activarla
 * (performClick). Si la acción falla, reset() la devuelve al inicio.
 */
public class SlideToConfirmView extends FrameLayout {

    public interface OnConfirmListener {
        void onConfirm();
    }

    private static final float CONFIRM_AT = 0.85f;

    private final View mFill;
    private final TextView mLabel;
    private final ImageView mThumb;
    private final int mThumbSize;
    private final int mInset;
    private OnConfirmListener mListener;
    private boolean mDragging;
    private boolean mDone;
    private float mGrabOffset;

    public SlideToConfirmView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        mThumbSize = Math.round(56 * density);
        mInset = Math.round(4 * density);
        setClickable(true);
        setFocusable(true);
        setMinimumHeight(mThumbSize + 2 * mInset);
        setBackground(pill(ContextCompat.getColor(context, R.color.ads_chip_bg)));

        mFill = new View(context);
        mFill.setBackground(pill(ContextCompat.getColor(context, R.color.ads_brand)));
        addView(mFill, new LayoutParams(mThumbSize + 2 * mInset, LayoutParams.MATCH_PARENT));

        mLabel = new TextView(context);
        mLabel.setGravity(Gravity.CENTER);
        mLabel.setTextColor(ContextCompat.getColor(context, R.color.ads_chip_text));
        mLabel.setTextSize(15);
        mLabel.setTypeface(ResourcesCompat.getFont(context, R.font.sora_semibold));
        mLabel.setPadding(mThumbSize + 2 * mInset, 0, mThumbSize / 2, 0);
        mLabel.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(mLabel, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mThumb = new ImageView(context);
        mThumb.setImageResource(R.drawable.ic_arrow_forward);
        ImageViewCompat.setImageTintList(mThumb,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ads_on_brand)));
        mThumb.setBackground(pill(ContextCompat.getColor(context, R.color.ads_brand)));
        int pad = Math.round(16 * density);
        mThumb.setPadding(pad, pad, pad, pad);
        mThumb.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        LayoutParams thumb = new LayoutParams(mThumbSize, mThumbSize, Gravity.CENTER_VERTICAL);
        thumb.leftMargin = mInset;
        addView(mThumb, thumb);
    }

    private GradientDrawable pill(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(999f);
        return shape;
    }

    public void setText(CharSequence text) {
        mLabel.setText(text);
        setContentDescription(text);
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        mListener = listener;
    }

    /** Devuelve la ficha al inicio y permite volver a deslizar. */
    public void reset() {
        mDone = false;
        animateTo(0f, true);
    }

    private float maxTravel() {
        return Math.max(1f, getWidth() - mThumbSize - 2f * mInset);
    }

    private void moveTo(float x) {
        float clamped = Math.max(0f, Math.min(x, maxTravel()));
        mThumb.setTranslationX(clamped);
        mLabel.setAlpha(1f - clamped / maxTravel());
        LayoutParams params = (LayoutParams) mFill.getLayoutParams();
        params.width = Math.round(clamped) + mThumbSize + 2 * mInset;
        mFill.setLayoutParams(params);
    }

    private void animateTo(float x, boolean bounce) {
        ValueAnimator animator = ValueAnimator.ofFloat(mThumb.getTranslationX(), x);
        animator.setDuration(bounce ? 320 : 160);
        animator.setInterpolator(bounce ? new OvershootInterpolator(1.2f) : new DecelerateInterpolator());
        animator.addUpdateListener(a -> moveTo((float) a.getAnimatedValue()));
        animator.start();
    }

    private void confirm() {
        if (mDone) return;
        mDone = true;
        Haptics.confirm(this);
        animateTo(maxTravel(), false);
        if (mListener != null) mListener.onConfirm();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDone || !isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float thumbEnd = mThumb.getTranslationX() + mThumbSize + 2 * mInset;
                mDragging = event.getX() <= thumbEnd;
                mGrabOffset = event.getX() - mThumb.getTranslationX();
                if (mDragging) getParent().requestDisallowInterceptTouchEvent(true);
                return mDragging;
            case MotionEvent.ACTION_MOVE:
                if (mDragging) moveTo(event.getX() - mGrabOffset);
                return mDragging;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mDragging) return false;
                mDragging = false;
                if (mThumb.getTranslationX() / maxTravel() >= CONFIRM_AT) {
                    confirm();
                } else {
                    animateTo(0f, true);
                }
                return true;
            default:
                return false;
        }
    }

    /** Accesibilidad: con lector de pantalla no hay arrastre, activar confirma. */
    @Override
    public boolean performClick() {
        super.performClick();
        confirm();
        return true;
    }
}
