package com.project.ads.helpers;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.TaskStackBuilder;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.ads.providers.NotificationHistoryProvider;
import com.project.ads.activities.MainActivity;
import com.project.ads.activities.NotificationsActivity;
import com.project.ads.activities.client.HomeUserActivity;
import com.project.ads.activities.client.ServiceRequestDetailActivity;
import com.project.ads.activities.worker.HomeWorkerActivity;
import com.project.ads.activities.worker.RequestDetailActivity;
import com.project.ads.activities.worker.ServiceDetailWorkerActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Decide a qué pantalla lleva una notificación, a partir de (tipo, solicitud, rol).
 * Lo usan el push (MyFirebaseMessagingService) y la pantalla de Notificaciones,
 * para que los dos abran lo mismo. Tipos: los que envía NotificationService del backend.
 */
public final class NotificationRouter {

    public static final String EXTRA_REQUEST_ID = "request_id";
    public static final String EXTRA_TYPE = "notification_type";
    public static final String EXTRA_PQR_ID = "pqr_id";
    // Clave de la entrada en notifications/<uid> (la envía el backend en el push)
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    private static final String ROLE_PREFS = "typeUser";
    private static final String ROLE_KEY = "user";
    private static final String ROLE_WORKER = "trabajador";
    private static final String ROLE_CLIENT = "cliente";

    // Destino pendiente cuando el push se toca sin sesión: lo abre el inicio tras el login
    private static final String PENDING_PREFS = "pending_notification";

    private static final Set<String> WORKER_SERVICE_TYPES = new HashSet<>(Arrays.asList(
            "quote_accepted", "quote_rejected", "request_cancelled", "payment_approved",
            "requote_accepted", "requote_rejected", "pqr_answered"));

    private static final Set<String> CLIENT_SERVICE_TYPES = new HashSet<>(Arrays.asList(
            "request_accepted", "request_rejected", "quote_received", "request_rescheduled",
            "status_change", "request_cancelled", "cash_received", "requote_received",
            // Mientras no exista el detalle de la PQR se abre la solicitud
            "pqr_answered"));

    private NotificationRouter() {
    }

    public static String role(Context context) {
        return context.getSharedPreferences(ROLE_PREFS, Context.MODE_PRIVATE).getString(ROLE_KEY, "");
    }

    private static boolean hasSession(Context context) {
        String role = role(context);
        return FirebaseAuth.getInstance().getCurrentUser() != null
                && (ROLE_WORKER.equals(role) || ROLE_CLIENT.equals(role));
    }

    /** Pantalla de destino del tipo para el rol; sin solicitud o tipo desconocido, Notificaciones. */
    public static Class<?> destination(String role, String type, String requestId) {
        if (requestId == null || requestId.isEmpty() || type == null) {
            return NotificationsActivity.class;
        }
        if (ROLE_WORKER.equals(role)) {
            if ("new_request".equals(type)) return RequestDetailActivity.class;
            if (WORKER_SERVICE_TYPES.contains(type)) return ServiceDetailWorkerActivity.class;
        } else if (ROLE_CLIENT.equals(role)) {
            if (CLIENT_SERVICE_TYPES.contains(type)) return ServiceRequestDetailActivity.class;
        }
        return NotificationsActivity.class;
    }

    public static Intent destinationIntent(Context context, String type, String requestId, String pqrId) {
        return destinationIntent(context, type, requestId, pqrId, null);
    }

    public static Intent destinationIntent(Context context, String type, String requestId, String pqrId,
                                           String notificationId) {
        Intent intent = new Intent(context, destination(role(context), type, requestId));
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        intent.putExtra(EXTRA_TYPE, type);
        if (pqrId != null) intent.putExtra(EXTRA_PQR_ID, pqrId);
        if (notificationId != null) intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        return intent;
    }

