package com.project.ads.helpers;

import android.app.Activity;
import android.content.Intent;

import com.project.ads.R;

/**
 * Animación al cambiar de pestaña en la barra inferior (cliente y trabajador).
 * Cada pestaña es una Activity que se trae al frente; la nueva entra desde el lado
 * hacia el que está en la barra, con un fundido corto, y la anterior se queda quieta.
 * Si el teléfono tiene las animaciones desactivadas, Android las omite.
 */
public final class TabTransition {

    private TabTransition() {
    }

    /** Abre la pestaña; forward = la de destino está más a la derecha en la barra. */
    public static void start(Activity activity, Intent intent, boolean forward) {
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
        activity.overridePendingTransition(
                forward ? R.anim.ads_tab_enter_right : R.anim.ads_tab_enter_left, R.anim.ads_tab_exit);
    }
}
