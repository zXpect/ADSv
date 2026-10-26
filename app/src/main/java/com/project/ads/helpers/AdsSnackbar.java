package com.project.ads.helpers;

import android.app.Activity;
import android.view.View;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;
import com.project.ads.R;

/**
 * Avisos dentro de la pantalla con el estilo ADS (Widget.ADS.Snackbar), en lugar
 * de Toast. Se apoyan sobre la barra inferior o la barra de acción fija para no
 * tapar sus botones.
 *
 * No usar cuando la pantalla se cierra justo después (el aviso se perdería) ni
 * con un diálogo abierto (quedaría detrás): ahí se sigue usando Toast.
 */
public final class AdsSnackbar {

    /** Barras sobre las que se apoya el aviso, en orden de preferencia. */
    private static final int[] ANCHORS = {R.id.bottom_nav, R.id.ads_action_bar, R.id.card_actions};

    private AdsSnackbar() {}

    public static void show(Activity activity, CharSequence message) {
        make(activity, message, Snackbar.LENGTH_SHORT).show();
    }

    public static void show(Activity activity, @StringRes int message) {
        show(activity, activity.getText(message));
    }

    /** Para errores o mensajes largos. */
    public static void showLong(Activity activity, CharSequence message) {
        make(activity, message, Snackbar.LENGTH_LONG).show();
    }

    public static void showLong(Activity activity, @StringRes int message) {
        showLong(activity, activity.getText(message));
    }

    private static Snackbar make(Activity activity, CharSequence message, int duration) {
        View root = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(root, message, duration);
        for (int id : ANCHORS) {
            View anchor = activity.findViewById(id);
            if (anchor != null && anchor.isShown()) {
                snackbar.setAnchorView(anchor);
                break;
            }
        }
        return snackbar;
    }
}
