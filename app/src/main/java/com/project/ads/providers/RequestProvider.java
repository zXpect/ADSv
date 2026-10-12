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
import com.project.ads.models.api.RequestActionData;
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
     * Crea la solicitud en el backend (POST /api/requests/), que valida al
     * trabajador y las tareas, congela el precio de referencia, escribe
     * requests/<id> y notifica al trabajador. Las reglas de Firebase ya no
     * dejan que la app cree solicitudes.
     */
    public void createRequest(ApiBodies.CreateRequestBody body, ApiCallback<RequestActionData> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().createRequest(body), callback);
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
     * Cancela la solicitud (cliente o trabajador) con un motivo, hasta antes de
     * «Iniciar viaje» (D8). Lo hace el backend, que guarda quién canceló y
     * avisa a la otra parte; después de on_the_way responde 409 (PQR).
     *
     * @param reasonText obligatorio solo con el motivo «otro»
     */
    public void cancelRequest(String requestId, String reasonCode, String reasonText,
                              ApiCallback<RequestActionData> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().cancelRequest(
                requestId, new ApiBodies.CancelBody(reasonCode, reasonText)), callback);
    }

    /** Trabajador: reprograma una solicitud aceptada; el backend avisa al cliente (D9). */
    public void reschedule(String requestId, long scheduledAt, int durationMin,
                           ApiCallback<RequestActionData> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().reschedule(
                requestId, new ApiBodies.ScheduleBody(scheduledAt, durationMin)), callback);
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
