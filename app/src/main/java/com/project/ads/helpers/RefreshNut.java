package com.project.ads.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.project.ads.R;

import java.lang.reflect.Field;

/**
 * «Deslizar para actualizar» con la tuerca de ADS en lugar del círculo de Android.
 * SwipeRefreshLayout no deja cambiar su indicador: se le pone NutSpinnerDrawable a su
 * vista interna (mCircleView) por reflexión, con la regla -keep de proguard-rules.pro
 * para el release. Si la librería cambia y no está el campo, queda el círculo naranja.
 */
public final class RefreshNut {

    private static final String TAG = "RefreshNut";

    private RefreshNut() {
    }

    public static void install(SwipeRefreshLayout swipe) {
        Context context = swipe.getContext();
        swipe.setColorSchemeResources(R.color.ads_brand);
        swipe.setProgressBackgroundColorSchemeResource(R.color.ads_card_raised);
        try {
            Field field = SwipeRefreshLayout.class.getDeclaredField("mCircleView");
            field.setAccessible(true);
            ImageView circle = (ImageView) field.get(swipe);
            float stroke = 2.4f * context.getResources().getDisplayMetrics().density;
            circle.setImageDrawable(new NutSpinnerDrawable(ContextCompat.getColor(context, R.color.ads_brand), stroke));
        } catch (Exception e) {
            Log.w(TAG, "Indicador de actualizar sin tuerca: " + e.getClass().getSimpleName());
        }
    }
}
