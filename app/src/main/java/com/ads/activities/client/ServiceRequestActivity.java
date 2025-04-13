package com.ads.activities.client;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
    private AutoCompleteTextView serviceTypeInput;
    private TextInputEditText descriptionInput;
    private MaterialButton sendRequestButton;
    private ProgressDialog progressDialog;
    private String mWorkerId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request);
        setupStatusBar();
        initProviders();
        initViews();
        setupServiceTypeDropdown();
        setupSendRequestButton();

        if (getIntent().hasExtra("workerId")) {
            mWorkerId = getIntent().getStringExtra("workerId");
            Log.d("ServiceRequestActivity", "ID del trabajador recibido: " + mWorkerId);
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
        mNotificationProvider = new NotificationProvider();
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



    private void submitRequest(String address, String description, String serviceType) {
        String clientId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> requestData = new HashMap<>();

        requestData.put("client_id", clientId);
        requestData.put("worker_id", mWorkerId);
        requestData.put("address", address);
        requestData.put("description", description);
        requestData.put("service_type", serviceType);
        requestData.put("status", "pending");
        requestData.put("timestamp", System.currentTimeMillis());

        // Mostrar indicador de progreso
        showProgressDialog("Enviando solicitud...");

        mRequestProvider.createRequest(requestData)
                .addOnSuccessListener(databaseReference -> {
                    hideProgressDialog();

                    // El ID ya está incluido en requestData desde el provider
                    // Enviar notificación al trabajador
                    sendNotificationToWorker(mWorkerId, requestData);

                    showToast("Solicitud creada con éxito");
                    navigateToVerifyRequest(address, serviceType);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e("ServiceRequest", "Error creating request: ", e);

                    String errorMessage = "Error al crear la solicitud";
                    if (e.getCause() instanceof com.android.volley.ServerError) {
                        errorMessage = "Error de conexión con el servidor. Intenta de nuevo más tarde.";
                    }

                    showToast(errorMessage);
                });
    }


    private void sendNotificationToWorker(String workerId, Map<String, Object> requestData) {
        Log.d("NotificationDebug", "Enviando notificación al trabajador: " + workerId);

        // Obtener el token del trabajador usando DatabaseReference
        DatabaseReference tokenRef = mTokenProvider.getToken(workerId);

        tokenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.child("token").exists()) {
                    String workerToken = dataSnapshot.child("token").getValue(String.class);

                    if (workerToken != null && !workerToken.isEmpty()) {
                        Log.d("NotificationDebug", "Token del trabajador: " + workerToken);

                        String title = "Nueva Solicitud de Servicio";
                        String body = "Tipo: " + requestData.get("service_type") +
                                "\nDirección: " + requestData.get("address");

                        Map<String, String> notificationData = new HashMap<>();
                        notificationData.put("title", title);
                        notificationData.put("body", body);
                        notificationData.put("clientId", (String) requestData.get("client_id"));

                        // Si tienes un ID de solicitud, agrégalo
                        if (requestData.containsKey("id")) {
                            notificationData.put("requestId", (String) requestData.get("id"));
                        }

                        notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

                        FCMBody fcmBody = new FCMBody(workerToken, "high", notificationData);

                        mNotificationProvider.sendNotification(fcmBody).enqueue(new retrofit2.Callback<FCMResponse>() {
                            @Override
                            public void onResponse(retrofit2.Call<FCMResponse> call, retrofit2.Response<FCMResponse> response) {
                                if (response.isSuccessful()) {
                                    Log.d("NotificationDebug", "Notificación enviada con éxito");
                                } else {
                                    Log.e("NotificationDebug", "Error al enviar notificación: " + response.message());
                                }
                            }

                            @Override
                            public void onFailure(retrofit2.Call<FCMResponse> call, Throwable t) {
                                Log.e("NotificationDebug", "Error en la llamada: " + t.getMessage());
                            }
                        });
                    } else {
                        Log.e("NotificationDebug", "Token del trabajador es nulo o vacío");
                    }
                } else {
                    Log.e("NotificationDebug", "No se encontró el token del trabajador");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("NotificationDebug", "Error al leer token: " + databaseError.getMessage());
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
