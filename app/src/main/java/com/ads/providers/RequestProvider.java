package com.ads.providers;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.util.Map;
import java.util.HashMap;

public class RequestProvider {

    private final DatabaseReference mDatabaseReference;
    private final NotificationProvider notificationProvider;
    private final TokenProvider tokenProvider;
    private final WorkerProvider workerProvider;
    private final Context context;

    public RequestProvider(Context context) {
        this.context = context;
        this.mDatabaseReference = FirebaseDatabase.getInstance()
                .getReference()
                .child("requests");
        this.notificationProvider = new NotificationProvider(context);
        this.tokenProvider = new TokenProvider();
        this.workerProvider = new WorkerProvider();
    }

    public Task<Void> createRequest(Map<String, Object> requestData) {
        Log.d("RequestProvider", "createRequest() called");
        String requestId = mDatabaseReference.push().getKey();
        if (requestId == null) {
            return Tasks.forException(new Exception("Failed to generate request ID"));
        }
        Log.d("RequestProvider", "Request data: " + requestData.toString());
        requestData.put("request_id", requestId);

        // Primero guardamos la solicitud
        Task<Void> saveTask = mDatabaseReference.child(requestId).setValue(requestData);

        // Luego notificamos a los trabajadores disponibles
        return saveTask.continueWithTask(task -> {
            if (task.isSuccessful()) {
                return notifyAvailableWorkers(requestData);
            } else {
                throw task.getException();
            }
        });
    }

    // En RequestProvider.java, añade más logs para depurar
    private Task<Void> notifyAvailableWorkers(Map<String, Object> requestData) {
        Log.d("RequestProvider", "Initiating notification to available workers");
        return workerProvider.getAvailableWorkers().continueWithTask(workersTask -> {
            if (!workersTask.isSuccessful()) {
                Log.e("RequestProvider", "Failed to get available workers", workersTask.getException());
                return Tasks.forException(workersTask.getException());
            }

            DataSnapshot workersSnapshot = workersTask.getResult();
            if (!workersSnapshot.exists()) {
                Log.e("RequestProvider", "No available workers found");
                return Tasks.forException(new Exception("No available workers found"));
            }

            Log.d("RequestProvider", "Found " + workersSnapshot.getChildrenCount() + " available workers");
            // Crear las tareas de notificación para cada trabajador
            java.util.List<Task<Void>> notificationTasks = new java.util.ArrayList<>();

            for (DataSnapshot workerSnapshot : workersSnapshot.getChildren()) {
                String workerId = workerSnapshot.getKey();
                if (workerId != null) {
                    Task<Void> notificationTask = sendWorkerNotification(workerId, requestData);
                    notificationTasks.add(notificationTask);
                }
            }

            return Tasks.whenAll(notificationTasks);
        });
    }

    private Task<Void> sendWorkerNotification(String workerId, Map<String, Object> requestData) {
        return tokenProvider.mDatabase.child(workerId).get().continueWithTask(tokenTask -> {
            if (!tokenTask.isSuccessful() || !tokenTask.getResult().exists()) {
                Log.w("RequestProvider", "No token found for worker: " + workerId);
                return Tasks.forResult(null);
            }

            String token = tokenTask.getResult().child("token").getValue(String.class);
            if (token == null) {
                return Tasks.forResult(null);
            }

            // Preparar los datos de la notificación
            String title = "Nueva Solicitud de Servicio";
            String body = String.format("Tipo: %s\nDirección: %s",
                    requestData.get("service_type"),
                    requestData.get("address"));

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("request_id", (String) requestData.get("request_id"));
            notificationData.put("client_id", (String) requestData.get("client_id"));
            notificationData.put("service_type", (String) requestData.get("service_type"));
            notificationData.put("address", (String) requestData.get("address"));
            notificationData.put("status", "pending");

            return notificationProvider.sendNotificationViaNetlify(token, title, body, notificationData);
        });
    }

    public Task<Void> updateRequestStatus(String requestId, String status, String workerId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("worker_id", workerId);
        updates.put("updated_at", System.currentTimeMillis());

        return mDatabaseReference.child(requestId).updateChildren(updates);
    }

    public Task<Void> acceptRequest(String requestId, String workerId) {
        return updateRequestStatus(requestId, "accepted", workerId)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        // Notificar al cliente que su solicitud fue aceptada
                        return notifyClientRequestAccepted(requestId);
                    }
                    return Tasks.forException(task.getException());
                });
    }

    private Task<Void> notifyClientRequestAccepted(String requestId) {
        return mDatabaseReference.child(requestId).get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }

            DataSnapshot requestSnapshot = task.getResult();
            String clientId = requestSnapshot.child("client_id").getValue(String.class);
            if (clientId == null) {
                return Tasks.forException(new Exception("Client ID not found"));
            }

            return tokenProvider.mDatabase.child(clientId).get().continueWithTask(tokenTask -> {
                if (!tokenTask.isSuccessful() || !tokenTask.getResult().exists()) {
                    return Tasks.forResult(null);
                }

                String token = tokenTask.getResult().child("token").getValue(String.class);
                if (token == null) {
                    return Tasks.forResult(null);
                }

                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("request_id", requestId);
                notificationData.put("type", "request_accepted");

                return notificationProvider.sendNotificationViaNetlify(
                        token,
                        "Solicitud Aceptada",
                        "Tu solicitud de servicio ha sido aceptada",
                        notificationData
                );
            });
        });
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

    public DatabaseReference getRequestReference() {
        return mDatabaseReference;
    }

    public DatabaseReference getmDatabase() {
        return mDatabaseReference;
    }

    public DatabaseReference getWorkers() {
        return FirebaseDatabase.getInstance()
                .getReference()
                .child("User")
                .child("Trabajadores");
    }
}