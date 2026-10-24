package com.project.ads.helpers;

import android.app.Activity;
import android.util.Log;

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
 * Número de solicitudes nuevas sobre «Solicitudes» en la barra inferior del trabajador
 * (mockup 13): las que esperan respuesta, con la misma regla que la pestaña «Nuevas»
 * de la bandeja. Lee la misma consulta que la bandeja; escucha solo entre start y stop.
 */
public class WorkerRequestsBadge {

    private static final String TAG = "WorkerRequestsBadge";

    private final Activity mActivity;
    private Query mQuery;
    private ValueEventListener mListener;

    public WorkerRequestsBadge(Activity activity) {
        mActivity = activity;
    }

    /** Pendiente, o aceptada sin cotizar: el trabajador tiene que responder. */
    public static boolean isNew(ServiceRequest request) {
        String status = request.getStatus() != null ? request.getStatus().toLowerCase(Locale.ROOT) : "";
        return status.equals("pending") || (status.equals("accepted") && !request.hasQuote());
    }

    /** Llamar en onStart. */
    public void start() {
        String uid = new AuthProvider().getId();
        if (uid == null || mListener != null) return;
        mQuery = FirebaseDatabase.getInstance().getReference().child("requests")
                .orderByChild("worker_id").equalTo(uid);
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object value = child.getValue();
                    if (!(value instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    ServiceRequest request = ServiceRequest.fromMap((Map<String, Object>) value);
                    if (isNew(request)) count++;
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
            nav.removeBadge(R.id.nav_worker_requests);
            return;
        }
        // Círculo naranja con el número en navy (mockup 13); "9+" desde 10
        BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_worker_requests);
        badge.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.ads_brand));
        badge.setBadgeTextColor(ContextCompat.getColor(mActivity, R.color.ads_on_brand));
        badge.setMaxCharacterCount(2);
        badge.setNumber(count);
        badge.setVisible(true);
    }
}
