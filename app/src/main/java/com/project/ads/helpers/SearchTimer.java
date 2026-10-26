package com.project.ads.helpers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Marcas de tiempo para medir el tiempo de búsqueda (D22, HU-28): empieza al
 * abrir la búsqueda o el mapa desde el inicio del cliente y termina al tocar
 * «Solicitar» en el perfil del técnico. Se envían al crear la solicitud
 * (search_started_at y worker_selected_at, en ms) y se borran al crearla.
 */
public final class SearchTimer {

    private static final String PREFS = "search_timer";
    private static final String KEY_STARTED = "search_started_at";
    private static final String KEY_SELECTED = "worker_selected_at";

    // Una búsqueda que lleva más de 12 h abierta no es una medición válida
    static final long MAX_SEARCH_MS = 12 * 60 * 60 * 1000L;

    private SearchTimer() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** El cliente abre la búsqueda, un oficio o el mapa desde el inicio: empieza una medición nueva. */
    public static void start(Context context) {
        prefs(context).edit()
                .putLong(KEY_STARTED, System.currentTimeMillis())
                .remove(KEY_SELECTED)
                .apply();
    }

    /** El cliente toca «Solicitar» en el perfil del técnico. */
    public static void markSelected(Context context) {
        prefs(context).edit().putLong(KEY_SELECTED, System.currentTimeMillis()).apply();
    }

    /** {inicio, selección} para la solicitud, o null si la medición no es válida. */
    public static long[] values(Context context) {
        SharedPreferences prefs = prefs(context);
        return validPair(prefs.getLong(KEY_STARTED, 0), prefs.getLong(KEY_SELECTED, 0));
    }

    /** La solicitud ya se creó: la próxima búsqueda empieza de cero. */
    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    /** Válida si las dos marcas existen, van en orden y no superan el máximo. */
    public static long[] validPair(long started, long selected) {
        if (started <= 0 || selected <= 0 || selected < started) return null;
        if (selected - started > MAX_SEARCH_MS) return null;
        return new long[]{started, selected};
    }
}
