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
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.activities.MainActivity;
import com.project.ads.models.api.DeletedStatus;
import com.project.ads.providers.AccountProvider;
import com.project.ads.providers.ApiCallback;

/**
 * Cuenta suspendida (HU-32) o eliminada desde el panel. El backend deshabilita o
 * borra al usuario en Firebase Auth, pero la sesión del teléfono sigue viva hasta
 * que vence el token. Este vigilante escucha el perfil de la cuenta abierta y, si
 * isActive pasa a false o el perfil desaparece después de haber existido, avisa
 * al usuario y cierra la sesión, en cualquier pantalla.
 *
 * Si la cuenta se eliminó o suspendió con la app cerrada, el perfil ya no existe al
 * abrirla y el listener no lo nota: por eso también se recarga el usuario de Firebase
 * Auth al abrir la app y al volver a ella (USER_NOT_FOUND o USER_DISABLED). El aviso
 * da el correo de soporte y un botón para escribirle.
 *
 * Con la app en segundo plano, Firebase puede cerrar la sesión por su cuenta al ver
 * que el usuario ya no existe, antes de que este vigilante pregunte. Por eso, si la
 * sesión de una cuenta vigilada se cierra sin pasar por aquí, se consulta
 * POST /api/accounts/deleted-status/ con su correo y, si la eliminó el panel, se avisa.
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
    // Recarga del usuario de Auth como mucho cada 30 s al cambiar de pantalla
    private static final long AUTH_CHECK_INTERVAL_MS = 30_000;
    private long mLastAuthCheck;
    // Correo de la última cuenta vigilada, para preguntar si la eliminaron al perder la sesión
    private String mLastEmail;

    private AccountStatusWatcher(Context app) {
        mApp = app;
    }

    public static void install(Application app) {
        AccountStatusWatcher watcher = new AccountStatusWatcher(app.getApplicationContext());
        app.registerActivityLifecycleCallbacks(watcher);
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) watcher.onSessionLost();
            watcher.watch(user);
        });
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
        if (user != null && user.getEmail() != null) mLastEmail = user.getEmail();
        if (uid != null) checkAuthUser(user, true);
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
                // Con la sesión revocada la lectura deja de permitirse: se pregunta a Firebase Auth
                FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
                if (current != null) checkAuthUser(current, true);
            }
        };
        mRef.addValueEventListener(mListener);
    }

    /**
     * Recarga el usuario: si ya no existe (eliminado) o está deshabilitado
     * (suspendido), avisa igual que el listener. Sin red no hace nada.
     */
    private void checkAuthUser(@NonNull FirebaseUser user, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - mLastAuthCheck < AUTH_CHECK_INTERVAL_MS) return;
        mLastAuthCheck = now;
        String uid = user.getUid();
        user.reload().addOnFailureListener(e -> {
            if (!(e instanceof FirebaseAuthInvalidUserException) || sSelfDeletion || mShowing) return;
            // Firebase pudo cerrar ya la sesión por su cuenta (current == null): se avisa igual
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current != null && !uid.equals(current.getUid())) return;
            String code = ((FirebaseAuthInvalidUserException) e).getErrorCode();
            if ("ERROR_USER_NOT_FOUND".equals(code)) {
                mDeleted = true;
            } else if ("ERROR_USER_DISABLED".equals(code)) {
                mSuspended = true;
            } else {
                return;
            }
            if (mResumed != null) notifySuspended(mResumed);
        });
    }

    /**
     * La sesión se cerró. Si fue un cierre normal, el correo no figura como eliminado
     * y no pasa nada; si la cuenta la eliminó el panel, se avisa (al volver a la app,
     * si está en segundo plano).
     */
    private void onSessionLost() {
        String email = mLastEmail;
        mLastEmail = null;
        if (email == null || sSelfDeletion || mShowing || mDeleted) return;
        new AccountProvider().getDeletedStatus(email, new ApiCallback<DeletedStatus>() {
            @Override
            public void onSuccess(DeletedStatus status) {
                if (status == null || !status.isDeleted() || !status.isDeletedByAdmin() || mShowing) return;
                if (FirebaseAuth.getInstance().getCurrentUser() != null) return; // ya entró con otra cuenta
                mDeleted = true;
                if (mResumed != null) notifySuspended(mResumed);
            }

            @Override
            public void onError(String message, int httpStatus) {
                // Sin red o sin backend: el login lo explica si intenta entrar de nuevo
            }
        });
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
                .setPositiveButton(R.string.account_suspended_ok, (dialog, which) -> signOut(activity, deleted))
                .setNeutralButton(R.string.support_write, (dialog, which) -> {
                    // La sesión se cierra igual; el correo se abre encima de la bienvenida
                    signOut(activity, deleted);
                    SupportMail.open(mApp);
                })
                .show();
    }

    private void signOut(Activity activity, boolean deleted) {
        mShowing = false;
        mLastEmail = null; // cierre propio: no hace falta preguntar al backend
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
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mResumed = activity;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // El rol puede guardarse después del login: se empieza a vigilar al volver a una pantalla
        if (mRef == null && user != null) watch(user);
        // Eliminada o suspendida con la app cerrada o en segundo plano
        if (user != null && !mShowing) checkAuthUser(user, false);
        // Si la suspensión o la eliminación llegó con la app en segundo plano, se avisa al volver
        if (mDeleted || (mSuspended && user != null)) notifySuspended(activity);
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
