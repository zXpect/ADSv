package com.ads.providers;

import android.content.Context;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Map;

public class RequestProvider {

    private final DatabaseReference mDatabaseReference;
    private final NotificationProvider notificationProvider;
    private final Context context;

    public RequestProvider(Context context) {
        this.context = context;
        this.mDatabaseReference = FirebaseDatabase.getInstance()
                .getReference()
                .child("requests");
        this.notificationProvider = new NotificationProvider(context);
    }

    public Task<Void> createRequest(Map<String, Object> requestData) {
        String requestId = mDatabaseReference.push().getKey();
        if (requestId == null) {
            return Tasks.forException(new Exception("Failed to generate request ID"));
        }
        requestData.put("request_id", requestId);
        return mDatabaseReference.child(requestId).setValue(requestData);
    }

    public Task<Void> createRequestAndNotify(Map<String, Object> requestData, String workerToken) {
        String requestId = mDatabaseReference.push().getKey();
        if (requestId == null) {
            return Tasks.forException(new Exception("Failed to generate request ID"));
        }

        requestData.put("request_id", requestId);

        Task<Void> saveTask = mDatabaseReference.child(requestId).setValue(requestData);
        Task<Void> notificationTask = notificationProvider.sendNotificationToWorker(
                workerToken,
                "Nueva solicitud",
                "Tienes una nueva solicitud de servicio",
                requestData
        );

        return Tasks.whenAll(saveTask, notificationTask);
    }

    public void getRequests(ValueEventListener callback) {
        mDatabaseReference.addValueEventListener(callback);
    }

    public void getRequest(String requestId, ValueEventListener callback) {
        mDatabaseReference.child(requestId).addValueEventListener(callback);
    }

    public void removeRequestListener(ValueEventListener listener) {
        mDatabaseReference.removeEventListener(listener);
    }

    public void removeRequestListener(String requestId, ValueEventListener listener) {
        mDatabaseReference.child(requestId).removeEventListener(listener);
    }

    public Task<Void> updateRequestStatus(String requestId, String status) {
        return mDatabaseReference.child(requestId).child("status").setValue(status);
    }

    public DatabaseReference getRequestReference() {
        return mDatabaseReference;
    }
}