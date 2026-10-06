package com.project.ads.models.api;

public class QuoteData {
    private String request_id;
    private String status;
    private Long quoted_price;
    private String quote_status;

    public String getRequestId() {
        return request_id;
    }

    public String getStatus() {
        return status;
    }

    public Long getQuotedPrice() {
        return quoted_price;
    }

    public String getQuoteStatus() {
        return quote_status;
    }
}
