package com.project.ads.helpers;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.project.ads.R;

/**
 * Animación al cambiar de pestaña en la barra inferior (cliente y trabajador).
 * Cada pestaña es una Activity que se trae al frente; la nueva entra desde el lado
 * hacia el que está en la barra, con un fundido corto, y la anterior se queda quieta.
 * Al llegar, el fondo naranja de la pestaña de destino crece (animación de Material).
 * Si el teléfono tiene las animaciones desactivadas, Android las omite.
 */
public final class TabTransition {

    private TabTransition() {
    }

    /**
     * Abre la pestaña; forward = la de destino está más a la derecha en la barra;
     * fromItemId = pestaña actual, para animar el indicador al llegar.
     */
    public static void start(Activity activity, Intent intent, boolean forward, int fromItemId) {
        animateIndicatorOnArrival(activity, fromItemId);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
        activity.overridePendingTransition(
                forward ? R.anim.ads_tab_enter_right : R.anim.ads_tab_enter_left, R.anim.ads_tab_exit);
    }

    /**
     * En la primera pantalla que se reanude (la pestaña de destino), marca un instante
     * la pestaña de origen y luego la propia: Material anima el fondo naranja creciendo.
     * MenuItem.setChecked no avisa al listener, así que no navega.
     */
    private static void animateIndicatorOnArrival(Activity from, int fromItemId) {
        Application app = from.getApplication();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                app.unregisterActivityLifecycleCallbacks(this);
                if (activity == from || !Motion.enabled(activity)) return;
                BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
                if (nav == null) return;
                MenuItem current = nav.getMenu().findItem(nav.getSelectedItemId());
                MenuItem previous = nav.getMenu().findItem(fromItemId);
                if (current == null || previous == null || current == previous) return;
                previous.setChecked(true);
                nav.post(() -> current.setChecked(true));
            }

            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        });
    }
}
