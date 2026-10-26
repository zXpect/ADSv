package com.project.ads.models.api;

public class QuoteLimits {
    private long min_price;
    private long max_price;
    private String currency;

    public long getMinPrice() {
        return min_price;
    }

    public long getMaxPrice() {
        return max_price;
    }

    public String getCurrency() {
        return currency;
    }
}
