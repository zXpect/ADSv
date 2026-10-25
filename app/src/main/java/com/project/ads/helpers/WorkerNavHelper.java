package com.project.ads.helpers;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.ads.R;
import com.project.ads.activities.worker.ActiveServicesActivity;
import com.project.ads.activities.worker.HomeWorkerActivity;
import com.project.ads.activities.worker.PendingRequestsActivity;
import com.project.ads.activities.worker.ProfileWorkerActivity;

/**
 * Barra inferior del trabajador (mockups 13, 14, 51, 52):
 * Inicio / Solicitudes / Mis trabajos / Mi cuenta. Cada pestaña es una Activity;
 * el inicio no se cierra al cambiar, para que el trabajador siga disponible.
 */
public final class WorkerNavHelper {

    private WorkerNavHelper() {
    }

    public static void setup(Activity activity, int selectedItemId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        if (nav == null) return;
        nav.setSelectedItemId(selectedItemId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;
            Class<? extends Activity> target = targetFor(id);
            if (target == null) return false;
            Intent intent = new Intent(activity, target);
            // Las listas del trabajador leen su id del intent
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) intent.putExtra("worker_id", user.getUid());
            TabTransition.start(activity, intent, ClientNavHelper.isAfter(nav, id, selectedItemId), selectedItemId);
            return false;   // la pestaña se marca en la pantalla de destino
        });
        nav.setOnItemReselectedListener(item -> { });
    }

    private static Class<? extends Activity> targetFor(int itemId) {
        if (itemId == R.id.nav_worker_home) return HomeWorkerActivity.class;
        if (itemId == R.id.nav_worker_requests) return PendingRequestsActivity.class;
        if (itemId == R.id.nav_worker_jobs) return ActiveServicesActivity.class;
        if (itemId == R.id.nav_worker_account) return ProfileWorkerActivity.class;
        return null;
    }
}
