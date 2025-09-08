package com.ads.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.ads.activities.MainActivity;
import com.ads.activities.worker.RequestDetailActivity;
import com.ads.channel.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessaging";

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d(TAG, "New token received: " + s);
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

        Log.d(TAG, "Notification data - Title: " + title + ", Body: " + body + ", RequestId: " + requestId + ", Type: " + type);

        if (title != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showNotificationApiOreo(title, body, requestId, type);
            } else {
                showNotification(title, body, requestId, type);
            }
        }
    }

    private void showNotification(String title, String body, String requestId, String type) {
        PendingIntent intent = createPendingIntent(requestId, type);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        NotificationCompat.Builder builder = notificationHelper.getNotificationOldAPI(title, body, intent, sound);
        notificationHelper.getManager().notify(generateNotificationId(requestId), builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotificationApiOreo(String title, String body, String requestId, String type) {
        PendingIntent intent = createPendingIntent(requestId, type);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        Notification.Builder builder = notificationHelper.getNotification(title, body, intent, sound);
        notificationHelper.getManager().notify(generateNotificationId(requestId), builder.build());
    }

    private PendingIntent createPendingIntent(String requestId, String type) {
        Intent intent;

        // Check if user is authenticated and get user type
        if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                "new_request".equals(type) &&
                requestId != null &&
                !requestId.isEmpty()) {

            // Check user type from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("typeUser", MODE_PRIVATE);
            String userType = prefs.getString("user", "");

            if ("trabajador".equals(userType)) {
                // Direct intent to RequestDetailActivity for authenticated workers
                intent = new Intent(this, RequestDetailActivity.class);
                intent.putExtra("request_id", requestId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

                Log.d(TAG, "Creating direct intent to RequestDetailActivity for authenticated worker with requestId: " + requestId);
            } else {
                // Use MainActivity for non-workers or unauthenticated users
                intent = createMainActivityIntent(requestId, type);
            }
        } else {
            // Use MainActivity as fallback
            intent = createMainActivityIntent(requestId, type);
        }

        // Use the requestId as requestCode for unique PendingIntent
        int requestCode = generateNotificationId(requestId);

        return PendingIntent.getActivity(
                getBaseContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private Intent createMainActivityIntent(String requestId, String type) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if ("new_request".equals(type) && requestId != null && !requestId.isEmpty()) {
            intent.putExtra("request_id", requestId);
            intent.putExtra("notification_type", type);
            Log.d(TAG, "Creating intent for MainActivity with requestId: " + requestId);
        } else {
            intent.putExtra("notification_type", type != null ? type : "general");
            Log.d(TAG, "Creating default intent for notification type: " + type);
        }

        return intent;
    }

    private int generateNotificationId(String requestId) {
        // Generate a unique notification ID based on request ID
        // If requestId is null, use a default ID
        if (requestId != null && !requestId.isEmpty()) {
            return Math.abs(requestId.hashCode());
        }
        return (int) System.currentTimeMillis() % Integer.MAX_VALUE;
    }
}