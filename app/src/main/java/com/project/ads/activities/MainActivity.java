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
import com.google.firebase.auth.FirebaseAuth;
import com.project.ads.R;
import com.project.ads.helpers.NotificationRouter;

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
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.ads_start_bg));
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

    /**
     * Push tocado: con sesión abre su destino sobre el inicio del rol; sin sesión
     * lo guarda y el inicio lo abre después del login (NotificationRouter).
     */
    private boolean handleNotificationIntent() {
        boolean fromPush = getIntent() != null && getIntent().hasExtra(NotificationRouter.EXTRA_TYPE);
        if (NotificationRouter.handleLaunchIntent(this)) return true;
        if (fromPush) {
            Log.d(TAG, "Push sin sesión: el destino se abre después del login");
            Toast.makeText(this, R.string.notification_login_required, Toast.LENGTH_SHORT).show();
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