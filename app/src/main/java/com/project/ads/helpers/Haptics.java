package com.project.ads.helpers;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.RequiresApi;

/**
 * Vibración breve en los gestos clave (conectarse, aceptar una cotización,
 * calificar). Usa la retroalimentación háptica del sistema, así que respeta el
 * ajuste del teléfono y no necesita el permiso VIBRATE.
 *
 * Los teléfonos con vibrador básico (p. ej. Galaxy A17) no tienen efectos
 * predefinidos e ignoran CONFIRM o KEYBOARD_PRESS; ahí se usa LONG_PRESS, que
 * sí reproducen.
 */
public final class Haptics {

    private Haptics() {}

    /** Confirmación de una acción importante. */
    public static void confirm(View view) {
        view.performHapticFeedback(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasEffects(view)
                ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS);
    }

    /** Toque leve al elegir un valor (estrellas). */
    public static void tick(View view) {
        view.performHapticFeedback(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasEffects(view)
                ? HapticFeedbackConstants.KEYBOARD_PRESS : HapticFeedbackConstants.LONG_PRESS);
    }

    /** true si el vibrador reproduce los efectos predefinidos. */
    @RequiresApi(Build.VERSION_CODES.R)
    private static boolean hasEffects(View view) {
        Vibrator vibrator = view.getContext().getSystemService(Vibrator.class);
        return vibrator != null && vibrator.areAllEffectsSupported(VibrationEffect.EFFECT_CLICK)
                == Vibrator.VIBRATION_EFFECT_SUPPORT_YES;
    }
}
