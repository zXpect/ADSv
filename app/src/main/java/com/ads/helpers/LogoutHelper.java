package com.ads.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.ads.activities.MainActivity;
import com.ads.providers.AuthProvider;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class LogoutHelper {
    public static void performLogout(Activity activity, AuthProvider authProvider,
                                     FirebaseCrashlytics crashlytics,
                                     FirebaseAnalytics firebaseAnalytics) {
        try {
            // Registrar evento de logout si es posible
            if (authProvider != null && authProvider.getId() != null && firebaseAnalytics != null) {
                try {
                    Bundle params = new Bundle();
                    params.putString("user_id", authProvider.getId());
                    firebaseAnalytics.logEvent("user_logout", params);
                } catch (Exception e) {
                    if (crashlytics != null) crashlytics.recordException(e);
                }
            }

            // Cerrar sesión
            if (authProvider != null) {
                try {
                    authProvider.logOut();
                } catch (Exception e) {
                    if (crashlytics != null) crashlytics.recordException(e);
                    Toast.makeText(activity, "Error al cerrar sesión: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            // Limpiar datos de Crashlytics si está disponible
            if (crashlytics != null) {
                try {
                    crashlytics.setUserId("");
                    crashlytics.setCustomKey("user_type", "none");
                } catch (Exception e) {
                    crashlytics.recordException(e);
                }
            }

            // Navegar a MainActivity
            try {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent);
                activity.finish();
            } catch (Exception e) {
                if (crashlytics != null) crashlytics.recordException(e);
                Toast.makeText(activity, "Error al iniciar MainActivity: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            if (crashlytics != null) crashlytics.recordException(e);
            Toast.makeText(activity, "Error general al cerrar sesión", Toast.LENGTH_SHORT).show();
        }
    }
}