    /**
     * Para las pantallas de destino (onCreate): marca como leída la notificación
     * del push con que se abrió y las demás sin leer de la misma solicitud, así
     * queda bien entre por el push, por la lista o por el historial.
     */
    public static void markOpened(Activity activity, String requestId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        NotificationHistoryProvider provider = new NotificationHistoryProvider();
        String notificationId = activity.getIntent() != null
                ? activity.getIntent().getStringExtra(EXTRA_NOTIFICATION_ID) : null;
        if (notificationId != null && !notificationId.isEmpty()) {
            provider.markAsRead(user.getUid(), Collections.singletonList(notificationId));
        }
        if (requestId != null && !requestId.isEmpty()) {
            provider.markRequestAsRead(user.getUid(), requestId);
        }
    }

    private static Class<?> homeOf(String role) {
        return ROLE_WORKER.equals(role) ? HomeWorkerActivity.class : HomeUserActivity.class;
    }

    /** Pila [inicio del rol, destino]: «atrás» desde el destino vuelve al inicio y no cierra la app. */
    public static TaskStackBuilder stack(Context context, String type, String requestId, String pqrId,
                                         String notificationId) {
        return TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, homeOf(role(context))))
                .addNextIntent(destinationIntent(context, type, requestId, pqrId, notificationId));
    }

    /**
     * PendingIntent del push. Con sesión abre el destino sobre el inicio del rol;
     * sin sesión, la bienvenida con los datos, que los guarda para después del login.
     * El requestCode depende de la solicitud y del tipo, para que Android no
     * reutilice los extras de otro push de la misma solicitud.
     */
    public static PendingIntent pendingIntent(Context context, String type, String requestId, String pqrId,
                                              String notificationId) {
        int requestCode = (String.valueOf(requestId) + "|" + type).hashCode();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        if (hasSession(context)) {
            return stack(context, type, requestId, pqrId, notificationId).getPendingIntent(requestCode, flags);
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        intent.putExtra(EXTRA_TYPE, type);
        if (pqrId != null) intent.putExtra(EXTRA_PQR_ID, pqrId);
        if (notificationId != null) intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    /**
     * Para MainActivity: si el intent viene de un push, abre el destino (con sesión)
     * o lo guarda para después del login. Devuelve true si ya navegó.
     */
    public static boolean handleLaunchIntent(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_TYPE)) return false;
        String type = intent.getStringExtra(EXTRA_TYPE);
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        String pqrId = intent.getStringExtra(EXTRA_PQR_ID);
        String notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID);
        // Se consumen para que un onStart posterior no vuelva a abrir el destino
        intent.removeExtra(EXTRA_TYPE);
        intent.removeExtra(EXTRA_REQUEST_ID);
        intent.removeExtra(EXTRA_PQR_ID);
        intent.removeExtra(EXTRA_NOTIFICATION_ID);

        if (hasSession(activity)) {
            stack(activity, type, requestId, pqrId, notificationId).startActivities();
            activity.finish();
            return true;
        }
        activity.getSharedPreferences(PENDING_PREFS, Context.MODE_PRIVATE).edit()
                .putString("type", type)
                .putString("request_id", requestId)
                .putString("pqr_id", pqrId)
                .putString("notification_id", notificationId)
                .apply();
        return false;
    }

    /** Para el inicio de cada rol (onCreate): abre el destino guardado antes del login, si hay. */
    public static void openPendingIfAny(Activity home) {
        SharedPreferences prefs = home.getSharedPreferences(PENDING_PREFS, Context.MODE_PRIVATE);
        String type = prefs.getString("type", null);
        if (type == null) return;
        String requestId = prefs.getString("request_id", null);
        String pqrId = prefs.getString("pqr_id", null);
        String notificationId = prefs.getString("notification_id", null);
        prefs.edit().clear().apply();
        // Si entró con el otro rol, el push no era para esta sesión: se descarta
        Class<?> target = destination(role(home), type, requestId);
        if (target == NotificationsActivity.class && requestId != null) return;
        home.startActivity(destinationIntent(home, type, requestId, pqrId, notificationId));
    }
}
