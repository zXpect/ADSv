package com.project.ads.helpers;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

import com.project.ads.R;

/**
 * Estado del pago en el detalle del servicio (cliente y trabajador): «Pagado» va
 * como chip con ✓, igual que el chip «Completada»; los demás estados siguen como
 * texto de color.
 */
public final class PaymentStatusChip {

    private PaymentStatusChip() {}

    public static void showPaid(TextView view, @StringRes int text) {
        int horizontal = dp(view, 12);
        int vertical = dp(view, 4);
        view.setText(text);
        view.setBackgroundResource(R.drawable.bg_ads_status_chip);
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(
                ContextCompat.getColor(view.getContext(), R.color.ads_chip_strong_bg)));
        int textColor = ContextCompat.getColor(view.getContext(), R.color.ads_chip_strong_text);
        view.setTextColor(textColor);
        Drawable check = AppCompatResources.getDrawable(view.getContext(), R.drawable.ic_ads_check);
        if (check != null) {
            check = DrawableCompat.wrap(check.mutate());
            DrawableCompat.setTint(check, textColor);
            int size = dp(view, 16);
            check.setBounds(0, 0, size, size);
        }
        view.setCompoundDrawablesRelative(check, null, null, null);
        view.setCompoundDrawablePadding(dp(view, 6));
        view.setPaddingRelative(horizontal - dp(view, 2), vertical, horizontal, vertical);
    }

    public static void showText(TextView view, @StringRes int text, @ColorRes int color) {
        view.setText(text);
        view.setBackground(null);
        view.setTextColor(ContextCompat.getColor(view.getContext(), color));
        view.setCompoundDrawablesRelative(null, null, null, null);
        view.setPaddingRelative(0, 0, 0, 0);
    }

    private static int dp(TextView view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}
