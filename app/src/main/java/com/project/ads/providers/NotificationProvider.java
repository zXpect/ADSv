package com.project.ads.providers;

import android.util.Log;

import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.NotificationResult;
import com.project.ads.retrofit.AdsApiClient;
import com.project.ads.retrofit.ApiCallAdapter;

/**
 * Pide al backend que envíe una notificación push al otro participante de la
 * solicitud. El backend decide destinatario y texto, y valida que quien
 * notifica tenga el rol correcto. La app ya no envía FCM directamente ni
 * necesita credenciales de servicio.
 */
public class NotificationProvider {

    private static final String TAG = "NotificationProvider";

    // Tipos aceptados por POST /api/notifications/
    public static final String TYPE_NEW_REQUEST = "new_request";
    public static final String TYPE_REQUEST_ACCEPTED = "request_accepted";
    public static final String TYPE_REQUEST_REJECTED = "request_rejected";
    public static final String TYPE_STATUS_CHANGE = "status_change";

    /**
     * Las notificaciones son de mejor esfuerzo: un fallo no interrumpe el flujo.
     */
    public void notify(String requestId, String type) {
        ApiCallAdapter.enqueue(
                AdsApiClient.getApi().notify(new ApiBodies.NotificationBody(requestId, type)),
                new ApiCallback<NotificationResult>() {
                    @Override
                    public void onSuccess(NotificationResult data) {
                        Log.d(TAG, "Notificación " + type + " procesada (enviada: "
                                + (data != null && data.isSent()) + ")");
                    }

                    @Override
                    public void onError(String message, int httpStatus) {
                        Log.w(TAG, "No se pudo notificar " + type + " (HTTP " + httpStatus + ")");
                    }
                });
    }
}
