package com.project.ads.helpers;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.activities.MainActivity;

/**
 * Cuenta suspendida desde el panel (HU-32). El backend deshabilita al usuario en
 * Firebase Auth y pone isActive = false, pero la sesión del teléfono sigue viva
 * hasta que vence el token. Este vigilante escucha isActive de la cuenta abierta
 * y, si pasa a false, avisa al usuario y cierra la sesión, en cualquier pantalla.
 */
public final class AccountStatusWatcher implements Application.ActivityLifecycleCallbacks {

    private static final String ROLE_PREFS = "typeUser";
    private static final String ROLE_WORKER = "trabajador";
    private static final String ROLE_CLIENT = "cliente";

    private final Context mApp;
    private Activity mResumed;
    private DatabaseReference mRef;
    private ValueEventListener mListener;
    private String mWatchedUid;
    private boolean mSuspended;
    private boolean mShowing;

    private AccountStatusWatcher(Context app) {
        mApp = app;
    }

    public static void install(Application app) {
        AccountStatusWatcher watcher = new AccountStatusWatcher(app.getApplicationContext());
        app.registerActivityLifecycleCallbacks(watcher);
        FirebaseAuth.getInstance().addAuthStateListener(auth -> watcher.watch(auth.getCurrentUser()));
    }

    /** true solo cuando el campo existe y es false: sin el campo, la cuenta está activa. */
    public static boolean isSuspended(Boolean isActive) {
        return Boolean.FALSE.equals(isActive);
    }

    private void watch(@Nullable FirebaseUser user) {
        String uid = user != null ? user.getUid() : null;
        String role = mApp.getSharedPreferences(ROLE_PREFS, Context.MODE_PRIVATE).getString("user", "");
        String node = ROLE_WORKER.equals(role) ? "Trabajadores" : ROLE_CLIENT.equals(role) ? "Clientes" : null;
        if (uid != null && uid.equals(mWatchedUid) && mRef != null) return;

        stop();
        if (uid == null || node == null) return;
        mWatchedUid = uid;
        mSuspended = false;
        mRef = FirebaseDatabase.getInstance().getReference("User").child(node).child(uid).child("isActive");
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isSuspended(snapshot.getValue(Boolean.class)) && !mSuspended) {
                    mSuspended = true;
                    if (mResumed != null) notifySuspended(mResumed);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Con la sesión revocada la lectura puede dejar de permitirse; el login lo explica después
            }
        };
        mRef.addValueEventListener(mListener);
    }

    private void stop() {
        if (mRef != null && mListener != null) mRef.removeEventListener(mListener);
        mRef = null;
        mListener = null;
        mWatchedUid = null;
    }

    /** Aviso que no se puede descartar; al aceptar se cierra la sesión y se vuelve a la bienvenida. */
    private void notifySuspended(Activity activity) {
        if (activity.isFinishing() || mShowing) return;
        mShowing = true;
        new AlertDialog.Builder(activity)
                .setTitle(R.string.account_suspended_title)
                .setMessage(R.string.account_suspended_message)
                .setCancelable(false)
                .setPositiveButton(R.string.account_suspended_ok, (dialog, which) -> {
                    mShowing = false;
                    mSuspended = false;
                    stop();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                })
                .show();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mResumed = activity;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // El rol puede guardarse después del login: se empieza a vigilar al volver a una pantalla
        if (mRef == null && user != null) watch(user);
        // Si la suspensión llegó con la app en segundo plano, se avisa al volver
        if (mSuspended && user != null) notifySuspended(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (mResumed == activity) mResumed = null;
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) { }
    @Override public void onActivityStarted(@NonNull Activity activity) { }
    @Override public void onActivityStopped(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle state) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
