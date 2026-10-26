package com.project.ads.retrofit;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.project.ads.models.api.ApiResponse;
import com.project.ads.providers.ApiCallback;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Traduce una respuesta del backend ({"success", "data", "error"}) a ApiCallback.
 */
public class ApiCallAdapter<T> implements Callback<ApiResponse<T>> {

    private static final String TAG = "AdsApi";
    private static final String NETWORK_ERROR = "No se pudo conectar con el servidor. Revisa tu conexión.";
    private static final String GENERIC_ERROR = "Ocurrió un error. Intenta nuevamente.";

    private final ApiCallback<T> callback;

    public ApiCallAdapter(ApiCallback<T> callback) {
        this.callback = callback;
    }

    public static <T> void enqueue(Call<ApiResponse<T>> call, ApiCallback<T> callback) {
        call.enqueue(new ApiCallAdapter<>(callback));
    }

    @Override
    public void onResponse(@NonNull Call<ApiResponse<T>> call, @NonNull Response<ApiResponse<T>> response) {
        ApiResponse<T> body = response.body();
        if (response.isSuccessful() && body != null && body.isSuccess()) {
            callback.onSuccess(body.getData());
            return;
        }

        String message = parseErrorMessage(response);
        Log.w(TAG, call.request().method() + " " + call.request().url().encodedPath() + " -> " + response.code());
        callback.onError(message, response.code());
    }

    @Override
    public void onFailure(@NonNull Call<ApiResponse<T>> call, @NonNull Throwable t) {
        Log.e(TAG, "Fallo de red en " + call.request().url().encodedPath(), t);
        callback.onError(NETWORK_ERROR, 0);
    }

    private String parseErrorMessage(Response<ApiResponse<T>> response) {
        if (response.code() == 401) {
            return "Tu sesión expiró. Inicia sesión nuevamente.";
        }
        if (response.code() == 429) {
            return "Demasiadas solicitudes. Espera un momento e intenta de nuevo.";
        }
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return GENERIC_ERROR;
        }
        try {
            ApiResponse<?> error = new Gson().fromJson(errorBody.charStream(), ApiResponse.class);
            if (error != null && error.getError() != null) {
                return error.getError();
            }
            if (error != null && error.getErrors() != null) {
                return "Datos inválidos. Revisa la información ingresada.";
            }
        } catch (Exception e) {
            Log.w(TAG, "Respuesta de error no estándar", e);
        }
        return GENERIC_ERROR;
    }
}
