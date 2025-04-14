package com.ads.providers;

import android.content.Context;
import android.util.Log;
import com.ads.models.FCMBody;
import com.ads.models.FCMResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import java.util.Map;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestProvider {

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

    public Task<DatabaseReference> createRequest(Map<String, Object> data) {
        DatabaseReference newRef = mDatabaseReference.push();
        String id = newRef.getKey();
        data.put("id", id);

        // Retorna una Task que se completará con la referencia completa
        return newRef.setValue(data).continueWith(task -> {
            if (task.isSuccessful()) {
                Log.d("RequestProvider", "Solicitud creada con éxito: " + id);

                // Verificar si la solicitud tiene un ID de trabajador específico
                if (data.containsKey("worker_id")) {
                    String workerId = (String) data.get("worker_id");
                    // Enviar notificación al trabajador seleccionado
                    sendNotificationToWorker(workerId, data);
                }

                return newRef;
            } else {
                Log.e("RequestProvider", "Error al crear solicitud", task.getException());
                throw task.getException();
            }
        });
    }

    // Método para enviar notificación a un trabajador específico
    private void sendNotificationToWorker(String workerId, Map<String, Object> requestData) {
        Log.d("RequestProvider", "Enviando notificación al trabajador: " + workerId);

        mTokenProvider.getToken(workerId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String token = task.getResult().child("token").getValue(String.class);

                if (token != null && !token.isEmpty()) {
                    Log.d("RequestProvider", "Token del trabajador encontrado: " + token);

                    // Crear datos de la notificación
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

                    // Crear el objeto FCMBody
                    FCMBody fcmBody = new FCMBody(token, "high", notificationData);

                    // Enviar notificación usando Retrofit
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.isSuccessful()) {
                                Log.d("RequestProvider", "Notificación enviada con éxito");
                            } else {
                                Log.e("RequestProvider", "Error al enviar notificación: " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.e("RequestProvider", "Error en la llamada: " + t.getMessage());
                        }
                    });
                } else {
                    Log.e("RequestProvider", "Token del trabajador no encontrado");
                }
            } else {
                Log.e("RequestProvider", "Error al obtener token del trabajador", task.getException());
            }
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

            return mTokenProvider.getToken(clientId).get().continueWithTask(tokenTask -> {
                if (!tokenTask.isSuccessful() || !tokenTask.getResult().exists()) {
                    return Tasks.forResult(null);
                }

                String token = tokenTask.getResult().child("token").getValue(String.class);
                if (token == null) {
                    return Tasks.forResult(null);
                }

                String title = "Solicitud Aceptada";
                String body = "Tu solicitud de servicio ha sido aceptada";

                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("requestId", requestId);
                notificationData.put("type", "request_accepted");
                notificationData.put("title", title);
                notificationData.put("body", body);
                notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

                FCMBody fcmBody = new FCMBody(token, "high", notificationData);

                // Crear una tarea personalizada para manejar la respuesta de Retrofit
                return Tasks.call(() -> {
                    try {
                        mNotificationProvider.sendNotification(fcmBody).execute();
                        return null;
                    } catch (Exception e) {
                        Log.e("RequestProvider", "Error sending notification", e);
                        return null;
                    }
                });
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