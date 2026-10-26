package com.project.ads.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.Manifest;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.project.ads.R;

public class NotificationPermissionHelper {

    private static final String PREFS = "notification_permission";
    private static final String KEY_ASKED = "asked";

    /**
     * Pide el permiso de notificaciones una sola vez, al entrar al inicio del
     * cliente o del trabajador. Desde Android 13 (targetSdk 33) el sistema no lo
     * muestra solo. Si el usuario lo niega no se vuelve a insistir: queda el
     * acceso «Activar notificaciones» en Mi cuenta. Llamar desde onCreate.
     */
    public static void askOnce(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ASKED, false)) return;

        ActivityResultLauncher<String> launcher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (!granted) {
                        Toast.makeText(activity, R.string.notifications_permission_denied, Toast.LENGTH_LONG).show();
                    }
                });
        prefs.edit().putBoolean(KEY_ASKED, true).apply();
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    /**
     * Muestra la fila «Activar notificaciones» de Mi cuenta solo si están
     * apagadas, y la conecta con los ajustes del sistema. Llamar en onResume:
     * el usuario puede volver de los ajustes con el permiso ya activado.
     */
    public static void bindEnableRow(AppCompatActivity activity, android.view.View row) {
        boolean enabled = areNotificationsEnabled(activity);
        row.setVisibility(enabled ? android.view.View.GONE : android.view.View.VISIBLE);
        row.setOnClickListener(v -> openNotificationSettings(activity));
    }

    /**
     * Interfaz para manejar el callback del resultado del permiso
     */
    public interface PermissionCallback {
        void onPermissionResult(boolean granted);
    }

    /**
     * Verifica si las notificaciones están habilitadas para la aplicación
     */
    public static boolean areNotificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

            // También verificamos si las notificaciones están habilitadas a nivel del sistema
            boolean notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();

            return hasPermission && notificationsEnabled;
        } else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }

    /**
     * Solicita permisos de notificación si es necesario
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static void requestNotificationPermissionIfNeeded(Context context,
                                                             ActivityResultLauncher<String> launcher) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /**
     * Abre la configuración de notificaciones de la aplicación
     * Esta función se mantiene por si se necesita en otro lugar de la aplicación
     */
    public static void openNotificationSettings(Context context) {
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", context.getPackageName());
                intent.putExtra("app_uid", context.getApplicationInfo().uid);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            // Si hay algún error, abrir la configuración general de la aplicación
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
        }
    }
}