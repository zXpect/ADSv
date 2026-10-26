package com.project.ads.models.api;

public class PaymentData {
    private String reference;
    private String service_request_id;
    private long amount_in_cents;
    private long amount_cop;
    private String currency;
    private String status;
    private String checkout_url;

    public String getReference() {
        return reference;
    }

    public String getServiceRequestId() {
        return service_request_id;
    }

    public long getAmountInCents() {
        return amount_in_cents;
    }

    public long getAmountCop() {
        return amount_cop;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getCheckoutUrl() {
        return checkout_url;
    }
}
