package com.project.ads.helpers;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
 * Cuenta suspendida (HU-32) o eliminada desde el panel. El backend deshabilita o
 * borra al usuario en Firebase Auth, pero la sesión del teléfono sigue viva hasta
 * que vence el token. Este vigilante escucha el perfil de la cuenta abierta y, si
 * isActive pasa a false o el perfil desaparece después de haber existido, avisa
 * al usuario y cierra la sesión, en cualquier pantalla.
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
    private boolean mDeleted;
    private boolean mProfileSeen;   // en el registro el perfil todavía no existe
    private boolean mShowing;
    // «Eliminar mi cuenta» también borra el perfil: ahí no se avisa
    private static boolean sSelfDeletion;

    private AccountStatusWatcher(Context app) {
        mApp = app;
    }

    public static void install(Application app) {
        AccountStatusWatcher watcher = new AccountStatusWatcher(app.getApplicationContext());
        app.registerActivityLifecycleCallbacks(watcher);
        FirebaseAuth.getInstance().addAuthStateListener(auth -> watcher.watch(auth.getCurrentUser()));
    }

    /** Llamar antes de DELETE /api/me/; con false si la eliminación falla. */
    public static void setSelfDeletion(boolean deleting) {
        sSelfDeletion = deleting;
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
        sSelfDeletion = false;
        mSuspended = false;
        mDeleted = false;
        mProfileSeen = false;
        mRef = FirebaseDatabase.getInstance().getReference("User").child(node).child(uid);
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    mProfileSeen = true;
                    if (isSuspended(snapshot.child("isActive").getValue(Boolean.class)) && !mSuspended) {
                        mSuspended = true;
                        if (mResumed != null) notifySuspended(mResumed);
                    }
                } else if (mProfileSeen && !sSelfDeletion && !mDeleted) {
                    // Eliminada desde el panel (DEF-34)
                    mDeleted = true;
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

    /**
     * Aviso de cuenta suspendida o eliminada que no se puede descartar; al aceptar se
     * cierra la sesión y se vuelve a la bienvenida.
     */
    private void notifySuspended(Activity activity) {
        if (activity.isFinishing() || mShowing) return;
        mShowing = true;
        boolean deleted = mDeleted;
        new MaterialAlertDialogBuilder(activity)
                .setTitle(deleted ? R.string.account_deleted_title : R.string.account_suspended_title)
                .setMessage(deleted ? R.string.account_deleted_message : R.string.account_suspended_message)
                .setCancelable(false)
                .setPositiveButton(R.string.account_suspended_ok, (dialog, which) -> {
                    mShowing = false;
                    mSuspended = false;
                    mDeleted = false;
                    if (deleted) {
                        // El rol y las preferencias de esa cuenta ya no sirven
                        mApp.getSharedPreferences(ROLE_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
                    }
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
        // Si la suspensión o la eliminación llegó con la app en segundo plano, se avisa al volver
        if ((mSuspended || mDeleted) && user != null) notifySuspended(activity);
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
