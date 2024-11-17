package com.ads.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.ads.activities.worker.RequestDetailActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ads.providers.AuthProvider;
import com.ads.providers.WorkerProvider;
import com.project.ads.R;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        String requestId = data.get("requestId");

        if (remoteMessage.getNotification() != null) {
            showNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    data
            );
        }
    }

    private void showNotification(String title, String body, Map<String, String> data) {
        String channelId = "requests";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Solicitudes",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Asumiendo que tienes una actividad llamada RequestDetailActivity
        Intent intent = new Intent(this, RequestDetailActivity.class);
        intent.putExtra("requestId", data.get("requestId"));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo) //
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(1, builder.build());
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        AuthProvider authProvider = new AuthProvider(this);
        String workerId = authProvider.getId();

        if (workerId != null) {
            // Guardar token localmente
            authProvider.saveFCMToken(token);

            // Actualizar token en la base de datos
            WorkerProvider workerProvider = new WorkerProvider();
            workerProvider.updateWorkerFCMToken(workerId, token)
                    .addOnSuccessListener(aVoid -> {
                        // Token actualizado exitosamente
                    })
                    .addOnFailureListener(e -> {
                        // Manejar el error
                    });
        }
    }
}