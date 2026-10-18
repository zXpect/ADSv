package com.project.ads.helpers;

import android.app.Activity;
import android.widget.TextView;

import com.project.ads.R;

/**
 * Encabezado de los 3 pasos del registro del trabajador (view_register_steps):
 * pinta las barras hasta el paso actual y escribe «Paso N de 3 · …».
 */
public final class RegisterStepsHelper {

    private RegisterStepsHelper() {
    }

    public static void bind(Activity activity, int step, int labelRes) {
        int[] bars = {R.id.step_bar_1, R.id.step_bar_2, R.id.step_bar_3};
        for (int i = 0; i < bars.length; i++) {
            activity.findViewById(bars[i]).setBackgroundResource(
                    i < step ? R.drawable.bg_ads_progress_on : R.drawable.bg_ads_progress_off);
        }
        activity.findViewById(R.id.layout_register_steps).setContentDescription(
                activity.getString(R.string.register_step_desc, step));
        ((TextView) activity.findViewById(R.id.tv_register_step)).setText(
                activity.getString(R.string.register_step_label, step, activity.getString(labelRes)));
    }
}
