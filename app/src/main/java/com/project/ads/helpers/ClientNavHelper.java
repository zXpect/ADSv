package com.project.ads.helpers;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.ads.R;
import com.project.ads.activities.PqrListActivity;
import com.project.ads.activities.client.HomeUserActivity;
import com.project.ads.activities.client.MyRequestsActivity;
import com.project.ads.activities.client.ProfileClientActivity;

/**
 * Barra inferior del cliente (mockups 06, 37, 41, 42): Inicio / Solicitudes / PQR / Mi cuenta.
 * Cada pestaña es una Activity; al cambiar se trae al frente con TabTransition.
 */
public final class ClientNavHelper {

    private ClientNavHelper() {
    }

    /** Marca la pestaña de la pantalla actual y conecta la navegación. */
    public static void setup(Activity activity, int selectedItemId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        if (nav == null) return;
        nav.setSelectedItemId(selectedItemId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;
            Class<? extends Activity> target = targetFor(id);
            if (target == null) {
                Toast.makeText(activity, R.string.nav_coming_soon, Toast.LENGTH_SHORT).show();
                return false;
            }
            TabTransition.start(activity, new Intent(activity, target),
                    isAfter(nav, id, selectedItemId), selectedItemId);
            return false;   // la pestaña se marca en la pantalla de destino
        });
        nav.setOnItemReselectedListener(item -> { });
    }

    /** true si la pestaña de destino está a la derecha de la actual en la barra. */
    static boolean isAfter(BottomNavigationView nav, int targetId, int currentId) {
        int target = -1, current = -1;
        for (int i = 0; i < nav.getMenu().size(); i++) {
            int id = nav.getMenu().getItem(i).getItemId();
            if (id == targetId) target = i;
            if (id == currentId) current = i;
        }
        return target > current;
    }

    private static Class<? extends Activity> targetFor(int itemId) {
        if (itemId == R.id.nav_client_home) return HomeUserActivity.class;
        if (itemId == R.id.nav_client_requests) return MyRequestsActivity.class;
        if (itemId == R.id.nav_client_account) return ProfileClientActivity.class;
        if (itemId == R.id.nav_client_pqr) return PqrListActivity.class;
        return null;
    }
}
