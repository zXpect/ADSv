package com.project.ads.providers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.project.ads.models.api.ApiResponse;
import com.project.ads.retrofit.AdsApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cuenta del usuario en el backend. DELETE /api/me/ borra la cuenta de Auth y los
 * datos personales, y anonimiza solicitudes y pagos (D18, HU-13): responde 204 sin
 * cuerpo, por eso no pasa por ApiCallAdapter. 409 si hay un servicio en camino o
 * en progreso.
 */
public class AccountProvider {

    private static final String TAG = "AccountProvider";
    private static final String GENERIC_ERROR = "No se pudo eliminar la cuenta. Intenta de nuevo.";
    private static final String NETWORK_ERROR = "Sin conexión. Revisa tu internet e intenta de nuevo.";

    public void deleteAccount(ApiCallback<Void> callback) {
        AdsApiClient.getApi().deleteAccount().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                    return;
                }
                String message = GENERIC_ERROR;
                try {
                    if (response.errorBody() != null) {
                        ApiResponse<?> error = new Gson().fromJson(response.errorBody().charStream(), ApiResponse.class);
                        if (error != null && error.getError() != null) message = error.getError();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Respuesta de error no estándar", e);
                }
                callback.onError(message, response.code());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Fallo de red al eliminar la cuenta", t);
                callback.onError(NETWORK_ERROR, 0);
            }
        });
    }
}
