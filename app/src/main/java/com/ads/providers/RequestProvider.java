package com.ads.providers;

import android.content.Context;
import android.util.Log;
import com.ads.models.FCMBody;
import com.ads.models.FCMResponse;
import com.ads.models.ServiceRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import java.util.Map;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestProvider {
    private static final String TAG = "RequestProvider";

    private final DatabaseReference mDatabaseReference;
    private final NotificationProvider mNotificationProvider;
    private final TokenProvider mTokenProvider;
    private final WorkerProvider mWorkerProvider;
    private final Context context;

    public RequestProvider(Context context) {
        this.context = context;
        this.mDatabaseReference = FirebaseDatabase.getInstance()
                .getReference()
                .child("requests");
        this.mNotificationProvider = new NotificationProvider(context);
        this.mTokenProvider = new TokenProvider();
        this.mWorkerProvider = new WorkerProvider();
    }

    // ========== MÉTODOS DE CREACIÓN ==========

    public Task<DatabaseReference> createRequest(Map<String, Object> data) {
        DatabaseReference newRef = mDatabaseReference.push();
        String id = newRef.getKey();
        data.put("id", id);
        data.put("request_id", id);
        data.put("is_rated", false);

        return newRef.setValue(data).continueWith(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Solicitud creada con éxito: " + id);

                if (data.containsKey("worker_id")) {
                    String workerId = (String) data.get("worker_id");
                    sendNotificationToWorker(workerId, data);
                }

                return newRef;
            } else {
                Log.e(TAG, "Error al crear solicitud", task.getException());
                throw task.getException();
            }
        });
    }

    // ========== MÉTODOS DE CONSULTA ==========

    /**
     * Obtiene todas las solicitudes de un cliente
     */
    public Query getClientRequests(String clientId) {
        return mDatabaseReference
                .orderByChild("client_id")
                .equalTo(clientId);
    }

    /**
     * Obtiene las solicitudes activas de un cliente
     */
    public Query getActiveClientRequests(String clientId) {
        // Firebase no permite múltiples condiciones orderByChild
        // Necesitaremos filtrar en el cliente
        return getClientRequests(clientId);
    }

    /**
     * Obtiene una solicitud específica
     */
    public Task<DataSnapshot> getRequest(String requestId) {
        return mDatabaseReference.child(requestId).get();
    }

    /**
     * Listener para una solicitud específica
     */
    public void listenToRequest(String requestId, ValueEventListener listener) {
        mDatabaseReference.child(requestId).addValueEventListener(listener);
    }

    /**
     * Remover listener de una solicitud
     */
    public void removeRequestListener(String requestId, ValueEventListener listener) {
        mDatabaseReference.child(requestId).removeEventListener(listener);
    }

    /**
     * Listener para todas las solicitudes de un cliente
     */
    public void listenToClientRequests(String clientId, ValueEventListener listener) {
        getClientRequests(clientId).addValueEventListener(listener);
    }

    /**
     * Remover listener de solicitudes del cliente
     */
    public void removeClientRequestsListener(ValueEventListener listener) {
        mDatabaseReference.removeEventListener(listener);
    }

    // ========== MÉTODOS DE ACTUALIZACIÓN DE ESTADO ==========

    /**
     * Actualiza el estado de una solicitud
     */
    public Task<Void> updateRequestStatus(String requestId, String newStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updated_at", System.currentTimeMillis());

        // Agregar timestamp según el estado
        switch (newStatus.toLowerCase()) {
            case "accepted":
                updates.put("accepted_timestamp", System.currentTimeMillis());
                break;
            case "on_the_way":
                updates.put("on_the_way_timestamp", System.currentTimeMillis());
                break;
            case "in_progress":
                updates.put("started_timestamp", System.currentTimeMillis());
                break;
            case "completed":
                updates.put("completion_timestamp", System.currentTimeMillis());
                break;
        }

        return mDatabaseReference.child(requestId).updateChildren(updates);
    }

    /**
     * Actualiza el estado y asigna un trabajador
     */
    public Task<Void> updateRequestStatus(String requestId, String status, String workerId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("worker_id", workerId);
        updates.put("updated_at", System.currentTimeMillis());

        if (status.equalsIgnoreCase("accepted")) {
            updates.put("accepted_timestamp", System.currentTimeMillis());
        }

        return mDatabaseReference.child(requestId).updateChildren(updates);
    }

    /**
     * Acepta una solicitud
     */
    public Task<Void> acceptRequest(String requestId, String workerId, String workerName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "accepted");
        updates.put("worker_id", workerId);
        updates.put("worker_name", workerName);
        updates.put("accepted_timestamp", System.currentTimeMillis());
        updates.put("updated_at", System.currentTimeMillis());

        return mDatabaseReference.child(requestId).updateChildren(updates)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return notifyClientRequestAccepted(requestId);
                    }
                    return Tasks.forException(task.getException());
                });
    }

    /**
     * Rechaza una solicitud
     */
    public Task<Void> rejectRequest(String requestId, String workerId, String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("worker_id", workerId);
        updates.put("rejection_reason", reason);
        updates.put("updated_at", System.currentTimeMillis());

        return mDatabaseReference.child(requestId).updateChildren(updates)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return notifyClientRequestRejected(requestId, reason);
                    }
                    return Tasks.forException(task.getException());
                });
    }

    /**
     * Cancela una solicitud (por el cliente)
     */
    public Task<Void> cancelRequest(String requestId, String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("cancellation_reason", reason);
        updates.put("updated_at", System.currentTimeMillis());

        return mDatabaseReference.child(requestId).updateChildren(updates);
    }

    /**
     * Marca la solicitud como completada
     */
    public Task<Void> completeRequest(String requestId, double finalCost) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "completed");
        updates.put("completion_timestamp", System.currentTimeMillis());
        updates.put("final_cost", finalCost);
        updates.put("updated_at", System.currentTimeMillis());

        return mDatabaseReference.child(requestId).updateChildren(updates);
    }

    // ========== MÉTODOS DE RATING ==========

    /**
     * Guarda el rating del cliente para el trabajador
     */
    public Task<Void> rateRequest(String requestId, float rating, String comment) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("client_rating", rating);
        updates.put("client_comment", comment);
        updates.put("is_rated", true);
        updates.put("rated_at", System.currentTimeMillis());

        return mDatabaseReference.child(requestId).updateChildren(updates)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        // Obtener el worker_id y actualizar su rating
                        return getRequest(requestId).continueWithTask(requestTask -> {
                            if (requestTask.isSuccessful() && requestTask.getResult().exists()) {
                                String workerId = requestTask.getResult()
                                        .child("worker_id").getValue(String.class);
                                if (workerId != null) {
                                    return updateWorkerRating(workerId, rating);
                                }
                            }
                            return Tasks.forResult(null);
                        });
                    }
                    return Tasks.forException(task.getException());
                });
    }

    /**
     * Actualiza el rating promedio del trabajador
     */
    private Task<Void> updateWorkerRating(String workerId, float newRating) {
        return mWorkerProvider.getWorker(workerId).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot workerSnapshot = task.getResult();

                Float currentRating = workerSnapshot.child("rating").getValue(Float.class);
                Integer totalRatings = workerSnapshot.child("totalRatings").getValue(Integer.class);

                if (currentRating == null) currentRating = 0f;
                if (totalRatings == null) totalRatings = 0;

                // Calcular nuevo promedio
                float totalScore = currentRating * totalRatings;
                totalRatings++;
                float newAverage = (totalScore + newRating) / totalRatings;

                Log.d(TAG, String.format("Actualizando rating del trabajador: %.2f (%d ratings)",
                        newAverage, totalRatings));

                return mWorkerProvider.updateWorkerRating(workerId, newAverage, totalRatings);
            }
            return Tasks.forResult(null);
        });
    }

    // ========== MÉTODOS DE NOTIFICACIÓN ==========

    /**
     * Envía notificación a un trabajador específico
     */
    private void sendNotificationToWorker(String workerId, Map<String, Object> requestData) {
        Log.d(TAG, "Enviando notificación al trabajador: " + workerId);

        mTokenProvider.getToken(workerId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String token = task.getResult().child("token").getValue(String.class);

                if (token != null && !token.isEmpty()) {
                    String title = "Nueva Solicitud de Servicio";
                    String body = "Tipo: " + requestData.get("service_type") +
                            "\nDirección: " + requestData.get("address");

                    Map<String, String> notificationData = new HashMap<>();
                    notificationData.put("title", title);
                    notificationData.put("body", body);
                    notificationData.put("requestId", (String) requestData.get("id"));
                    notificationData.put("clientId", (String) requestData.get("client_id"));
                    notificationData.put("type", "new_request");
                    notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

                    FCMBody fcmBody = new FCMBody(token, "high", notificationData);

                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Notificación enviada con éxito");
                            } else {
                                Log.e(TAG, "Error al enviar notificación: " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.e(TAG, "Error en la llamada: " + t.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Token del trabajador no encontrado");
                }
            } else {
                Log.e(TAG, "Error al obtener token del trabajador", task.getException());
            }
        });
    }

    /**
     * Notifica al cliente que su solicitud fue aceptada
     */
    private Task<Void> notifyClientRequestAccepted(String requestId) {
        return mDatabaseReference.child(requestId).get().continueWithTask(task -> {
            if (!task.isSuccessful() || !task.getResult().exists()) {
                return Tasks.forException(new Exception("Request not found"));
            }

            DataSnapshot requestSnapshot = task.getResult();
            String clientId = requestSnapshot.child("client_id").getValue(String.class);
            String workerName = requestSnapshot.child("worker_name").getValue(String.class);

            if (clientId == null) {
                return Tasks.forException(new Exception("Client ID not found"));
            }

            return mTokenProvider.getToken(clientId).get().continueWithTask(tokenTask -> {
                if (!tokenTask.isSuccessful() || !tokenTask.getResult().exists()) {
                    return Tasks.forResult(null);
                }

                String token = tokenTask.getResult().child("token").getValue(String.class);
                if (token == null) {
                    return Tasks.forResult(null);
                }

                String title = "¡Solicitud Aceptada!";
                String body = (workerName != null ? workerName : "Un trabajador") +
                        " ha aceptado tu solicitud";

                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("requestId", requestId);
                notificationData.put("type", "request_accepted");
                notificationData.put("title", title);
                notificationData.put("body", body);
                notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

                FCMBody fcmBody = new FCMBody(token, "high", notificationData);

                return Tasks.call(() -> {
                    try {
                        mNotificationProvider.sendNotification(fcmBody).execute();
                        return null;
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending notification", e);
                        return null;
                    }
                });
            });
        });
    }

    /**
     * Notifica al cliente que su solicitud fue rechazada
     */
    private Task<Void> notifyClientRequestRejected(String requestId, String reason) {
        return mDatabaseReference.child(requestId).get().continueWithTask(task -> {
            if (!task.isSuccessful() || !task.getResult().exists()) {
                return Tasks.forResult(null);
            }

            DataSnapshot requestSnapshot = task.getResult();
            String clientId = requestSnapshot.child("client_id").getValue(String.class);

            if (clientId == null) {
                return Tasks.forResult(null);
            }

            return mTokenProvider.getToken(clientId).get().continueWithTask(tokenTask -> {
                if (!tokenTask.isSuccessful() || !tokenTask.getResult().exists()) {
                    return Tasks.forResult(null);
                }

                String token = tokenTask.getResult().child("token").getValue(String.class);
                if (token == null) {
                    return Tasks.forResult(null);
                }

                String title = "Solicitud Rechazada";
                String body = "Tu solicitud no pudo ser atendida";
                if (reason != null && !reason.isEmpty()) {
                    body += ": " + reason;
                }

                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("requestId", requestId);
                notificationData.put("type", "request_rejected");
                notificationData.put("title", title);
                notificationData.put("body", body);
                notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

                FCMBody fcmBody = new FCMBody(token, "high", notificationData);

                return Tasks.call(() -> {
                    try {
                        mNotificationProvider.sendNotification(fcmBody).execute();
                        return null;
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending notification", e);
                        return null;
                    }
                });
            });
        });
    }

    /**
     * Notifica cambio de estado al cliente
     */
    public Task<Void> notifyStatusChange(String requestId, String newStatus) {
        return mDatabaseReference.child(requestId).get().continueWithTask(task -> {
            if (!task.isSuccessful() || !task.getResult().exists()) {
                return Tasks.forResult(null);
            }

            DataSnapshot requestSnapshot = task.getResult();
            String clientId = requestSnapshot.child("client_id").getValue(String.class);

            if (clientId == null) {
                return Tasks.forResult(null);
            }

            return mTokenProvider.getToken(clientId).get().continueWithTask(tokenTask -> {
                if (!tokenTask.isSuccessful() || !tokenTask.getResult().exists()) {
                    return Tasks.forResult(null);
                }

                String token = tokenTask.getResult().child("token").getValue(String.class);
                if (token == null) {
                    return Tasks.forResult(null);
                }

                String title = getStatusNotificationTitle(newStatus);
                String body = getStatusNotificationBody(newStatus);

                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("requestId", requestId);
                notificationData.put("type", "status_change");
                notificationData.put("status", newStatus);
                notificationData.put("title", title);
                notificationData.put("body", body);
                notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

                FCMBody fcmBody = new FCMBody(token, "high", notificationData);

                return Tasks.call(() -> {
                    try {
                        mNotificationProvider.sendNotification(fcmBody).execute();
                        return null;
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending notification", e);
                        return null;
                    }
                });
            });
        });
    }

    // ========== MÉTODOS DE UTILIDAD ==========

    private String getStatusNotificationTitle(String status) {
        switch (status.toLowerCase()) {
            case "on_the_way":
                return "¡El trabajador va en camino!";
            case "in_progress":
                return "Trabajo en progreso";
            case "completed":
                return "¡Trabajo completado!";
            default:
                return "Actualización de solicitud";
        }
    }

    private String getStatusNotificationBody(String status) {
        switch (status.toLowerCase()) {
            case "on_the_way":
                return "El trabajador se dirige a tu ubicación";
            case "in_progress":
                return "El trabajador ha comenzado el servicio";
            case "completed":
                return "El servicio ha sido completado. ¡Califica tu experiencia!";
            default:
                return "Tu solicitud ha sido actualizada";
        }
    }

    /**
     * Elimina una solicitud
     */
    public Task<Void> deleteRequest(String requestId) {
        return mDatabaseReference.child(requestId).removeValue();
    }

    /**
     * Verifica si existe una solicitud
     */
    public Task<Boolean> requestExists(String requestId) {
        return mDatabaseReference.child(requestId).get()
                .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }

    // ========== GETTERS ==========

    public void getRequests(ValueEventListener callback) {
        mDatabaseReference.addValueEventListener(callback);
    }

    public void getRequest(String requestId, ValueEventListener callback) {
        mDatabaseReference.child(requestId).addValueEventListener(callback);
    }

    public void removeRequestListener(ValueEventListener listener) {
        mDatabaseReference.removeEventListener(listener);
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