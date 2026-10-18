package com.project.ads.models;

import com.google.firebase.database.DataSnapshot;

/**
 * Notificación guardada en notifications/<uid>/<id>. La escribe el backend al
 * enviar cada push (mismo texto y tipo que el push); la app solo la lee y marca
 * "read". Se llama AppNotification para no chocar con android.app.Notification.
 */
public class AppNotification {

    private String id;
    private String title;
    private String body;
    private String type;
    private String requestId;
    private boolean read;
    private long createdAt;

    public static AppNotification fromSnapshot(DataSnapshot snapshot) {
        AppNotification n = new AppNotification();
        n.id = snapshot.getKey();
        n.title = snapshot.child("title").getValue(String.class);
        n.body = snapshot.child("body").getValue(String.class);
        n.type = snapshot.child("type").getValue(String.class);
        n.requestId = snapshot.child("request_id").getValue(String.class);
        Boolean read = snapshot.child("read").getValue(Boolean.class);
        n.read = read != null && read;
        Long createdAt = snapshot.child("created_at").getValue(Long.class);
        n.createdAt = createdAt != null ? createdAt : 0L;
        return n;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getType() { return type; }
    public String getRequestId() { return requestId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public long getCreatedAt() { return createdAt; }
}
