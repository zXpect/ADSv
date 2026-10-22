package com.project.ads.providers;

import com.project.ads.models.Pqr;
import com.project.ads.models.api.ApiBodies;
import com.project.ads.retrofit.AdsApiClient;
import com.project.ads.retrofit.ApiCallAdapter;

import java.util.List;

/**
 * PQR del usuario (mockups 42, 43 y 54). El backend decide el rol desde la
 * solicitud, calcula el plazo de 15 días hábiles y solo devuelve las propias.
 */
public class PqrProvider {

    /** Las PQR propias, la más reciente primero (primera página: 20). */
    public void getMyPqrs(ApiCallback<List<Pqr>> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().getPqrs(), callback);
    }

    public void create(String requestId, String type, String description, ApiCallback<Pqr> callback) {
        ApiCallAdapter.enqueue(AdsApiClient.getApi().createPqr(
                new ApiBodies.PqrBody(requestId, type, description)), callback);
    }
}
