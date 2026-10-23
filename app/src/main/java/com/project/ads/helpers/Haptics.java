package com.project.ads.helpers;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Vibración breve en los gestos clave (conectarse, aceptar una cotización,
 * calificar). Usa la retroalimentación háptica del sistema, así que respeta el
 * ajuste del teléfono y no necesita el permiso VIBRATE.
 */
public final class Haptics {

    private Haptics() {}

    /** Confirmación de una acción importante. */
    public static void confirm(View view) {
        int effect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY;
        view.performHapticFeedback(effect);
    }

    /** Toque leve al elegir un valor (estrellas). */
    public static void tick(View view) {
        int effect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                ? HapticFeedbackConstants.KEYBOARD_PRESS : HapticFeedbackConstants.VIRTUAL_KEY;
        view.performHapticFeedback(effect);
    }
}
