package com.project.ads.activities.client;

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

import com.project.ads.models.FCMBody;
import com.project.ads.models.FCMResponse;
import com.project.ads.providers.RequestProvider;
import com.project.ads.providers.NotificationProvider;
import com.project.ads.providers.TokenProvider;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    // Providers
    private RequestProvider mRequestProvider;
    private NotificationProvider mNotificationProvider;
    private TokenProvider mTokenProvider;
    private DatabaseReference mDatabaseReference;

    // Datos para la solicitud
    private String mWorkerId;
    private String mWorkerName;
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
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
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
            mWorkerName = getIntent().getStringExtra("worker_name");
            mAddress = getIntent().getStringExtra("address");
            mDescription = getIntent().getStringExtra("description");
            mServiceType = getIntent().getStringExtra("service_type");
            mWorkerId = getIntent().getStringExtra("worker_id");
            String amount = getIntent().getStringExtra("amount");

            Log.d(TAG, "Datos recibidos - Worker name: " + mWorkerName +
                    ", Address: " + mAddress +
                    ", Service type: " + mServiceType +
                    ", Worker ID: " + mWorkerId);

            // Configurar las vistas con los datos recibidos
            mWorkerPhotoImageView.setImageResource(workerPhotoResId);

            // Asegurar que existe un nombre para mostrar
            if (mWorkerName != null && !mWorkerName.isEmpty()) {
                mWorkerNameTextView.setText(mWorkerName);
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

            // Asegurar que existe un monto para mostrar
            if (amount != null && !amount.isEmpty()) {
                mAmountTextView.setText(amount);
            } else {
                mAmountTextView.setText("A confirmar");
            }
        } else {
            Log.e(TAG, "No se recibieron datos en el Intent");
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

        showProgressDialog("Preparando solicitud...");

        // PASO 1: Obtener información del cliente
        getUserInfo(mDatabaseReference.child("User").child("Clientes").child(clientId)).addOnSuccessListener(clientInfo -> {

            // PASO 2: Obtener información del trabajador
            getUserInfo(mDatabaseReference.child("User").child("Trabajadores").child(mWorkerId)).addOnSuccessListener(workerInfo -> {

                // PASO 3: Crear la solicitud con todos los datos
                Map<String, Object> requestData = new HashMap<>();

                // Datos del cliente
                requestData.put("client_id", clientId);
                requestData.put("client_name", getValueOrDefault(clientInfo, "name", "Cliente"));
                requestData.put("client_phone", getValueOrDefault(clientInfo, "phone", ""));
                requestData.put("client_email", getValueOrDefault(clientInfo, "email", ""));

                // Datos del trabajador
                requestData.put("worker_id", mWorkerId);
                requestData.put("worker_name", getValueOrDefault(workerInfo, "name", mWorkerName != null ? mWorkerName : "Trabajador"));
                requestData.put("worker_phone", getValueOrDefault(workerInfo, "phone", ""));

                // Datos del servicio
                requestData.put("address", mAddress);
                requestData.put("description", mDescription);
                requestData.put("service_type", mServiceType);
                requestData.put("status", "pending");
                requestData.put("timestamp", System.currentTimeMillis());
                requestData.put("last_updated", System.currentTimeMillis());

                // Valores iniciales para tracking
                requestData.put("started_trip_at", 0);
                requestData.put("started_work_at", 0);
                requestData.put("completed_at", 0);
                requestData.put("cancelled_at", 0);
                requestData.put("rating", 0.0);
                requestData.put("rating_comment", "");

                Log.d(TAG, "Solicitud preparada para el trabajador: " + mWorkerId);

                // PASO 4: Crear la solicitud en Firebase
                mProgressDialog.setMessage("Enviando solicitud...");

                mRequestProvider.createRequest(requestData)
                        .addOnSuccessListener(databaseReference -> {
                            String requestId = databaseReference.getKey();
                            Log.d(TAG, "Solicitud creada con ID: " + requestId);

                            // PASO 5: Enviar notificación al trabajador
                            sendNotificationToWorker(mWorkerId, requestData, requestId);
                        })
                        .addOnFailureListener(e -> {
                            hideProgressDialog();
                            Log.e(TAG, "Error creating request: ", e);
                            showToast("Error al crear la solicitud");
                        });

            }).addOnFailureListener(e -> {
                hideProgressDialog();
                Log.e(TAG, "Error obteniendo info del trabajador", e);
                showToast("Error al obtener información del trabajador");
            });

        }).addOnFailureListener(e -> {
            hideProgressDialog();
            Log.e(TAG, "Error obteniendo info del cliente", e);
            showToast("Error al obtener tu información");
        });
    }

    /**
     * Obtiene la información de un usuario desde Firebase
     * (User/Clientes/<uid> o User/Trabajadores/<uid>)
     */
    private Task<Map<String, Object>> getUserInfo(DatabaseReference userRef) {
        TaskCompletionSource<Map<String, Object>> taskSource = new TaskCompletionSource<>();
        String userId = userRef.getKey();

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Map<String, Object> userInfo = new HashMap<>();

                            // Extraer datos del snapshot
                            for (DataSnapshot child : snapshot.getChildren()) {
                                userInfo.put(child.getKey(), child.getValue());
                            }

                            Log.d(TAG, "Info de usuario obtenida: " + userId + " (" + userInfo.size() + " campos)");
                            taskSource.setResult(userInfo);
                        } else {
                            Log.w(TAG, "Usuario no encontrado: " + userId);
                            // Devolver mapa vacío en lugar de error
                            taskSource.setResult(new HashMap<>());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error obteniendo usuario: " + error.getMessage());
                        taskSource.setException(error.toException());
                    }
                });

        return taskSource.getTask();
    }

    /**
     * Obtiene un valor del mapa o retorna un valor por defecto
     */
    private String getValueOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    private void sendNotificationToWorker(String workerId, Map<String, Object> requestData, String requestId) {
        Log.d(TAG, "Enviando notificación al trabajador: " + workerId);

        DatabaseReference tokenRef = mTokenProvider.getToken(workerId);

        tokenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.child("token").exists()) {
                    String workerToken = dataSnapshot.child("token").getValue(String.class);

                    if (workerToken != null && !workerToken.isEmpty()) {
                        Log.d(TAG, "Token del trabajador: " + workerToken);

                        String title = "Nueva Solicitud de Servicio";
                        String body = "Tipo: " + requestData.get("service_type") +
                                "\nDirección: " + requestData.get("address");

                        Map<String, String> notificationData = new HashMap<>();
                        notificationData.put("title", title);
                        notificationData.put("body", body);
                        notificationData.put("clientId", (String) requestData.get("client_id"));
                        notificationData.put("requestId", requestId);
                        notificationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

                        FCMBody fcmBody = new FCMBody(workerToken, "high", notificationData);

                        mNotificationProvider.sendNotification(fcmBody).enqueue(new retrofit2.Callback<FCMResponse>() {
                            @Override
                            public void onResponse(retrofit2.Call<FCMResponse> call, retrofit2.Response<FCMResponse> response) {
                                hideProgressDialog();

                                if (response.isSuccessful()) {
                                    Log.d(TAG, "Notificación enviada con éxito");
                                    showToast("Solicitud enviada con éxito");
                                } else {
                                    Log.e(TAG, "Error al enviar notificación: " + response.message());
                                    showToast("Solicitud creada exitosamente");
                                }

                                navigateToRequestDetail(requestId);
                            }

                            @Override
                            public void onFailure(retrofit2.Call<FCMResponse> call, Throwable t) {
                                hideProgressDialog();
                                Log.e(TAG, "Error en la llamada: " + t.getMessage());
                                showToast("Solicitud enviada");
                                navigateToRequestDetail(requestId);
                            }
                        });
                    } else {
                        hideProgressDialog();
                        Log.e(TAG, "Token del trabajador es nulo o vacío");
                        showToast("Solicitud creada exitosamente");
                        navigateToRequestDetail(requestId);
                    }
                } else {
                    hideProgressDialog();
                    Log.e(TAG, "No se encontró el token del trabajador");
                    showToast("Solicitud creada exitosamente");
                    navigateToRequestDetail(requestId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                hideProgressDialog();
                Log.e(TAG, "Error al leer token: " + databaseError.getMessage());
                showToast("Solicitud creada exitosamente");
                navigateToRequestDetail(requestId);
            }
        });
    }

    /**
     * Navega directamente a los detalles de la solicitud recién creada
     */
    private void navigateToRequestDetail(String requestId) {
        Intent intent = new Intent(this, ServiceRequestDetailActivity.class);
        intent.putExtra("request_id", requestId);

        // Limpiar el back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
        finish();

        Toast.makeText(this, "Solicitud creada. Puedes ver el progreso aquí.",
                Toast.LENGTH_LONG).show();
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