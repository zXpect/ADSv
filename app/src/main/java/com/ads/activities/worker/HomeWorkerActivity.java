package com.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ads.activities.worker.fixdepot.FixDepotActivity;
import com.ads.helpers.LogoutHelper;
import com.ads.models.Worker;
import com.ads.providers.AuthProvider;
import com.ads.providers.GeofireProvider;
import com.ads.providers.TokenProvider;
import com.ads.providers.WorkerDocumentProvider;
import com.ads.providers.WorkerProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;

public class HomeWorkerActivity extends AppCompatActivity {

    private static final String TAG = "HomeWorkerActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 30000;
    private static final long LOCATION_FASTEST_INTERVAL = 15000;

    // Componentes UI
    MaterialButton mButtonConnect;
    MaterialButton mButtonDisconnect;
    LinearLayout mButtonViewMap2;
    LinearLayout mButtonFixDepot;
    View mConnectionIndicator;
    View mConnectionIndicatorGlow;
    TextView mTextConnectionStatus;
    TextView mTextConnectionMessage;
    TextView mTextRating;
    TextView mTextCompletedServices;
    TextView mTextMonthlyEarnings;
    TextView mTextActiveServices;
    TextView mTextActiveServicesCount;
    TextView mTextPendingRequests;
    TextView mTextWorkerName;
    TextView mTextWorkerType;
    ImageView mImageWorkerProfile;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    AuthProvider mAuthProvider;
    WorkerProvider mWorkerProvider;
    TokenProvider mTokenProvider;
    Toolbar mToolbar;
    private FusedLocationProviderClient mFusedLocation;
    private GeofireProvider mGeofireProvider;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    private boolean isConnected = false;
    private Worker mCurrentWorker;
    private LatLng mCurrentLatLng;
    private Handler mUIUpdateHandler;
    private ValueEventListener mWorkerDataListener;
    private LinearLayout mLayoutPendingRequests;
    private ValueAnimator mPulseAnimator;

