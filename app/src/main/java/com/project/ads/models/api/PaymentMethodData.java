package com.project.ads.models.api;

/** Respuesta de POST /api/requests/<id>/payment-method/. */
public class PaymentMethodData {
    private String request_id;
    private String payment_method;
    private String payment_status;

    public String getRequestId() {
        return request_id;
    }

    public String getPaymentMethod() {
        return payment_method;
    }

    public String getPaymentStatus() {
        return payment_status;
    }
}
