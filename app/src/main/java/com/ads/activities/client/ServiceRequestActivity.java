package com.ads.activities.client;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ads.models.FCMBody;
import com.ads.models.FCMResponse;
import com.ads.providers.RequestProvider;
import com.ads.providers.NotificationProvider;
import com.ads.providers.TokenProvider;
import com.ads.providers.WorkerProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import java.util.HashMap;
import java.util.Map;

public class ServiceRequestActivity extends AppCompatActivity {

    private RequestProvider mRequestProvider;
    private NotificationProvider mNotificationProvider;
    private TokenProvider mTokenProvider;
    private WorkerProvider mWorkerProvider;
    private TextInputEditText addressInput;
    private TextView serviceTypeText;
    private TextInputEditText descriptionInput;
    private MaterialButton sendRequestButton;
    private ProgressDialog progressDialog;
    private String mWorkerId;
    private String mWorkerServiceType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request);
        setupStatusBar();
        initProviders();
        initViews();
        setupSendRequestButton();

        if (getIntent().hasExtra("workerId")) {
            mWorkerId = getIntent().getStringExtra("workerId");
            Log.d("ServiceRequestActivity", "ID del trabajador recibido: " + mWorkerId);
            // Cargar información del trabajador, incluido su tipo de servicio
            loadWorkerInfo();
        } else {
            Log.e("ServiceRequestActivity", "No se recibió ID del trabajador");
            // Mostrar mensaje y finalizar actividad si no hay ID de trabajador
            Toast.makeText(this, "Error: No se seleccionó un trabajador", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void initProviders() {
        mRequestProvider = new RequestProvider(this);
        mNotificationProvider = new NotificationProvider(this);
        mTokenProvider = new TokenProvider();
        mWorkerProvider = new WorkerProvider();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void initViews() {
        addressInput = findViewById(R.id.et_address);
        serviceTypeText = findViewById(R.id.tv_service_type); // Cambio de ID
        descriptionInput = findViewById(R.id.et_description);
        sendRequestButton = findViewById(R.id.btn_send_request);
    }

    private void loadWorkerInfo() {
        showProgressDialog("Cargando información...");

        // Obtener información del trabajador usando el WorkerProvider
        mWorkerProvider.getWorker(mWorkerId)
                .addOnSuccessListener(dataSnapshot -> {
                    hideProgressDialog();

                    if (dataSnapshot.exists()) {
                        // Obtener el tipo de trabajo del trabajador
                        if (dataSnapshot.child("work").exists()) {
                            mWorkerServiceType = dataSnapshot.child("work").getValue(String.class);

                            // Capitalizar la primera letra para mejor presentación
                            if (mWorkerServiceType != null && !mWorkerServiceType.isEmpty()) {
                                mWorkerServiceType = mWorkerServiceType.substring(0, 1).toUpperCase() +
                                        mWorkerServiceType.substring(1).toLowerCase();

                                // Mostrar el tipo de servicio en el TextView
                                serviceTypeText.setText(mWorkerServiceType);
                            } else {
                                serviceTypeText.setText("No especificado");
                            }
                        } else {
                            serviceTypeText.setText("No especificado");
                        }
                        TextView workerNameTextView = findViewById(R.id.worker_name);
                        String firstName = dataSnapshot.child("name").exists() ?
                                dataSnapshot.child("name").getValue(String.class) : "";
                        String lastName = dataSnapshot.child("lastName").exists() ?
                                dataSnapshot.child("lastName").getValue(String.class) : "";

                        String fullName = firstName + " " + lastName;
                        workerNameTextView.setText(fullName.trim());
                    } else {
                        Toast.makeText(ServiceRequestActivity.this,
                                "No se pudo cargar la información del trabajador", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(ServiceRequestActivity.this,
                            "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ServiceRequestActivity", "Error loading worker info", e);
                });
    }

    private void setupSendRequestButton() {
        sendRequestButton.setOnClickListener(v -> {
            if (validateInputs()) {
                // En lugar de enviar la solicitud aquí, pasamos a la pantalla de verificación
                navigateToVerifyRequest();
            }
        });
    }

    private boolean validateInputs() {
        if (addressInput.getText().toString().trim().isEmpty()) {
            showToast("Por favor, ingresa la dirección.");
            return false;
        }

        if (descriptionInput.getText().toString().trim().isEmpty()) {
            showToast("Por favor, ingresa una descripción.");
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void navigateToVerifyRequest() {
        String address = addressInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String serviceType = mWorkerServiceType != null ? mWorkerServiceType : "No especificado";

        Intent intent = new Intent(this, VerifyRequestActivity.class);

        // Información del trabajador
        intent.putExtra("worker_photo", R.drawable.workerlogo);
        TextView workerNameTextView = findViewById(R.id.worker_name);
        String workerName = (workerNameTextView != null && workerNameTextView.getText() != null)
                ? workerNameTextView.getText().toString()
                : "Trabajador";
        intent.putExtra("worker_name", workerName);

        // Información de la solicitud
        intent.putExtra("address", address);
        intent.putExtra("description", description);
        intent.putExtra("service_type", serviceType);
        intent.putExtra("worker_id", mWorkerId);
        intent.putExtra("amount", "A confirmar");

        // Iniciar la actividad
        startActivity(intent);
        // No finalizamos esta actividad para permitir volver atrás y editar
    }
}