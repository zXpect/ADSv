package com.project.ads.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.project.ads.R;
import com.project.ads.activities.client.RegisterActivity;
import com.project.ads.activities.worker.FirtsRegisterWorkerActivity;
import com.project.ads.helpers.SystemBarsHelper;

/**
 * Elegir rol · mockup 03 (docs/mockups/Rol.dc.html).
 * Guarda el rol en las preferencias "typeUser" igual que lo hacía MainActivity y
 * abre el registro del rol (modo registro) o el login (modo login).
 */
public class RoleActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_REGISTER = "register";
    public static final String MODE_LOGIN = "login";

    private boolean mLoginMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role);
        // Barra de navegación del color del fondo (sin franja gris abajo)
        SystemBarsHelper.matchNavigationBar(this, R.color.ads_surface,
                getResources().getBoolean(R.bool.ads_light_status_bar));

        mLoginMode = MODE_LOGIN.equals(getIntent().getStringExtra(EXTRA_MODE));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.card_role_client).setOnClickListener(v -> choose("cliente"));
        findViewById(R.id.card_role_worker).setOnClickListener(v -> choose("trabajador"));
        findViewById(R.id.btn_role_switch).setOnClickListener(v -> {
            mLoginMode = !mLoginMode;
            render();
        });
        render();
    }

    private void render() {
        ((TextView) findViewById(R.id.tv_role_subtitle)).setText(
                mLoginMode ? R.string.role_subtitle_login : R.string.role_subtitle_register);
        ((TextView) findViewById(R.id.tv_role_switch_label)).setText(
                mLoginMode ? R.string.role_no_account : R.string.role_have_account);
        ((TextView) findViewById(R.id.btn_role_switch)).setText(
                mLoginMode ? R.string.role_register : R.string.role_login);
    }

    private void choose(String user) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);
        pref.edit().putString("user", user).apply();

        Intent intent;
        if (mLoginMode) {
            intent = new Intent(this, LoginActivity.class);
        } else if ("cliente".equals(user)) {
            intent = new Intent(this, RegisterActivity.class);
        } else {
            intent = new Intent(this, FirtsRegisterWorkerActivity.class);
        }
        startActivity(intent);
    }
}
