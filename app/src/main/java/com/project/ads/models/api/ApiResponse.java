package com.project.ads.models.api;

import com.google.gson.JsonElement;

/**
 * Formato estándar de respuesta del backend: {"success": ..., "data": ...}
 * En error: {"success": false, "error": "..."} o {"success": false, "errors": {...}}
 */
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private JsonElement errors;

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public JsonElement getErrors() {
        return errors;
    }
}
