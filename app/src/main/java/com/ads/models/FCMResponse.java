package com.ads.models;

import retrofit2.Call;
import retrofit2.http.POST;

public class FCMResponse {
    private String name;

    public FCMResponse(String name) {
        this.name = name;
    }

// Getter Methods

    public String getName() {
        return name;
    }

    // Setter Methods

    public void setName( String name ) {
        this.name = name;
    }
}
