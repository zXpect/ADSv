package com.project.ads.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.project.ads.activities.client.HomeUserActivity;
import com.project.ads.activities.worker.HomeWorkerActivity;
import com.project.ads.activities.worker.RequestDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.project.ads.R;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    Button mButtonClient;
    Button mButtonWorker;
    SharedPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);

        // Bienvenida (mockup 02): ambos caminos pasan por Elegir rol, que guarda el rol
        mButtonClient = findViewById(R.id.buttonLoginClient);
        mButtonWorker = findViewById(R.id.buttonLoginWorker);
        mButtonClient.setOnClickListener(v -> openRole(RoleActivity.MODE_REGISTER));
        mButtonWorker.setOnClickListener(v -> openRole(RoleActivity.MODE_LOGIN));

        // Barra de estado navy con íconos claros
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.ads_header));
        new WindowInsetsControllerCompat(window, window.getDecorView()).setAppearanceLightStatusBars(false);
    }

    private void openRole(String mode) {
        Intent intent = new Intent(this, RoleActivity.class);
        intent.putExtra(RoleActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (handleNotificationIntent()) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String user = mPref.getString("user", "");

            if (user.equals("cliente")) {
                goToHomeUser();
            } else if (user.equals("trabajador")) {
                goToHomeWorker();
            } else {
                auth.signOut(); // evitar entrar al home por error
            }
        }
    }

    private void goToHomeUser() {
        Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HomeUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void goToHomeWorker() {
        Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HomeWorkerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private boolean handleNotificationIntent() {
        Intent intent = getIntent();
        Log.d(TAG, "Checking intent extras: " + (intent.getExtras() != null ? intent.getExtras().toString() : "null"));

        if (intent != null && intent.hasExtra("request_id")) {
            String requestId = intent.getStringExtra("request_id");
            Log.d(TAG, "App opened from notification with request ID: " + requestId);

            // Verificar que el usuario está autenticado y es trabajador
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String userType = mPref.getString("user", "");

                if ("trabajador".equals(userType)) {
                    // Abrir directamente RequestDetailActivity
                    Intent requestDetailIntent = new Intent(this, RequestDetailActivity.class);
                    requestDetailIntent.putExtra("request_id", requestId);
                    requestDetailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(requestDetailIntent);
                    finish();
                    return true;
                } else {
                    Log.w(TAG, "Notification intended for worker but user type is: " + userType);
                    Toast.makeText(this, "Esta notificación es para trabajadores", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "User not authenticated, cannot open request details");
                Toast.makeText(this, "Debes iniciar sesión para ver los detalles", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Manejar el nuevo intent si contiene datos de notificación
        if (handleNotificationIntent()) {
            return;
        }
    }
}