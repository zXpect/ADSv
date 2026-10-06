package com.project.ads.providers;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.RatingData;
import com.project.ads.retrofit.AdsApiClient;
import com.project.ads.retrofit.ApiCallAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Acceso a requests/<id> en Firebase.
 *
 * Las notificaciones push y la calificación ya no se hacen desde el teléfono:
 * las envía/registra el backend (NotificationProvider y rateService).
 */
public class RequestProvider {
    private static final String TAG = "RequestProvider";

    private final DatabaseReference mDatabaseReference;
    private final NotificationProvider mNotificationProvider;

    public RequestProvider(Context context) {
        this.mDatabaseReference = FirebaseDatabase.getInstance()
                .getReference()
                .child("requests");
        this.mNotificationProvider = new NotificationProvider();
    }

    // ========== MÉTODOS DE CREACIÓN ==========

    /**
     * Crea la solicitud y pide al backend que notifique al trabajador.
     */
    public Task<DatabaseReference> createRequest(Map<String, Object> data) {
        DatabaseReference newRef = mDatabaseReference.push();
        String id = newRef.getKey();
        data.put("id", id);
        data.put("request_id", id);
        data.put("is_rated", false);

        return newRef.setValue(data).continueWith(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Solicitud creada con éxito: " + id);
                mNotificationProvider.notify(id, NotificationProvider.TYPE_NEW_REQUEST);
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

    // ========== MÉTODOS DE ACTUALIZACIÓN DE ESTADO ==========

    /**
     * Rechaza una solicitud (trabajador) y notifica al cliente
     */
    public Task<Void> rejectRequest(String requestId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("last_updated", System.currentTimeMillis());

        return mDatabaseReference.child(requestId).updateChildren(updates)
                .addOnSuccessListener(aVoid ->
                        mNotificationProvider.notify(requestId, NotificationProvider.TYPE_REQUEST_REJECTED));
    }

    /**
     * Cancela una solicitud (cliente). Las reglas de Firebase solo lo permiten
     * mientras no haya una cotización aceptada.
     */
    public Task<Void> cancelRequest(String requestId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("cancelled_at", System.currentTimeMillis());
        updates.put("last_updated", System.currentTimeMillis());

        return mDatabaseReference.child(requestId).updateChildren(updates);
    }

    // ========== CALIFICACIÓN ==========

    /**
     * Registra la calificación del cliente en el backend, que exige el servicio
     * pagado y recalcula el promedio del trabajador.
     *
     * @param rating de 1 a 5 (entero)
     */
    public void rateService(String requestId, int rating, String comment, ApiCallback<RatingData> callback) {
        ApiCallAdapter.enqueue(
                AdsApiClient.getApi().rateService(requestId, new ApiBodies.RatingBody(rating, comment)),
                callback);
    }

    // ========== GETTERS ==========

    public void getRequest(String requestId, ValueEventListener callback) {
        mDatabaseReference.child(requestId).addValueEventListener(callback);
    }

    public DatabaseReference getRequestReference() {
        return mDatabaseReference;
    }
}
