package com.ads.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.ads.activities.client.HomeUserActivity;
import com.ads.activities.worker.HomeWorkerActivity;
import com.ads.activities.worker.RequestDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.ads.R;

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
        SharedPreferences.Editor editor = mPref.edit();

        mButtonClient = findViewById(R.id.buttonLoginClient);
        mButtonWorker = findViewById(R.id.buttonLoginWorker);

        mButtonClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("user","cliente");
                editor.apply();
                goToLogin();
            }

            private void goToLogin() {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        mButtonWorker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("user","trabajador");
                editor.apply();
                goToLoginWorker();
            }

            private void goToLoginWorker() {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Configurar el color de la barra de estado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Verificar si la app se abrió desde una notificación
        if (handleNotificationIntent()) {
            return; // Si manejamos la notificación, no continuamos con el flujo normal
        }

        // Flujo normal de autenticación
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String user = mPref.getString("user", "");

            if (user.equals("cliente")) {
                Toast.makeText(MainActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeUserActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else if (user.equals("trabajador")) {
                Toast.makeText(MainActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeWorkerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }
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