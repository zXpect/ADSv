package com.project.ads.providers;

import android.net.Uri;

import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.PaymentData;
import com.project.ads.retrofit.AdsApiClient;
import com.project.ads.retrofit.ApiCallAdapter;

/**
 * Pagos a través del backend. La app nunca envía el monto: el servidor lo
 * calcula desde la cotización aceptada. El estado definitivo del pago se lee
 * de requests/<id>/payment_status en Firebase (lo escribe el backend al
 * recibir el evento de la pasarela).
 */
public class PaymentProvider {

    // Deep link de regreso desde el checkout (ver PaymentActivity en el manifest)
    public static final String REDIRECT_SCHEME = "adsapp";
    public static final String REDIRECT_HOST = "payment-result";
    public static final String REDIRECT_PARAM_REQUEST_ID = "request_id";

    // Valores de requests/<id>/payment_status
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_DECLINED = "declined";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_VOIDED = "voided";

    public void createPayment(String requestId, ApiCallback<PaymentData> callback) {
        ApiCallAdapter.enqueue(
                AdsApiClient.getApi().createPayment(
                        new ApiBodies.CreatePaymentBody(requestId, buildRedirectUrl(requestId))),
                callback);
    }

    public void getPayment(String reference, ApiCallback<PaymentData> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().getPayment(reference), callback);
    }

    public static String buildRedirectUrl(String requestId) {
        return new Uri.Builder()
                .scheme(REDIRECT_SCHEME)
                .authority(REDIRECT_HOST)
                .appendQueryParameter(REDIRECT_PARAM_REQUEST_ID, requestId)
                .build()
                .toString();
    }
}
