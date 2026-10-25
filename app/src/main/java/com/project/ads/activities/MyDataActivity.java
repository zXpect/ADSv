package com.project.ads.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.project.ads.R;
import com.project.ads.helpers.AdsSnackbar;
import com.project.ads.helpers.LogoutHelper;
import com.project.ads.providers.AccountProvider;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.ClientProvider;
import com.project.ads.providers.WorkerProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Mis datos · mockup 38 (docs/mockups/MisDatos.dc.html), para cliente y trabajador
 * según el rol guardado en "typeUser". Edita nombres, apellidos y teléfono con los
 * mismos providers y validaciones del registro; el correo es el de Firebase Auth y
 * no se edita. Cambiar contraseña envía el correo de restablecimiento de Firebase.
 * Eliminar cuenta (mockup 39) llama a DELETE /api/me/ y cierra la sesión.
 */
public class MyDataActivity extends AppCompatActivity {

    private static final int MAX_NAME_LENGTH = 50;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{7,10}$");

    private boolean mIsWorker;
    private String mUid;
    private TextInputEditText mNames, mLastNames, mEmail, mPhone;
    private TextInputLayout mNamesLayout, mLastNamesLayout, mPhoneLayout;
    private String mOriginal = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_data);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);
        mIsWorker = "trabajador".equals(pref.getString("user", ""));
        mUid = new AuthProvider().getId();
        if (mUid == null) {
            finish();
            return;
        }

        mNames = findViewById(R.id.et_data_names);
        mLastNames = findViewById(R.id.et_data_last_names);
        mEmail = findViewById(R.id.et_data_email);
        mPhone = findViewById(R.id.et_data_phone);
        mNamesLayout = findViewById(R.id.til_data_names);
        mLastNamesLayout = findViewById(R.id.til_data_last_names);
        mPhoneLayout = findViewById(R.id.til_data_phone);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_data_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_data_password).setOnClickListener(v -> changePassword());
        findViewById(R.id.btn_data_delete).setOnClickListener(v -> confirmDeleteAccount());

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) { updateSaveButton(); }
        };
        mNames.addTextChangedListener(watcher);
        mLastNames.addTextChangedListener(watcher);
        mPhone.addTextChangedListener(watcher);

        Task<DataSnapshot> load = mIsWorker ? new WorkerProvider().getWorker(mUid) : new ClientProvider().getClient(mUid);
        load.addOnSuccessListener(snapshot -> {
            mNames.setText(snapshot.child("name").getValue(String.class));
            mLastNames.setText(snapshot.child("lastName").getValue(String.class));
            String email = snapshot.child("email").getValue(String.class);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            mEmail.setText(email != null ? email : (user != null ? user.getEmail() : ""));
            mPhone.setText(snapshot.child("phone").getValue(String.class));
            mOriginal = snapshotKey();
            showContent();
        }).addOnFailureListener(e -> {
            AdsSnackbar.show(this, R.string.data_load_error);
            showContent();
        });
    }

    private void showContent() {
        findViewById(R.id.progress_data).setVisibility(View.GONE);
        findViewById(R.id.layout_data_content).setVisibility(View.VISIBLE);
        updateSaveButton();
    }

    private String text(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private String snapshotKey() {
        return text(mNames) + "|" + text(mLastNames) + "|" + text(mPhone);
    }

    private void updateSaveButton() {
        findViewById(R.id.btn_data_save).setEnabled(!snapshotKey().equals(mOriginal));
    }

    private boolean validName(String name) {
        return !TextUtils.isEmpty(name) && name.length() <= MAX_NAME_LENGTH && NAME_PATTERN.matcher(name).matches();
    }

    private void save() {
        String name = text(mNames);
        String lastName = text(mLastNames);
        String phone = text(mPhone);
        mNamesLayout.setError(validName(name) ? null : getString(R.string.data_error_name));
        mLastNamesLayout.setError(validName(lastName) ? null : getString(R.string.data_error_last_name));
        mPhoneLayout.setError(PHONE_PATTERN.matcher(phone).matches() ? null : getString(R.string.data_error_phone));
        if (mNamesLayout.getError() != null || mLastNamesLayout.getError() != null || mPhoneLayout.getError() != null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("lastName", lastName);
        updates.put("phone", phone);
        findViewById(R.id.btn_data_save).setEnabled(false);
        Task<Void> task = mIsWorker
                ? new WorkerProvider().updateWorkerFields(mUid, updates)
                : new ClientProvider().updateClient(mUid, updates);
        // Se queda en la pantalla con el aviso de ADS; al cerrar, el aviso no se alcanzaba a ver (DEF-31)
        task.addOnSuccessListener(v -> {
            mOriginal = snapshotKey();
            updateSaveButton();
            AdsSnackbar.show(this, R.string.data_saved);
        }).addOnFailureListener(e -> {
            AdsSnackbar.show(this, R.string.data_save_error);
            updateSaveButton();
        });
    }

    /** Con correo y contraseña envía el enlace de Firebase; con Google no hay contraseña propia. */
    private void changePassword() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        boolean hasPassword = false;
        for (UserInfo info : user.getProviderData()) {
            if ("password".equals(info.getProviderId())) hasPassword = true;
        }
        if (!hasPassword) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.data_password_google)
                    .setPositiveButton(R.string.verify_ok, null)
                    .show();
            return;
        }
        String email = user.getEmail();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.data_change_password)
                .setMessage(getString(R.string.data_password_confirm, email))
                .setPositiveButton(R.string.data_password_send, (d, w) ->
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnSuccessListener(x -> AdsSnackbar.showLong(this, R.string.data_password_sent))
                                .addOnFailureListener(e -> AdsSnackbar.show(this, R.string.data_password_error)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Eliminar cuenta (mockup 39, D18): DELETE /api/me/ y, con 204, se cierra la sesión. */
    private void confirmDeleteAccount() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        View keep = view.findViewById(R.id.btn_delete_account_keep);
        View confirm = view.findViewById(R.id.btn_delete_account_confirm);
        View progress = view.findViewById(R.id.progress_delete_account);
        keep.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            keep.setEnabled(false);
            confirm.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            dialog.setCancelable(false);
            new AccountProvider().deleteAccount(new com.project.ads.providers.ApiCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    dialog.dismiss();
                    Toast.makeText(MyDataActivity.this, R.string.delete_account_done, Toast.LENGTH_LONG).show();
                    // La cuenta ya no existe en Auth: cerrar la sesión local y volver a la bienvenida
                    getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE).edit().clear().apply();
                    LogoutHelper.performLogout(MyDataActivity.this, new AuthProvider(), null, null);
                }

                @Override
                public void onError(String message, int httpStatus) {
                    // 409: hay un servicio en camino o en progreso; el mensaje viene del backend
                    progress.setVisibility(View.GONE);
                    keep.setEnabled(true);
                    confirm.setEnabled(true);
                    dialog.setCancelable(true);
                    ((TextView) view.findViewById(R.id.tv_delete_account_message)).setText(message);
                }
            });
        });
        dialog.show();
    }
}
