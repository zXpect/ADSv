package com.ads.activities.client;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ads.providers.RequestProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.project.ads.R;
import java.util.HashMap;
import java.util.Map;

public class ServiceRequestActivity extends AppCompatActivity {

    private RequestProvider mRequestProvider;
    private TextInputEditText addressInput;
    private AutoCompleteTextView serviceTypeInput;
    private TextInputEditText descriptionInput;
    private MaterialButton sendRequestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request);
        setupStatusBar();
        initViews();
        setupServiceTypeDropdown();
        setupSendRequestButton();
        mRequestProvider = new RequestProvider(this);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void initViews() {
        addressInput = findViewById(R.id.et_address);
        serviceTypeInput = findViewById(R.id.spinner_service_type);
        descriptionInput = findViewById(R.id.et_description);
        sendRequestButton = findViewById(R.id.btn_send_request);
    }

    private void setupServiceTypeDropdown() {
        String[] items = getResources().getStringArray(R.array.service_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                items
        );
        serviceTypeInput.setAdapter(adapter);
    }

    private void setupSendRequestButton() {
        sendRequestButton.setOnClickListener(v -> {
            if (validateInputs()) {
                sendServiceRequest();
            }
        });
    }

    private boolean validateInputs() {
        if (addressInput.getText().toString().trim().isEmpty()) {
            showToast("Por favor, ingresa la dirección.");
            return false;
        }
        if (serviceTypeInput.getText().toString().trim().isEmpty()) {
            showToast("Por favor, selecciona un tipo de servicio.");
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

    private void sendServiceRequest() {
        String address = addressInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String serviceType = serviceTypeInput.getText().toString().trim();
        submitRequest(address, description, serviceType);
    }

    private void submitRequest(String address, String description, String serviceType) {
        String clientId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("client_id", clientId);
        requestData.put("address", address);
        requestData.put("description", description);
        requestData.put("service_type", serviceType);
        requestData.put("status", "pending");
        requestData.put("timestamp", System.currentTimeMillis());

        mRequestProvider.createRequest(requestData)
                .addOnSuccessListener(aVoid -> {
                    showToast("Solicitud creada con éxito");
                    navigateToVerifyRequest(address, serviceType);
                })
                .addOnFailureListener(e -> {
                    showToast("Error al crear la solicitud: " + e.getMessage());
                });
    }

    private void navigateToVerifyRequest(String address, String serviceType) {
        Intent intent = new Intent(this, VerifyRequestActivity.class);
        intent.putExtra("worker_photo", R.drawable.workerlogo);
        intent.putExtra("worker_name", "Nombre del Trabajador");
        intent.putExtra("address", address);
        intent.putExtra("service_type", serviceType);
        intent.putExtra("amount", "Monto del Servicio");
        startActivity(intent);
        finish(); // Opcional: cierra esta actividad si no quieres que el usuario regrese a ella
    }
}