    private String mVerificationStatus = null;
    private boolean isVerified = false;
    private boolean isVerificationChecked = false;
    private WorkerDocumentProvider mDocumentProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_worker);

        initProviders();
        initViews();
        setupNavigationDrawer();
        setupClickListeners();
        setupStatusBar();
        setupLocationCallback();
        generateToken();


        loadWorkerData();

        mUIUpdateHandler = new Handler(Looper.getMainLooper());
        startPeriodicUIUpdates();

        // Animación de entrada (con pequeño delay para que se carguen datos primero)
        new Handler(Looper.getMainLooper()).postDelayed(this::animateEntranceEffects, 300);
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mWorkerProvider = new WorkerProvider();
        mGeofireProvider = new GeofireProvider();
        mTokenProvider = new TokenProvider();
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
        mDocumentProvider = new WorkerDocumentProvider();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        mToolbar = findViewById(R.id.toolbar);

        mButtonViewMap2 = findViewById(R.id.vermapaworker);
        mButtonFixDepot = findViewById(R.id.buttonFixDepot);

        mButtonConnect = findViewById(R.id.buttonConnect);
        mButtonDisconnect = findViewById(R.id.buttonDisconnect);
        mConnectionIndicator = findViewById(R.id.connectionIndicator);
        mConnectionIndicatorGlow = findViewById(R.id.connectionIndicatorGlow);
        mTextConnectionStatus = findViewById(R.id.textConnectionStatus);
        mTextConnectionMessage = findViewById(R.id.textConnectionMessage);

        mTextWorkerName = findViewById(R.id.textWorkerName);
        mTextWorkerType = findViewById(R.id.textWorkerType);
        mImageWorkerProfile = findViewById(R.id.imageWorkerProfile);

        mTextRating = findViewById(R.id.textRating);
        mTextCompletedServices = findViewById(R.id.textCompletedServices);
        mTextMonthlyEarnings = findViewById(R.id.textMonthlyEarnings);

        mTextActiveServices = findViewById(R.id.textActiveServices);
        mTextActiveServicesCount = findViewById(R.id.textActiveServicesCount);
        mTextPendingRequests = findViewById(R.id.textPendingRequests);
        mLayoutPendingRequests = findViewById(R.id.layoutPendingRequests);

        // Inicializar en estado deshabilitado por defecto
        mButtonConnect.setEnabled(false);
        mButtonConnect.setAlpha(0.5f);
        mButtonDisconnect.setEnabled(false);
        mButtonDisconnect.setAlpha(0.5f);
    }

    private void setupNavigationDrawer() {
        setSupportActionBar(mToolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, mToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Toast.makeText(HomeWorkerActivity.this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_profile) {
                openProfile();
            } else if (id == R.id.nav_settings) {
                openSettings();
            } else if (id == R.id.nav_faq) {
                openFAQ();
            } else if (id == R.id.action_logout) {
                logout();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void openFAQ() {
        Toast.makeText(this, "Función de FAQ en desarrollo", Toast.LENGTH_SHORT).show();
    }
    private void setupClickListeners() {
        mLayoutPendingRequests.setOnClickListener(v -> {
            animateClick(v);
            viewPendingRequests();
        });

        mButtonViewMap2.setOnClickListener(v -> {
            animateClick(v);
            viewMap();
        });

        mButtonFixDepot.setOnClickListener(v -> {
            animateClick(v);
            goToFixDepot();
        });

        mButtonConnect.setOnClickListener(v -> connectWorker());
        mButtonDisconnect.setOnClickListener(v -> disconnectWorker());
    }

    private void animateClick(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void animateEntranceEffects() {
        View[] views = {
                findViewById(R.id.cardConnectionStatus),
                findViewById(R.id.cardPerformance),
                findViewById(R.id.cardPendingRequests)
        };

        for (int i = 0; i < views.length; i++) {
            final View view = views[i];
            if (view != null) {
                view.setAlpha(0f);
                view.setTranslationY(50f);

                view.postDelayed(() -> {
                    view.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(400)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                }, i * 100L);
            }
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
    }

    private void setupLocationCallback() {
        mLocationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(10);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    if (location != null && isConnected) {
                        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        updateLocationInFirebase();
                    }
                }
            }
        };
    }

    private void loadWorkerData() {
        if (mAuthProvider.getId() != null) {
            mWorkerDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        mCurrentWorker = snapshot.getValue(Worker.class);
                        if (mCurrentWorker != null) {
                            updateWorkerInfoUI();
                            updatePerformanceIndicators();
                        }

                        // PRIMERO: Verificar estado de documentos
                        checkVerificationStatus(snapshot);

                        // SEGUNDO: Marcar como verificado solo si está aprobado
                        if (!isVerificationChecked) {
                            isVerificationChecked = true;

                            // TERCERO: Actualizar UI según el estado
                            runOnUiThread(() -> {
                                if (isVerified) {
                                    // Si está aprobado, habilitar normalmente
                                    setWorkerOnlineStatus(true);
                                    updateConnectionStatusForVerified();
                                } else {
                                    // Si NO está aprobado, mostrar estado de verificación pendiente
                                    // Y asegurar que isAvailable sea false
                                    updateWorkerAvailabilityStatus(false);
                                    updateConnectionStatusForUnverified();
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error al cargar datos del trabajador", error.toException());
                    showSnackbar("Error al cargar datos del perfil");
                }
            };

            mWorkerProvider.listenForWorkerChanges(mAuthProvider.getId(), mWorkerDataListener);
        }
    }

    private void updateWorkerInfoUI() {
        if (mCurrentWorker != null) {
            runOnUiThread(() -> {
                mTextWorkerName.setText(mCurrentWorker.getFullName());
                mTextWorkerType.setText(mCurrentWorker.getWork() != null ?
                        mCurrentWorker.getWork() : "Sin especialidad");
            });
        }
    }

    private void checkVerificationStatus(DataSnapshot snapshot) {
        try {
            DataSnapshot verificationSnapshot = snapshot.child("verificationStatus");

            if (verificationSnapshot.exists() && verificationSnapshot.child("status").exists()) {
                mVerificationStatus = verificationSnapshot.child("status").getValue(String.class);
                isVerified = "approved".equalsIgnoreCase(mVerificationStatus);

                Log.d(TAG, "Estado de verificación: " + mVerificationStatus + ", isVerified: " + isVerified);
            } else {
                isVerified = false;
                mVerificationStatus = "incomplete";
                Log.w(TAG, "Worker sin estado de verificación");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al verificar estado de documentos", e);
            isVerified = false;
            mVerificationStatus = "error";
        }
    }

    private void updateConnectionStatusForVerified() {
        // Usuario APROBADO: Botones funcionan normalmente
        mTextConnectionStatus.setText("Desconectado");
        mTextConnectionStatus.setTextColor(getResources().getColor(R.color.error_color, getTheme()));
        mTextConnectionMessage.setText("Conéctate para recibir solicitudes de trabajo cercanas");

        mConnectionIndicator.setBackgroundResource(R.drawable.circle_red);
        if (mConnectionIndicatorGlow != null) {
            mConnectionIndicatorGlow.setBackgroundResource(R.drawable.circle_glow_red);
        }

        // Habilitar botón de conectar
        mButtonConnect.setEnabled(true);
        mButtonConnect.setAlpha(1.0f);
        mButtonDisconnect.setEnabled(false);
        mButtonDisconnect.setAlpha(0.5f);
    }

    private void updateConnectionStatusForUnverified() {
        // Usuario NO APROBADO: Mostrar estado de verificación pendiente
        String message = getVerificationMessage();

        mTextConnectionStatus.setText("Verificación Pendiente");
        mTextConnectionStatus.setTextColor(getResources().getColor(R.color.warning_color, getTheme()));
        mTextConnectionMessage.setText(message);
        mTextConnectionMessage.setTextColor(getResources().getColor(R.color.warning_color, getTheme()));

        // Indicador amarillo
        mConnectionIndicator.setBackgroundResource(R.drawable.circle_yellow);
        if (mConnectionIndicatorGlow != null) {
            mConnectionIndicatorGlow.setBackgroundResource(R.drawable.circle_glow_yellow);
        }

        // Deshabilitar AMBOS botones
        mButtonConnect.setEnabled(false);
        mButtonConnect.setAlpha(0.5f);
        mButtonDisconnect.setEnabled(false);
        mButtonDisconnect.setAlpha(0.5f);

        // Si por alguna razón estaba conectado, desconectarlo
        if (isConnected) {
            disconnectWorker();
        }
    }

    private String getVerificationMessage() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return "📄 Debes completar tu perfil y subir los documentos requeridos para poder conectarte.";
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return "⏳ Tus documentos están en proceso de verificación. Podrás conectarte cuando sean aprobados.";

            case "rejected":
                return "❌ Tus documentos fueron rechazados. Por favor, actualiza tu información en tu perfil.";

            case "incomplete":
                return "📋 Debes completar la verificación de documentos para poder conectarte.";

            case "approved":
                return "✅ Tus documentos han sido verificados. Ya puedes conectarte.";

            default:
                return "⚠️ Verificación de documentos pendiente. Contacta con soporte si el problema persiste.";
        }
    }

    private void connectWorker() {
        // VALIDACIÓN CRÍTICA: Verificar si está aprobado
        if (!isVerified) {
            showErrorToast("No puedes conectarte hasta que tus documentos sean verificados");

            // Mostrar diálogo informativo
            new AlertDialog.Builder(this)
                    .setTitle("Verificación de Documentos Pendiente")
                    .setMessage(getVerificationMessage() +
                            "\n\n¿Deseas revisar el estado de tus documentos?")
                    .setPositiveButton("Ver Documentos", (dialog, which) -> {
                        openDocumentsStatus();
                    })
                    .setNegativeButton("Entendido", null)
                    .show();

            return; // SALIR DEL MÉTODO
        }

        if (checkLocationPermissions()) {
            try {
                startLocationUpdates();
                isConnected = true;

                // Cambiar disponibilidad a true
                updateWorkerAvailabilityStatus(true);
                setWorkerOnlineStatus(true);

                updateConnectionStatus();
                showSuccessToast("¡Conectado exitosamente!");
                startPulseAnimation();

                // Configurar desconexión automática
                if (mAuthProvider.getId() != null) {
                    DatabaseReference workerRef = FirebaseDatabase.getInstance()
                            .getReference("User/Trabajadores/" + mAuthProvider.getId());

                    workerRef.child("isOnline").onDisconnect().setValue(false);
                    workerRef.child("isAvailable").onDisconnect().setValue(false);

                    DatabaseReference activeWorkerRef = FirebaseDatabase.getInstance()
                            .getReference("active_workers/" + mAuthProvider.getId());
                    activeWorkerRef.onDisconnect().removeValue();
                }

                Log.d(TAG, "Worker conectado - Disponible para trabajos");

            } catch (Exception e) {
                Log.e(TAG, "Error al conectar worker", e);
                showErrorToast("Error al conectar");
            }
        } else {
            requestLocationPermissions();
        }
    }

    private void openDocumentsStatus() {
        try {
            Intent intent = new Intent(HomeWorkerActivity.this, DocumentStatusActivity.class);
            intent.putExtra("worker_id", mAuthProvider.getId());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir documentos", e);
            showErrorToast("Error al abrir pantalla de documentos");
        }
    }

    private void disconnectWorker() {
        try {
            stopLocationUpdates();
            isConnected = false;

            // Cambiar disponibilidad a false
            updateWorkerAvailabilityStatus(false);
            setWorkerOnlineStatus(true); // Sigue online, solo no disponible

            updateConnectionStatus();
            showInfoToast("Desconectado");
            stopPulseAnimation();

            Log.d(TAG, "Worker desconectado - No disponible para trabajos");

        } catch (Exception e) {
            Log.e(TAG, "Error al desconectar worker", e);
            showErrorToast("Error al desconectar");
        }
    }

    private void startPulseAnimation() {
        if (mPulseAnimator != null && mPulseAnimator.isRunning()) {
            mPulseAnimator.cancel();
        }

        mPulseAnimator = ValueAnimator.ofFloat(0.3f, 0.8f);
        mPulseAnimator.setDuration(1500);
        mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mPulseAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            if (mConnectionIndicatorGlow != null) {
                mConnectionIndicatorGlow.setAlpha(alpha);
            }
        });
        mPulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (mPulseAnimator != null && mPulseAnimator.isRunning()) {
            mPulseAnimator.cancel();
        }
        if (mConnectionIndicatorGlow != null) {
            mConnectionIndicatorGlow.setAlpha(0.3f);
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    private void stopLocationUpdates() {
        if (mFusedLocation != null && mLocationCallback != null) {
            mFusedLocation.removeLocationUpdates(mLocationCallback);

            if (!isConnected && mAuthProvider.getId() != null) {
                mGeofireProvider.removeLocation(mAuthProvider.getId());
            }
        }
    }

    private void updateLocationInFirebase() {
        if (mCurrentLatLng != null && mAuthProvider.getId() != null && isConnected) {
            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);

            mWorkerProvider.updateWorkerLocation(
                    mAuthProvider.getId(),
                    mCurrentLatLng.latitude,
                    mCurrentLatLng.longitude
            ).addOnFailureListener(e ->
                    Log.e(TAG, "Error al actualizar ubicación en perfil", e)
            );
        }
    }

    private void setWorkerOnlineStatus(boolean isOnline) {
        if (mAuthProvider.getId() != null) {
            mWorkerProvider.updateWorkerOnlineStatus(mAuthProvider.getId(), isOnline)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error al actualizar estado online", e)
                    );
        }
    }

    private void updateWorkerAvailabilityStatus(boolean isAvailable) {
        if (mAuthProvider.getId() != null) {
            mWorkerProvider.updateWorkerAvailability(mAuthProvider.getId(), isAvailable)
                    .addOnSuccessListener(aVoid -> {
                        if (isAvailable && mCurrentLatLng != null) {
                            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);
                            Log.d(TAG, "Trabajador agregado a active_workers - isAvailable: true");
                        } else if (!isAvailable) {
                            mGeofireProvider.removeLocation(mAuthProvider.getId());
                            Log.d(TAG, "Trabajador removido de active_workers - isAvailable: false");
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error al actualizar disponibilidad", e));
        }
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permisos de ubicación requeridos")
                    .setMessage("Esta aplicación necesita acceso a tu ubicación para conectarte con clientes cercanos")
                    .setPositiveButton("Conceder", (dialog, which) ->
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_REQUEST_CODE))
                    .setNegativeButton("Cancelar", null)
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectWorker();
            } else {
                showErrorToast("Permisos de ubicación necesarios");
            }
        }
    }

    private void updateConnectionStatus() {
        runOnUiThread(() -> {
            // Si no está verificado, mostrar estado de verificación
            if (!isVerified) {
                updateConnectionStatusForUnverified();
                return;
            }

            // Si está verificado, actualizar según conexión
            if (isConnected) {
                mTextConnectionStatus.setText("Conectado");
                mTextConnectionStatus.setTextColor(getResources().getColor(R.color.success_color, getTheme()));
                mTextConnectionMessage.setText("Estás visible para recibir solicitudes de trabajo");
                mConnectionIndicator.setBackgroundResource(R.drawable.circle_green);
                mConnectionIndicatorGlow.setBackgroundResource(R.drawable.circle_glow_green);
                mButtonConnect.setEnabled(false);
                mButtonDisconnect.setEnabled(true);

                animateButtonTransition(mButtonConnect, false);
                animateButtonTransition(mButtonDisconnect, true);
            } else {
                mTextConnectionStatus.setText("Desconectado");
                mTextConnectionStatus.setTextColor(getResources().getColor(R.color.error_color, getTheme()));
                mTextConnectionMessage.setText("Conéctate para recibir solicitudes de trabajo cercanas");
                mConnectionIndicator.setBackgroundResource(R.drawable.circle_red);
                mConnectionIndicatorGlow.setBackgroundResource(R.drawable.circle_glow_red);
                mButtonConnect.setEnabled(true);
                mButtonDisconnect.setEnabled(false);

                animateButtonTransition(mButtonConnect, true);
                animateButtonTransition(mButtonDisconnect, false);
            }
        });
    }

    private void animateButtonTransition(View button, boolean enabled) {
        button.animate()
                .alpha(enabled ? 1f : 0.5f)
                .scaleX(enabled ? 1f : 0.95f)
                .scaleY(enabled ? 1f : 0.95f)
                .setDuration(300)
                .start();
    }

    private void updatePerformanceIndicators() {
        if (mCurrentWorker != null) {
            runOnUiThread(() -> {
                animateNumberChange(mTextRating, mCurrentWorker.getFormattedRating());
                animateNumberChange(mTextCompletedServices, String.valueOf(mCurrentWorker.getTotalRatings()));

                double estimatedEarnings = mCurrentWorker.getPricePerHour() * mCurrentWorker.getTotalRatings() * 2;
                animateNumberChange(mTextMonthlyEarnings, "$" + String.format("%.0f", estimatedEarnings));
            });
        }
    }

    private void animateNumberChange(TextView textView, String newValue) {
        textView.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction(() -> {
                    textView.setText(newValue);
                    textView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void updateServicesStatus() {
        if (mAuthProvider == null || mAuthProvider.getId() == null) {
            runOnUiThread(() -> {
                if (mTextActiveServices != null) {
                    mTextActiveServices.setText("Error: Usuario no autenticado");
                }
                if (mTextPendingRequests != null) {
                    mTextPendingRequests.setText("Error: Usuario no autenticado");
                }
            });
            return;
        }

        String workerId = mAuthProvider.getId();

        FirebaseDatabase.getInstance()
                .getReference()
                .child("requests")
                .orderByChild("worker_id")
                .equalTo(workerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int pendingCount = 0;
                        int activeCount = 0;

                        if (snapshot.exists()) {
                            for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                                String status = requestSnapshot.child("status").getValue(String.class);

                                if (status != null) {
                                    switch (status.toLowerCase()) {
                                        case "pending":
                                            pendingCount++;
                                            break;
                                        case "accepted":
                                        case "in_progress":
                                            activeCount++;
                                            break;
                                    }
                                }
                            }
                        }

                        final int finalPendingCount = pendingCount;
                        final int finalActiveCount = activeCount;

                        runOnUiThread(() -> updateServicesUI(finalPendingCount, finalActiveCount));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar estado de servicios", error.toException());
                        runOnUiThread(() -> {
                            if (mTextActiveServices != null) {
                                mTextActiveServices.setText("Error al cargar");
                            }
                            if (mTextPendingRequests != null) {
                                mTextPendingRequests.setText("Error al cargar");
                            }
                        });
                    }
                });
    }

    private void updateServicesUI(int pendingCount, int activeCount) {
        try {
            if (mTextActiveServices != null && mTextActiveServicesCount != null) {
                if (activeCount > 0) {
                    mTextActiveServices.setText(activeCount + " servicio(s) en curso");
                    mTextActiveServicesCount.setText(String.valueOf(activeCount));
                } else {
                    mTextActiveServices.setText("No hay servicios activos");
                    mTextActiveServicesCount.setText("0");
                }
            }

            if (mTextPendingRequests != null && mLayoutPendingRequests != null) {
                if (pendingCount > 0) {
                    mTextPendingRequests.setText(pendingCount + " nueva(s) - Toca para ver");
                    mLayoutPendingRequests.setEnabled(true);
                    mLayoutPendingRequests.setAlpha(1.0f);

                    animateAttention(mLayoutPendingRequests);
                } else {
                    mTextPendingRequests.setText("No hay solicitudes pendientes");
                    mLayoutPendingRequests.setEnabled(false);
                    mLayoutPendingRequests.setAlpha(0.7f);
                }
            }

            Log.d(TAG, "UI actualizada - Activos: " + activeCount + ", Pendientes: " + pendingCount);

        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar UI de servicios", e);
        }
    }

    private void animateAttention(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.03f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.03f, 1f);
        scaleX.setDuration(600);
        scaleY.setDuration(600);
        scaleX.setRepeatCount(2);
        scaleY.setRepeatCount(2);
        scaleX.start();
        scaleY.start();
    }

    private void startPeriodicUIUpdates() {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateServicesStatus();
                mUIUpdateHandler.postDelayed(this, 30000);
            }
        };
        mUIUpdateHandler.post(updateRunnable);
    }

    private void viewPendingRequests() {
        if (mAuthProvider == null || mAuthProvider.getId() == null) {
            showErrorToast("Error: Usuario no autenticado");
            return;
        }

        String currentText = mTextPendingRequests.getText().toString();
        if (currentText.contains("No hay") || currentText.contains("Error")) {
            showInfoToast("No hay solicitudes pendientes");
            return;
        }

        try {
            Intent intent = new Intent(HomeWorkerActivity.this, PendingRequestsActivity.class);
            intent.putExtra("worker_id", mAuthProvider.getId());
            intent.putExtra("worker_type", mCurrentWorker != null ? mCurrentWorker.getWork() : "");
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir solicitudes pendientes", e);
            showErrorToast("Error al abrir solicitudes");
        }
    }

    private void openProfile() {
        try {
            Intent intent = new Intent(this, ProfileWorkerActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir perfil", e);
            showErrorToast("Error al abrir perfil");
        }
    }

    private void openSettings() {
        showInfoToast("Función de configuración en desarrollo");
    }

    private void viewMap() {
        Intent intent = new Intent(HomeWorkerActivity.this, MapWorkerActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void goToFixDepot() {
        Intent intent = new Intent(HomeWorkerActivity.this, FixDepotActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void generateToken() {
        if (mAuthProvider.getId() != null) {
            mTokenProvider.create(mAuthProvider.getId());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.worker_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro que deseas cerrar sesión?")
                    .setCancelable(false);

            builder.setPositiveButton("Sí", (dialog, which) -> {
                try {
                    if (isConnected) {
                        disconnectWorker();
                    }

                    setWorkerOnlineStatus(false);

                    LogoutHelper.performLogout(this, mAuthProvider, null, null);
                } catch (Exception e) {
                    Log.e(TAG, "Error durante logout", e);
                    showErrorToast("Error al cerrar sesión");
                }
            });

            builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

            runOnUiThread(() -> {
                AlertDialog dialog = builder.create();
                if (!isFinishing()) {
                    dialog.show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar diálogo de logout", e);
            showErrorToast("Error al mostrar diálogo");
        }
    }

    private void showSuccessToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showInfoToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSnackbar(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isVerified) {
            setWorkerOnlineStatus(true);
        }

        updateConnectionStatus();
        updatePerformanceIndicators();
        updateServicesStatus();

        if (isConnected) {
            startPulseAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPulseAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopPulseAnimation();

        if (mWorkerDataListener != null && mAuthProvider.getId() != null) {
            mWorkerProvider.removeWorkerListener(mAuthProvider.getId(), mWorkerDataListener);
        }

        if (mUIUpdateHandler != null) {
            mUIUpdateHandler.removeCallbacksAndMessages(null);
        }

        if (isFinishing()) {
            setWorkerOnlineStatus(false);

            try {
                stopLocationUpdates();
                updateWorkerAvailabilityStatus(false);
                isConnected = false;
            } catch (Exception e) {
                Log.e(TAG, "Error durante destrucción", e);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Salir de la aplicación")
                    .setMessage("¿Deseas salir?" + (isConnected ? " Te desconectarás automáticamente." : ""))
                    .setPositiveButton("Salir", (dialog, which) -> {
                        if (isConnected) {
                            disconnectWorker();
                        }
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }
}