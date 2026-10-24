package com.project.ads.helpers;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.models.ServiceRequest;
import com.project.ads.providers.AuthProvider;

import java.util.Locale;
import java.util.Map;

/**
 * Número sobre «Solicitudes» en la barra inferior: para el trabajador, las solicitudes
 * por responder (mockup 13, regla de la pestaña «Nuevas» de la bandeja); para el
 * cliente, las activas (regla de la sección «Activas» del historial). Círculo naranja
 * con el número en navy. Escucha las solicitudes del usuario solo entre start y stop.
 */
public class RequestsNavBadge {

    private static final String TAG = "RequestsNavBadge";

    private interface Rule {
        boolean counts(ServiceRequest request);
    }

    private final Activity mActivity;
    private final String mOwnerField;
    @IdRes private final int mMenuItem;
    private final Rule mRule;
    private Query mQuery;
    private ValueEventListener mListener;

    private RequestsNavBadge(Activity activity, String ownerField, @IdRes int menuItem, Rule rule) {
        mActivity = activity;
        mOwnerField = ownerField;
        mMenuItem = menuItem;
        mRule = rule;
    }

    /** Barra del trabajador: solicitudes nuevas sobre «Solicitudes». */
    public static RequestsNavBadge forWorker(Activity activity) {
        return new RequestsNavBadge(activity, "worker_id", R.id.nav_worker_requests, RequestsNavBadge::isNewForWorker);
    }

    /** Barra del cliente: solicitudes activas sobre «Solicitudes». */
    public static RequestsNavBadge forClient(Activity activity) {
        return new RequestsNavBadge(activity, "client_id", R.id.nav_client_requests, RequestsNavBadge::isActiveForClient);
    }

    /** Trabajador: pendiente, o aceptada sin cotizar (tiene que responder). */
    public static boolean isNewForWorker(ServiceRequest request) {
        String status = status(request);
        return status.equals("pending") || (status.equals("accepted") && !request.hasQuote());
    }

    /** Cliente: todavía no termina ni se canceló. */
    public static boolean isActiveForClient(ServiceRequest request) {
        String status = status(request);
        return !status.isEmpty() && !status.equals("completed")
                && !status.equals("cancelled") && !status.equals("rejected");
    }

    private static String status(ServiceRequest request) {
        return request.getStatus() != null ? request.getStatus().toLowerCase(Locale.ROOT) : "";
    }

    /** Llamar en onStart. */
    public void start() {
        String uid = new AuthProvider().getId();
        if (uid == null || mListener != null) return;
        mQuery = FirebaseDatabase.getInstance().getReference().child("requests")
                .orderByChild(mOwnerField).equalTo(uid);
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object value = child.getValue();
                    if (!(value instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    ServiceRequest request = ServiceRequest.fromMap((Map<String, Object>) value);
                    if (mRule.counts(request)) count++;
                }
                show(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "No se pudieron contar las solicitudes: " + error.getMessage());
                show(0);
            }
        };
        mQuery.addValueEventListener(mListener);
    }

    /** Llamar en onStop. */
    public void stop() {
        if (mQuery != null && mListener != null) mQuery.removeEventListener(mListener);
        mQuery = null;
        mListener = null;
    }

    private void show(int count) {
        BottomNavigationView nav = mActivity.findViewById(R.id.bottom_nav);
        if (nav == null) return;
        if (count <= 0) {
            nav.removeBadge(mMenuItem);
            return;
        }
        // "9+" desde 10
        BadgeDrawable badge = nav.getOrCreateBadge(mMenuItem);
        badge.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.ads_brand));
        badge.setBadgeTextColor(ContextCompat.getColor(mActivity, R.color.ads_on_brand));
        badge.setMaxCharacterCount(2);
        badge.setNumber(count);
        badge.setVisible(true);
    }
}
