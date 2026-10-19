package com.project.ads.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.project.ads.channel.NotificationHelper;
import com.project.ads.helpers.NotificationRouter;
import com.project.ads.providers.TokenProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessaging";

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        // FCM rota el token (reinstalación, borrado de datos...). Si no se guarda,
        // el backend sigue enviando al token viejo y las notificaciones se pierden.
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            new TokenProvider().saveToken(s);
        }
        Log.d(TAG, "Nuevo token FCM recibido");
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Map<String, String> data = remoteMessage.getData();

        String title = data.get("title");
        String body = data.get("body");
        String requestId = data.get("requestId");
        String type = data.get("type");
        String pqrId = data.get("pqrId");

        Log.d(TAG, "Notification data - Title: " + title + ", Body: " + body + ", RequestId: " + requestId + ", Type: " + type);

        if (title != null) {
            // El destino al tocar lo decide NotificationRouter (igual que la pantalla de Notificaciones)
            PendingIntent intent = NotificationRouter.pendingIntent(this, type, requestId, pqrId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showNotificationApiOreo(title, body, requestId, intent);
            } else {
                showNotification(title, body, requestId, intent);
            }
        }
    }

    private void showNotification(String title, String body, String requestId, PendingIntent intent) {
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        NotificationCompat.Builder builder = notificationHelper.getNotificationOldAPI(title, body, intent, sound);
        notificationHelper.getManager().notify(generateNotificationId(requestId), builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotificationApiOreo(String title, String body, String requestId, PendingIntent intent) {
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        Notification.Builder builder = notificationHelper.getNotification(title, body, intent, sound);
        notificationHelper.getManager().notify(generateNotificationId(requestId), builder.build());
    }

    private int generateNotificationId(String requestId) {
        // Un id por solicitud: cada push de la misma solicitud reemplaza al anterior en la barra
        // If requestId is null, use a default ID
        if (requestId != null && !requestId.isEmpty()) {
            return Math.abs(requestId.hashCode());
        }
        return (int) System.currentTimeMillis() % Integer.MAX_VALUE;
    }
}