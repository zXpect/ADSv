package com.project.ads.providers;

import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.QuoteData;
import com.project.ads.models.api.QuoteLimits;
import com.project.ads.retrofit.AdsApiClient;
import com.project.ads.retrofit.ApiCallAdapter;

/**
 * Cotización de una solicitud. Las reglas de Firebase reservan estos campos al
 * backend, que valida el precio, el motivo del ajuste, la agenda y quién puede
 * cotizar o responder.
 */
public class QuoteProvider {

    public static final String QUOTE_QUOTED = "quoted";
    public static final String QUOTE_ACCEPTED = "accepted";
    public static final String QUOTE_REJECTED = "rejected";

    public void getQuoteLimits(String serviceType, ApiCallback<QuoteLimits> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().getQuoteLimits(serviceType), callback);
    }

    /**
     * Trabajador: envía (o reenvía) la cotización en pesos COP enteros, con el
     * motivo si el precio se ajusta y la agenda (fecha, hora y duración).
     */
    public void submitQuote(String requestId, ApiBodies.QuoteBody body, ApiCallback<QuoteData> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().submitQuote(requestId, body), callback);
    }

    /** Cliente: acepta la cotización con el método de pago elegido (D10); queda fija. */
    public void acceptQuote(String requestId, String paymentMethod, ApiCallback<QuoteData> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().acceptQuote(requestId,
                new ApiBodies.PaymentMethodBody(paymentMethod)), callback);
    }

    /** Cliente: rechaza la cotización; el trabajador puede enviar otra. */
    public void rejectQuote(String requestId, String reason, ApiCallback<QuoteData> callback) {
        ApiCallAdapter.enqueue(
                AdsApiClient.getApi().rejectQuote(requestId, new ApiBodies.RejectQuoteBody(reason)), callback);
    }
}
