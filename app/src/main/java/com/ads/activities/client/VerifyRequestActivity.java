package com.ads.activities.client;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ads.models.FCMBody;
import com.ads.models.FCMResponse;
import com.ads.providers.RequestProvider;
import com.ads.providers.NotificationProvider;
import com.ads.providers.TokenProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;

import java.util.HashMap;
import java.util.Map;

public class VerifyRequestActivity extends AppCompatActivity {

    private static final String TAG = "VerifyRequestActivity";

    private ImageView mWorkerPhotoImageView;
    private TextView mWorkerNameTextView;
    private TextView mAddressTextView;
    private TextView mServiceTypeTextView;
    private TextView mAmountTextView;
    private Button mConfirmButton;
    private ProgressDialog mProgressDialog;

    // Nuevos providers necesarios para enviar la solicitud
    private RequestProvider mRequestProvider;
    private NotificationProvider mNotificationProvider;
    private TokenProvider mTokenProvider;

    // Datos para la solicitud
    private String mWorkerId;
    private String mAddress;
    private String mDescription;
    private String mServiceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_request);

        // Configurar la barra de estado transparente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }

        // Inicializar providers
        initProviders();

        // Inicializar vistas
        initViews();

        // Cargar datos recibidos desde la actividad anterior
        loadReceivedData();

        // Configurar el botón de confirmación
        setupConfirmButton();
    }

    private void initProviders() {
        mRequestProvider = new RequestProvider(this);
        mNotificationProvider = new NotificationProvider(this);
        mTokenProvider = new TokenProvider();
    }

    private void initViews() {
        mWorkerPhotoImageView = findViewById(R.id.worker_photo);
        mWorkerNameTextView = findViewById(R.id.worker_name);
        mAddressTextView = findViewById(R.id.tv_address_content);
        mServiceTypeTextView = findViewById(R.id.tv_service_type_content);
        mAmountTextView = findViewById(R.id.tv_amount_content);
        mConfirmButton = findViewById(R.id.btn_confirm);
    }

    private void loadReceivedData() {
        if (getIntent() != null) {
            // Obtener los datos del Intent
            int workerPhotoResId = getIntent().getIntExtra("worker_photo", R.drawable.workerlogo);
            String workerName = getIntent().getStringExtra("worker_name");
            mAddress = getIntent().getStringExtra("address");
            mDescription = getIntent().getStringExtra("description");
            mServiceType = getIntent().getStringExtra("service_type");
            mWorkerId = getIntent().getStringExtra("worker_id");
            String amount = getIntent().getStringExtra("amount");

            Log.d(TAG, "Datos recibidos - Worker name: " + workerName +
                    ", Address: " + mAddress +
                    ", Service type: " + mServiceType +
                    ", Worker ID: " + mWorkerId);

            // Configurar las vistas con los datos recibidos
            mWorkerPhotoImageView.setImageResource(workerPhotoResId);

            // Asegurar que existe un nombre para mostrar
            if (workerName != null && !workerName.isEmpty()) {
                mWorkerNameTextView.setText(workerName);
            } else {
                mWorkerNameTextView.setText("Trabajador");
            }

            // Asegurar que tenemos una dirección para mostrar
            if (mAddress != null && !mAddress.isEmpty()) {
                mAddressTextView.setText(mAddress);
            } else {
                mAddressTextView.setText("No especificada");
            }

            // Asegurar que tenemos un tipo de servicio para mostrar
            if (mServiceType != null && !mServiceType.isEmpty()) {
                mServiceTypeTextView.setText(mServiceType);
            } else {
                mServiceTypeTextView.setText("No especificado");
            }

            // Asegurar que exite  un monto para mostrar
            if (amount != null && !amount.isEmpty()) {
                mAmountTextView.setText(amount);
            } else {
                mAmountTextView.setText("A confirmar");
            }
        } else {
            Log.e(TAG, "No se recibieron datos en el Intent");
            // Si no hay datos, mostrar valores por defecto
            setDefaultValues();
        }
    }

    private void setDefaultValues() {
        mWorkerPhotoImageView.setImageResource(R.drawable.workerlogo);
        mWorkerNameTextView.setText("Trabajador");
        mAddressTextView.setText("No especificada");
        mServiceTypeTextView.setText("No especificado");
        mAmountTextView.setText("A confirmar");
    }

    private void setupConfirmButton() {
        mConfirmButton.setOnClickListener(v -> {
            // Al confirmar, enviamos la solicitud
            if (validateData()) {
                submitRequest();
            } else {
                Toast.makeText(this, "Datos de solicitud incompletos", LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private boolean validateData() {
        return mWorkerId != null && !mWorkerId.isEmpty() &&
                mAddress != null && !mAddress.isEmpty() &&
                mDescription != null && !mDescription.isEmpty() &&
                mServiceType != null;
    }

    private void submitRequest() {
        String clientId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> requestData = new HashMap<>();

        requestData.put("client_id", clientId);
        requestData.put("worker_id", mWorkerId);
        requestData.put("address", mAddress);
        requestData.put("description", mDescription);
        requestData.put("service_type", mServiceType);
        requestData.put("status", "pending");
        requestData.put("timestamp", System.currentTimeMillis());

        // Mostrar indicador de progreso
        showProgressDialog("Enviando solicitud...");

        mRequestProvider.createRequest(requestData)
                .addOnSuccessListener(databaseReference -> {
                    // No ocultar el progress dialog aquí, esperar a que se envíe la notificación
                    sendNotificationToWorker(mWorkerId, requestData);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e("VerifyRequest", "Error creating request: ", e);

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
                                hideProgressDialog();

                                if (response.isSuccessful()) {
                                    Log.d("NotificationDebug", "Notificación enviada con éxito");
                                    if (response.body() != null) {
                                        Log.d("NotificationDebug", "FCM Response Success: " + response.body().getSuccess());
                                    }

                                    showToast("Solicitud enviada con éxito");
                                    navigateToServiceCompletion();
                                } else {
                                    Log.e("NotificationDebug", "Error al enviar notificación: " + response.message());
                                    Log.e("NotificationDebug", "Response Code: " + response.code());

                                    // Mostrar detalles del error
                                    if (response.errorBody() != null) {
                                        try {
                                            String errorBody = response.errorBody().string();
                                            Log.e("NotificationDebug", "Error Body: " + errorBody);
                                        } catch (Exception e) {
                                            Log.e("NotificationDebug", "Error reading error body: " + e.getMessage());
                                        }
                                    }

                                    // Analizar tipos de error y mostrar mensaje apropiado al usuario
                                    String userMessage = getNotificationErrorMessage(response.code(), response.errorBody());
                                    showToast(userMessage);

                                    // Navegar de todas formas ya que la solicitud se creó exitosamente
                                    navigateToServiceCompletion();
                                }
                            }

                            private String getNotificationErrorMessage(int responseCode, okhttp3.ResponseBody errorBody) {
                                String userMessage;

                                switch (responseCode) {
                                    case 400:
                                        userMessage = "Solicitud creada exitosamente. Error en el formato de la notificación.";
                                        break;
                                    case 401:
                                        userMessage = "Solicitud creada exitosamente. Error de autenticación con el servicio de notificaciones.";
                                        break;
                                    case 403:
                                        userMessage = "Solicitud creada exitosamente. Sin permisos para enviar notificaciones.";
                                        break;
                                    case 404:
                                        userMessage = "Solicitud creada exitosamente. El trabajador no tiene configuradas las notificaciones.";
                                        break;
                                    case 429:
                                        userMessage = "Solicitud creada exitosamente. Límite de notificaciones alcanzado.";
                                        break;
                                    case 500:
                                    case 502:
                                    case 503:
                                        userMessage = "Solicitud creada exitosamente. Error temporal del servidor de notificaciones.";
                                        break;
                                    default:
                                        // Intentar obtener más información del error body si está disponible
                                        String errorDetails = "";
                                        if (errorBody != null) {
                                            try {
                                                errorDetails = errorBody.string();
                                                // Analizar si contiene información específica sobre el token
                                                if (errorDetails.contains("not_found") || errorDetails.contains("InvalidRegistration")) {
                                                    userMessage = "Solicitud creada exitosamente. El trabajador necesita actualizar su aplicación.";
                                                } else if (errorDetails.contains("MismatchSenderId")) {
                                                    userMessage = "Solicitud creada exitosamente. Error de configuración de notificaciones.";
                                                } else {
                                                    userMessage = "Solicitud creada exitosamente. No se pudo enviar la notificación.";
                                                }
                                            } catch (Exception e) {
                                                Log.e("NotificationDebug", "Error al leer error body: " + e.getMessage());
                                                userMessage = "Solicitud creada exitosamente. No se pudo enviar la notificación.";
                                            }
                                        } else {
                                            userMessage = "Solicitud creada exitosamente. No se pudo enviar la notificación.";
                                        }
                                        break;
                                }

                                return userMessage;
                            }

                            @Override
                            public void onFailure(retrofit2.Call<FCMResponse> call, Throwable t) {
                                hideProgressDialog();

                                Log.e("NotificationDebug", "Error en la llamada: " + t.getMessage());
                                Log.e("NotificationDebug", "Error completo: ", t);

                                // Mostrar mensaje de error de conexión
                                String userMessage = "Solicitud enviada, pero no se pudo notificar al trabajador debido a problemas de conexión.";
                                showToast(userMessage);

                                // Navegar de todas formas ya que la solicitud se creó exitosamente
                                navigateToServiceCompletion();
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

    private void navigateToServiceCompletion() {
        // Crear intent para la siguiente actividad
        Intent intent = new Intent(this, ServiceCompletionActivity.class);

        intent.putExtra("worker_name", mWorkerNameTextView.getText().toString());
        intent.putExtra("service_type", mServiceTypeTextView.getText().toString());

        startActivity(intent);

        finish();
    }

    private void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, LENGTH_SHORT).show();
    }
}