package com.ads.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.Manifest;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationPermissionHelper {

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
     * Registra un launcher para solicitar permisos de notificación
     * Sin callbacks ni diálogos adicionales
     */
    public static ActivityResultLauncher<String> registerForPermissionResult(AppCompatActivity activity) {
        return activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // No hacemos nada aquí para evitar diálogos adicionales
                    // El sistema se encarga de mostrar el diálogo estándar de permisos
                });
    }

    /**
     * Solicita permisos de notificación si es necesario
     * Sin diálogos adicionales
     */
    public static void requestNotificationPermissionIfNeeded(Context context,
                                                             ActivityResultLauncher<String> launcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Solicitar directamente sin diálogos personalizados
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Solicita permiso de notificación con un callback para manejar el resultado
     * Esta es la función que faltaba en la implementación original
     */
    public static void requestNotificationPermissionWithCallback(
            AppCompatActivity activity,
            ActivityResultLauncher<String> launcher,
            PermissionCallback callback) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Si ya tenemos el permiso, llamar directamente al callback con true
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                callback.onPermissionResult(true);
                return;
            }

            // Como no podemos modificar el launcher existente, creamos uno temporal para este caso específico
            ActivityResultLauncher<String> tempLauncher = activity.registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> callback.onPermissionResult(isGranted)
            );

            // Solicitamos el permiso con el launcher temporal
            tempLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // Para versiones anteriores a Android 13, no se necesita el permiso explícito
            callback.onPermissionResult(true);
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