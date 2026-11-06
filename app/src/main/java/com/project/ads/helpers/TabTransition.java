package com.project.ads.helpers;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.project.ads.R;

/**
 * Animación al cambiar de pestaña en la barra inferior (cliente y trabajador).
 * Cada pestaña es una Activity que se trae al frente; la nueva aparece con un fundido
 * sobre la anterior, que se queda quieta. Sin desplazamiento, para que la barra inferior
 * no se mueva. Al llegar, la píldora crece y el ícono da un saltito (Motion.tabSelected).
 * Si el teléfono tiene las animaciones desactivadas, Android las omite.
 */
public final class TabTransition {

    private TabTransition() {
    }

    /** Abre la pestaña con el fundido y anima la selección al llegar. */
    public static void start(Activity activity, Intent intent) {
        animateSelectionOnArrival(activity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.ads_tab_enter, R.anim.ads_tab_exit);
    }

    /** Anima la pestaña marcada en la primera pantalla que se reanude (la de destino). */
    private static void animateSelectionOnArrival(Activity from) {
        Application app = from.getApplication();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                app.unregisterActivityLifecycleCallbacks(this);
                if (activity == from || !Motion.enabled(activity)) return;
                BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
                if (nav == null) return;
                Motion.tabSelected(nav);
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
