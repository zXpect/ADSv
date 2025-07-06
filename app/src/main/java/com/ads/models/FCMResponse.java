package com.ads.models;

import retrofit2.Call;
import retrofit2.http.POST;

public class FCMResponse {
    private String name;
    private int success;
    private int failure;
    private String[] results;
    private String[] multicast_id;
    private String[] canonical_ids;

    public FCMResponse(String name) {
        this.name = name;
    }

    // Constructor vacío para deserialización
    public FCMResponse() {
    }

    // Getter Methods
    public String getName() {
        return name;
    }

    public int getSuccess() {
        return success;
    }

    public int getFailure() {
        return failure;
    }

    public String[] getResults() {
        return results;
    }

    public String[] getMulticastId() {
        return multicast_id;
    }

    public String[] getCanonicalIds() {
        return canonical_ids;
    }

    // Setter Methods
    public void setName(String name) {
        this.name = name;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public void setFailure(int failure) {
        this.failure = failure;
    }

    public void setResults(String[] results) {
        this.results = results;
    }

    public void setMulticastId(String[] multicast_id) {
        this.multicast_id = multicast_id;
    }

    public void setCanonicalIds(String[] canonical_ids) {
        this.canonical_ids = canonical_ids;
    }

    // Método para verificar si la notificación fue exitosa
    public boolean isSuccessful() {
        return success > 0;
    }

    // Método para obtener información detallada
    public String getDetailedInfo() {
        return "Success: " + success + ", Failure: " + failure + ", Name: " + name;
    }
}