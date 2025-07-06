package com.ads.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.ads.activities.worker.RequestDetailActivity;
import com.ads.channel.NotificationHelper;
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
        
        // Check if this is a new request notification that should open RequestDetailActivity
        if ("new_request".equals(type) && requestId != null && !requestId.isEmpty()) {
            intent = new Intent(this, RequestDetailActivity.class);
            intent.putExtra("request_id", requestId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            
            Log.d(TAG, "Creating intent for RequestDetailActivity with requestId: " + requestId);
        } else {
            // Default intent for other notification types
            intent = new Intent();
            Log.d(TAG, "Creating default intent for notification type: " + type);
        }
        
        return PendingIntent.getActivity(
            getBaseContext(), 
            generateNotificationId(requestId), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    
    private int generateNotificationId(String requestId) {
        // Generate a unique notification ID based on request ID
        // If requestId is null, use a default ID
        if (requestId != null && !requestId.isEmpty()) {
            return requestId.hashCode();
        }
        return 1;
    }
}
