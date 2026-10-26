package com.project.ads.retrofit;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Adjunta el ID token de Firebase Auth a cada llamada al backend
 * (Authorization: Bearer <idToken>). Si el backend responde 401, pide un
 * token nuevo una sola vez y reintenta.
 *
 * OkHttp ejecuta interceptores y authenticators fuera del hilo principal,
 * por eso aquí se puede esperar la Task de Firebase de forma síncrona.
 */
public class FirebaseIdTokenInterceptor implements Interceptor, Authenticator {

    private static final String TAG = "FirebaseIdToken";
    private static final String HEADER = "Authorization";
    private static final long TOKEN_TIMEOUT_SECONDS = 15;

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        String token = getIdToken(false);
        Request request = chain.request();
        if (token != null) {
            request = request.newBuilder().header(HEADER, "Bearer " + token).build();
        }
        return chain.proceed(request);
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) {
        // Solo un reintento: si ya se reintentó con token renovado, se rinde
        if (response.priorResponse() != null) {
            return null;
        }
        String token = getIdToken(true);
        if (token == null) {
            return null;
        }
        return response.request().newBuilder().header(HEADER, "Bearer " + token).build();
    }

    @Nullable
    private String getIdToken(boolean forceRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        try {
            GetTokenResult result = Tasks.await(user.getIdToken(forceRefresh), TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result.getToken();
        } catch (Exception e) {
            // No se registra el token, solo el error
            Log.e(TAG, "No se pudo obtener el ID token de Firebase", e);
            return null;
        }
    }
}
