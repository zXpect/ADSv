package com.ads.activities.client;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ads.providers.RequestProvider;
import com.ads.providers.NotificationProvider;
import com.ads.providers.TokenProvider;
import com.ads.providers.WorkerProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
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
    private AutoCompleteTextView serviceTypeInput;
    private TextInputEditText descriptionInput;
    private MaterialButton sendRequestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request);
        setupStatusBar();
        initProviders();
        initViews();
        setupServiceTypeDropdown();
        setupSendRequestButton();
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
        String requestId = mRequestProvider.getmDatabase().push().getKey();

        requestData.put("id", requestId);
        requestData.put("client_id", clientId);
        requestData.put("address", address);
        requestData.put("description", description);
        requestData.put("service_type", serviceType);
        requestData.put("status", "pending");
        requestData.put("timestamp", System.currentTimeMillis());

        mRequestProvider.createRequest(requestData)
                .addOnSuccessListener(aVoid -> {
                    showToast("Solicitud creada con éxito");
                    // Buscar trabajadores disponibles y enviar notificaciones
                    findAvailableWorkersAndNotify(requestData);
                    navigateToVerifyRequest(address, serviceType);
                })
                .addOnFailureListener(e -> {
                    showToast("Error al crear la solicitud: " + e.getMessage());
                });
    }

    private void findAvailableWorkersAndNotify(Map<String, Object> requestData) {
        mWorkerProvider.getWorkers().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot workerSnapshot : dataSnapshot.getChildren()) {
                    String workerId = workerSnapshot.getKey();
                    if (workerId != null) {
                        sendNotificationToWorker(workerId, requestData);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ServiceRequest", "Error getting workers: " + databaseError.getMessage());
            }
        });
    }

    private void sendNotificationToWorker(String workerId, Map<String, Object> requestData) {
        mTokenProvider.mDatabase.child(workerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot tokenSnapshot) {
                if (tokenSnapshot.exists()) {
                    String workerToken = tokenSnapshot.child("token").getValue(String.class);
                    if (workerToken != null) {
                        String title = "Nueva Solicitud de Servicio";
                        String body = "Tipo: " + requestData.get("service_type") +
                                "\nDirección: " + requestData.get("address");

                        // Crear datos adicionales para la notificación
                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("requestId", (String) requestData.get("id"));
                        notificationData.put("title", title);
                        notificationData.put("body", body);
                        notificationData.put("clientId", (String) requestData.get("client_id"));

                        mNotificationProvider.sendNotificationToWorker(workerToken, title, body, notificationData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Notification", "Notification sent successfully to worker: " + workerId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Notification", "Error sending notification: " + e.getMessage());
                                });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ServiceRequest", "Error getting worker token: " + databaseError.getMessage());
            }
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
