package com.project.ads.helpers;

import android.app.Activity;
import android.os.Build;
import android.view.Window;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Barra de navegación del sistema del mismo color que el fondo de la pantalla
 * (Bienvenida, Elegir rol, Login), para que no quede una franja gris abajo.
 */
public final class SystemBarsHelper {

    private SystemBarsHelper() {}

    /**
     * @param lightBackground true si el fondo es claro (íconos oscuros en la barra).
     */
    public static void matchNavigationBar(Activity activity, @ColorRes int color, boolean lightBackground) {
        // Los íconos oscuros de la barra de navegación solo existen desde Android 8;
        // antes se deja la barra del sistema.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        Window window = activity.getWindow();
        window.setNavigationBarColor(ContextCompat.getColor(activity, color));
        new WindowInsetsControllerCompat(window, window.getDecorView())
                .setAppearanceLightNavigationBars(lightBackground);
    }
}
