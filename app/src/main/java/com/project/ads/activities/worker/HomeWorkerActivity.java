package com.project.ads.activities.worker;

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
import android.graphics.drawable.Drawable;
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

import com.project.ads.helpers.LogoutHelper;
import com.project.ads.helpers.WorkerNavHelper;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.Worker;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.GeofireProvider;
import com.project.ads.providers.TokenProvider;
import com.project.ads.providers.WorkerDocumentProvider;
import com.project.ads.providers.WorkerProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

public class HomeWorkerActivity extends AppCompatActivity {

    private static final String TAG = "HomeWorkerActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 30000;
    private static final long LOCATION_FASTEST_INTERVAL = 15000;
    private static final int ICON_SIZE_DP = 20; // Tamaño del icono en dp
    private static final int ICON_PADDING_DP = 8; // Padding entre icono y texto

    // Componentes UI
    MaterialSwitch mButtonToggleConnection; // Interruptor Disponible (mockup 13)
    LinearLayout mButtonViewMap2;
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
    private LinearLayout mLayoutActiveServices;

    private String mVerificationStatus = null;
    private boolean isVerified = false;
    private boolean isVerificationChecked = false;
    private WorkerDocumentProvider mDocumentProvider;

    private ImageView mNavHeaderProfileImage;
    private TextView mNavHeaderName;
    private TextView mNavHeaderEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_worker);

        initProviders();
        initViews();
        setupClickListeners();
        setupLocationCallback();
        WorkerNavHelper.setup(this, R.id.nav_worker_home);
        generateToken();

        loadWorkerData();

        mUIUpdateHandler = new Handler(Looper.getMainLooper());
        startPeriodicUIUpdates();

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
        mButtonViewMap2 = findViewById(R.id.vermapaworker);

        // BOTÓN ÚNICO
        mButtonToggleConnection = findViewById(R.id.buttonToggleConnection);

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
        mLayoutActiveServices = findViewById(R.id.layoutActiveServices);
        mTextPendingRequests = findViewById(R.id.textPendingRequests);
        mLayoutPendingRequests = findViewById(R.id.layoutPendingRequests);

        // Inicializar en estado deshabilitado por defecto
        mButtonToggleConnection.setEnabled(false);
        mButtonToggleConnection.setAlpha(0.5f);
    }
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void openFAQ() {
        Toast.makeText(this, "Función de FAQ en desarrollo", Toast.LENGTH_SHORT).show();
    }

    private void setupClickListeners() {
        mLayoutPendingRequests.setOnClickListener(v -> {
            animateClick(v);
            viewPendingRequests();
        });

        mLayoutActiveServices.setOnClickListener(v -> {
            animateClick(v);
            viewActiveServices();
        });

        mButtonViewMap2.setOnClickListener(v -> {
            animateClick(v);
            viewMap();
        });

        // LISTENER DEL BOTÓN ÚNICO
        mButtonToggleConnection.setOnClickListener(v -> {
            toggleConnection();
            // El interruptor muestra el estado real (p. ej. si falta el permiso de ubicación)
            mButtonToggleConnection.setChecked(isConnected);
        });
        findViewById(R.id.card_next_service).setOnClickListener(v -> openNextService());
    }

    // MÉTODO NUEVO: Toggle entre conectar/desconectar
    private void toggleConnection() {
        if (isConnected) {
            disconnectWorker();
        } else {
            connectWorker();
        }
    }

    private void viewActiveServices() {
        if (mAuthProvider == null || mAuthProvider.getId() == null) {
            showErrorToast("Error: Usuario no autenticado");
            return;
        }

        try {
            Intent intent = new Intent(HomeWorkerActivity.this, ActiveServicesActivity.class);
            intent.putExtra("worker_id", mAuthProvider.getId());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir servicios activos", e);
            showErrorToast("Error al abrir servicios activos");
        }
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

                        // PRIMERO: Verificar el estado
                        checkVerificationStatus(snapshot);

                        // DEBUG: Confirmar qué valores tenemos
                        Log.d(TAG, "📋 Después de checkVerificationStatus:");
                        Log.d(TAG, "   mVerificationStatus = " + mVerificationStatus);
                        Log.d(TAG, "   isVerified = " + isVerified);
                        Log.d(TAG, "   isVerificationChecked = " + isVerificationChecked);

                        if (!isVerificationChecked) {
                            isVerificationChecked = true;

                            runOnUiThread(() -> {
                                if (isVerified) {
                                    Log.d(TAG, "✅ Worker VERIFICADO - Habilitando conexión");
                                    setWorkerOnlineStatus(true);
                                    updateConnectionStatusForVerified();
                                } else {
                                    Log.d(TAG, "⚠️ Worker NO VERIFICADO - Estado: " + mVerificationStatus);
                                    updateWorkerAvailabilityStatus(false);
                                    updateConnectionStatusForUnverified();
                                }
                            });
                        } else {
                            // Si ya se verificó antes, actualizar UI en tiempo real según estado ACTUAL
                            runOnUiThread(() -> {
                                if (isVerified) {
                                    updateConnectionStatus();
                                } else {
                                    updateConnectionStatusForUnverified();
                                }
                            });
                        }
                    } else {
                        Log.e(TAG, "❌ Snapshot del worker no existe");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error al cargar datos del trabajador", error.toException());
                    showSnackbar("Error al cargar datos del perfil");
                }
            };

            mWorkerProvider.listenForWorkerChanges(mAuthProvider.getId(), mWorkerDataListener);
        } else {
            Log.e(TAG, "❌ AuthProvider ID es null");
        }
    }
    private void updateWorkerInfoUI() {
        if (mCurrentWorker != null) {
            runOnUiThread(() -> {
                String fullName = mCurrentWorker.getFullName() != null ? mCurrentWorker.getFullName().trim() : "";
                mTextWorkerName.setText(fullName.isEmpty() ? getString(R.string.home_greeting_default)
                        : getString(R.string.home_greeting, fullName.split(" ")[0]));
                String categories = mCurrentWorker.getCategoriesDisplay();
                mTextWorkerType.setText(categories != null ? categories : getString(R.string.no_specialty));

                loadWorkerProfileImage();
                updateNavigationDrawerHeader();
            });
        }
    }

    private void updateNavigationDrawerHeader() {
        if (mCurrentWorker == null) {
            return;
        }

        try {
            if (mNavHeaderName != null) {
                mNavHeaderName.setText(mCurrentWorker.getFullName());
            }

            if (mNavHeaderEmail != null) {
                String email = mCurrentWorker.getEmail();
                mNavHeaderEmail.setText(email != null ? email : "");
            }

            if (mNavHeaderProfileImage != null) {
                RequestOptions options = new RequestOptions()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL);

                if (mCurrentWorker.hasImage()) {
                    String imageUrl = mCurrentWorker.getImage();

                    if (imageUrl != null && !imageUrl.trim().isEmpty() &&
                            !imageUrl.equalsIgnoreCase("default") &&
                            !imageUrl.equalsIgnoreCase("null")) {

                        Glide.with(this)
                                .load(imageUrl)
                                .apply(options)
                                .into(mNavHeaderProfileImage);
                    } else {
                        mNavHeaderProfileImage.setImageResource(R.drawable.ic_person);
                    }
                } else {
                    mNavHeaderProfileImage.setImageResource(R.drawable.ic_person);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar header del drawer", e);
        }
    }

    private void loadWorkerProfileImage() {
        if (mCurrentWorker == null || mImageWorkerProfile == null) {
            return;
        }

        try {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            if (mCurrentWorker.hasImage()) {
                String imageUrl = mCurrentWorker.getImage();

                if (imageUrl != null && !imageUrl.trim().isEmpty() &&
                        !imageUrl.equalsIgnoreCase("default") &&
                        !imageUrl.equalsIgnoreCase("null")) {

                    Log.d(TAG, "Cargando imagen del worker: " + imageUrl);

                    Glide.with(this)
                            .load(imageUrl)
                            .apply(options)
                            .into(mImageWorkerProfile);
                } else {
                    Log.d(TAG, "URL de imagen no válida, usando imagen por defecto");
                    mImageWorkerProfile.setImageResource(R.drawable.ic_person);
                }
            } else {
                Log.d(TAG, "Worker sin imagen, usando imagen por defecto");
                mImageWorkerProfile.setImageResource(R.drawable.ic_person);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar imagen del worker", e);
            mImageWorkerProfile.setImageResource(R.drawable.ic_person);
        }
    }

    private void checkVerificationStatus(DataSnapshot snapshot) {
        try {
            Log.d(TAG, "=== VERIFICANDO ESTADO ===");

            // Ahora el Worker ya tiene el campo verificationStatus mapeado
            if (mCurrentWorker != null && mCurrentWorker.getVerificationStatus() != null) {
                mVerificationStatus = mCurrentWorker.getVerificationStatusString();
                isVerified = mCurrentWorker.isVerified();

                Log.d(TAG, "✅ Usando Worker.verificationStatus");
                Log.d(TAG, "📄 Status: '" + mVerificationStatus + "'");
                Log.d(TAG, "✓ isVerified: " + isVerified);
            }
            // Fallback: intentar leer directamente del snapshot
            else {
                Log.d(TAG, "⚠️ Worker sin verificationStatus, leyendo snapshot...");

                DataSnapshot verificationSnapshot = snapshot.child("verificationStatus");

                if (verificationSnapshot.exists() && verificationSnapshot.child("status").exists()) {
                    mVerificationStatus = verificationSnapshot.child("status").getValue(String.class);

                    if (mVerificationStatus != null) {
                        mVerificationStatus = mVerificationStatus.trim().toLowerCase();
                        isVerified = "approved".equals(mVerificationStatus) ||
                                "verified".equals(mVerificationStatus);

                        Log.d(TAG, "📄 Status del snapshot: '" + mVerificationStatus + "'");
                        Log.d(TAG, "✓ isVerified: " + isVerified);
                    } else {
                        mVerificationStatus = "incomplete";
                        isVerified = false;
                    }
                } else {
                    mVerificationStatus = "incomplete";
                    isVerified = false;
                    Log.w(TAG, "❌ No se encontró verificationStatus en ningún lugar");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al verificar estado", e);
            isVerified = false;
            mVerificationStatus = "incomplete";
        }
    }
    private void updateConnectionStatusForVerified() {

        // ✅ No sobrescribir si ya está conectado
        if (isConnected) {
            return;
        }
        Log.d(TAG, "🎯 Actualizando UI para trabajador VERIFICADO");


        // Estado: no disponible (pero puede conectarse)
        mTextConnectionStatus.setText(R.string.worker_home_status_off);
        mTextConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.ads_on_navy));

        // Mensaje alentador
        mTextConnectionMessage.setText(R.string.worker_home_message_off);
        mTextConnectionMessage.setTextColor(ContextCompat.getColor(this, R.color.ads_on_navy_muted));

        // Limpiar icono del mensaje (no necesita icono cuando está verificado y desconectado)
        mTextConnectionMessage.setCompoundDrawablesRelative(null, null, null, null);

        // Indicador rojo (desconectado)
        mConnectionIndicator.setBackgroundResource(R.drawable.circle_red);
        if (mConnectionIndicatorGlow != null) {
            mConnectionIndicatorGlow.setBackgroundResource(R.drawable.circle_glow_red);
        }

        // Interruptor habilitado y apagado
        mButtonToggleConnection.setEnabled(true);
        mButtonToggleConnection.setAlpha(1.0f);
        mButtonToggleConnection.setChecked(false);

        Log.d(TAG, "✅ UI configurada - Botón HABILITADO para conectar");
    }
    private void updateConnectionStatusForUnverified() {
        Log.d(TAG, "⚠️ Actualizando UI para trabajador NO VERIFICADO - Estado: " + mVerificationStatus);

        // Obtener todos los recursos según el estado actual
        String message = getVerificationMessage();
        int iconRes = getVerificationIcon();
        int iconColor = getVerificationIconColor();
        int textColor = getVerificationTextColor();
        int statusColor = getVerificationStatusColor();
        int indicatorBg = getVerificationIndicatorBackground();
        int indicatorGlow = getVerificationIndicatorGlow();

        // DEBUG: Verificar qué recursos se obtuvieron
        Log.d(TAG, "📌 Recursos obtenidos:");
        Log.d(TAG, "   iconRes: " + getResources().getResourceEntryName(iconRes));
        Log.d(TAG, "   indicatorBg: " + getResources().getResourceEntryName(indicatorBg));
        Log.d(TAG, "   statusColor: " + getResources().getResourceEntryName(statusColor));

        // Actualizar texto de estado según verificación
        String statusText = getVerificationStatusText();
        mTextConnectionStatus.setText(statusText);
        mTextConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.ads_on_navy));

        // Configurar el mensaje con icono (sobre la tarjeta navy)
        mTextConnectionMessage.setText(message);
        mTextConnectionMessage.setTextColor(ContextCompat.getColor(this, R.color.ads_on_navy_muted));
        setTextViewIcon(mTextConnectionMessage, iconRes, R.color.ads_brand);

        // Actualizar indicador visual según estado
        mConnectionIndicator.setBackgroundResource(indicatorBg);
        if (mConnectionIndicatorGlow != null) {
            mConnectionIndicatorGlow.setBackgroundResource(indicatorGlow);
        }

        // Interruptor deshabilitado mientras no esté verificado
        mButtonToggleConnection.setEnabled(false);
        mButtonToggleConnection.setAlpha(0.5f);
        mButtonToggleConnection.setChecked(false);

        // Forzar desconexión si estaba conectado
        if (isConnected) {
            disconnectWorker();
        }

        Log.d(TAG, "❌ UI configurada - Botón DESHABILITADO");
    }

    private String getVerificationStatusText() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return "Perfil incompleto";
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return "En verificación";

            case "rejected":
                return "Documentos rechazados";

            case "incomplete":
                return "Perfil incompleto";

            case "approved":
                return "Verificado ✓";

            default:
                return "Verificación pendiente";
        }
    }
    private int getVerificationStatusColor() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return R.color.warning_color;
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return R.color.info_color;

            case "rejected":
                return R.color.error_color;

            case "incomplete":
                return R.color.warning_color;

            case "approved":
                return R.color.success_color;

            default:
                return R.color.warning_color;
        }
    }
    private int getVerificationTextColor() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return R.color.text_secondary;
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return R.color.text_secondary;

            case "rejected":
                return R.color.error_color;

            case "incomplete":
                return R.color.text_secondary;

            case "approved":
                return R.color.success_color;

            default:
                return R.color.text_secondary;
        }
    }
    private int getVerificationIcon() {
        Log.d(TAG, "🎨 getVerificationIcon() - Estado actual: " + mVerificationStatus);

        if (mVerificationStatus == null) {
            Log.d(TAG, "   → Estado NULL, usando ic_incomplete");
            return R.drawable.ic_incomplete;
        }

        String status = mVerificationStatus.trim().toLowerCase();
        Log.d(TAG, "   → Estado normalizado: '" + status + "'");

        switch (status) {
            case "documents_submitted":
            case "pending":
                Log.d(TAG, "   → Retornando ic_pending");
                return R.drawable.ic_pending;

            case "rejected":
                Log.d(TAG, "   → Retornando ic_rejected");
                return R.drawable.ic_rejected;

            case "incomplete":
                Log.d(TAG, "   → Retornando ic_incomplete");
                return R.drawable.ic_incomplete;

            case "approved":
                Log.d(TAG, "   → Retornando ic_approved ✓");
                return R.drawable.ic_approved;

            default:
                Log.d(TAG, "   → Estado desconocido, usando ic_warning");
                return R.drawable.ic_warning;
        }
    }
    private void setTextViewIcon(TextView textView, int iconRes, int colorRes) {
        try {
            Drawable icon = ContextCompat.getDrawable(this, iconRes);

            if (icon != null) {
                icon.setTint(ContextCompat.getColor(this, colorRes));
                int iconSize = dpToPx(ICON_SIZE_DP);
                icon.setBounds(0, 0, iconSize, iconSize);
                textView.setCompoundDrawablesRelative(icon, null, null, null);
                textView.setCompoundDrawablePadding(dpToPx(ICON_PADDING_DP));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al configurar icono del TextView", e);
        }
    }private void connectWorker() {
        if (!isVerified) {
            showErrorToast("No puedes conectarte hasta que tus documentos sean verificados");

            // Crear vista personalizada para el AlertDialog
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_verification_status, null);

            ImageView iconView = dialogView.findViewById(R.id.dialogIcon);
            TextView messageView = dialogView.findViewById(R.id.dialogMessage);
            androidx.cardview.widget.CardView iconBackground = dialogView.findViewById(R.id.dialogIconBackground);

            // Obtener el icono, color y fondo según el estado
            int iconRes = getVerificationIcon();
            int iconColor = getVerificationIconColor();
            int backgroundColor = getVerificationBackgroundColor();

            // Configurar el icono con su color
            iconView.setImageResource(iconRes);
            iconView.setColorFilter(ContextCompat.getColor(this, iconColor));

            // Configurar el fondo del círculo del icono
            iconBackground.setCardBackgroundColor(ContextCompat.getColor(this, backgroundColor));

            // Configurar el mensaje
            messageView.setText(getVerificationMessage());

            new AlertDialog.Builder(this)
                    .setTitle("Verificación de Documentos Pendiente")
                    .setView(dialogView)
                    .setPositiveButton("Ver Documentos", (dialog, which) -> {
                        openDocumentsStatus();
                    })
                    .setNegativeButton("Entendido", null)
                    .show();

            return;
        }

        if (checkLocationPermissions()) {
            try {
                startLocationUpdates();
                isConnected = true;

                updateWorkerAvailabilityStatus(true);
                setWorkerOnlineStatus(true);

                updateConnectionStatus();
                showSuccessToast("¡Conectado exitosamente!");
                startPulseAnimation();

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

    private int getVerificationIconColor() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return R.color.warning_color;
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return R.color.info_color;

            case "rejected":
                return R.color.error_color;

            case "incomplete":
                return R.color.warning_color;

            case "approved":
                return R.color.success_color;

            default:
                return R.color.warning_color;
        }
    }

    private String getVerificationMessage() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return "Debes completar tu perfil y subir los documentos requeridos para poder conectarte.";
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return "Tus documentos están en proceso de verificación. Podrás conectarte cuando sean aprobados.";

            case "rejected":
                return "Tus documentos fueron rechazados. Por favor, actualiza tu información en tu perfil.";

            case "incomplete":
                return "Debes completar la verificación de documentos para poder conectarte.";

            case "approved":
                return "¡Documentos verificados! Ya puedes conectarte para recibir solicitudes.";

            default:
                return "Verificación de documentos pendiente. Contacta con soporte si el problema persiste.";
        }
    }
    private int getVerificationBackgroundColor() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return R.color.warning_light_bg;
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return R.color.info_light_bg;

            case "rejected":
                return R.color.error_light_bg;

            case "incomplete":
                return R.color.warning_light_bg;

            case "approved":
                return R.color.success_light_bg;

            default:
                return R.color.warning_light_bg;
        }
    }
    private int getVerificationIndicatorBackground() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return R.drawable.circle_yellow;
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return R.drawable.circle_blue;

            case "rejected":
                return R.drawable.circle_red;

            case "incomplete":
                return R.drawable.circle_yellow;

            case "approved":
                return R.drawable.circle_green;

            default:
                return R.drawable.circle_yellow;
        }
    }
    private int getVerificationIndicatorGlow() {
        if (mVerificationStatus == null || "incomplete".equalsIgnoreCase(mVerificationStatus)) {
            return R.drawable.circle_glow_yellow;
        }

        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return R.drawable.circle_glow_blue;

            case "rejected":
                return R.drawable.circle_glow_red;

            case "incomplete":
                return R.drawable.circle_glow_yellow;

            case "approved":
                return R.drawable.circle_glow_green;

            default:
                return R.drawable.circle_glow_yellow;
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

            updateWorkerAvailabilityStatus(false);
            setWorkerOnlineStatus(true);

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
            Log.d(TAG, "🔄 updateConnectionStatus - isVerified: " + isVerified + ", isConnected: " + isConnected);

            if (!isVerified) {
                updateConnectionStatusForUnverified();
                return;
            }

            if (isConnected) {
                // MODO CONECTADO
                Log.d(TAG, "🟢 Actualizando UI - CONECTADO");
                mTextConnectionStatus.setText(R.string.worker_home_status_on);
                mTextConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.ads_on_navy));
                mTextConnectionMessage.setText(R.string.worker_home_message_on);
                mTextConnectionMessage.setTextColor(ContextCompat.getColor(this, R.color.ads_on_navy_muted));
                mTextConnectionMessage.setCompoundDrawablesRelative(null, null, null, null);
                mConnectionIndicator.setBackgroundResource(R.drawable.circle_green);
                mConnectionIndicatorGlow.setBackgroundResource(R.drawable.circle_glow_green);

                // Interruptor encendido
                mButtonToggleConnection.setEnabled(true);
                mButtonToggleConnection.setAlpha(1.0f);
                mButtonToggleConnection.setChecked(true);
            } else {
                // MODO DESCONECTADO (pero verificado)
                Log.d(TAG, "🔴 Actualizando UI - DESCONECTADO");
                updateConnectionStatusForVerified();
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

    private String mNextServiceId;

    private void updateServicesStatus() {
        if (mAuthProvider == null || mAuthProvider.getId() == null) {
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
                        ServiceRequest next = null;

                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String status = requestSnapshot.child("status").getValue(String.class);
                            if (status == null) continue;
                            switch (status.toLowerCase()) {
                                case "pending":
                                    pendingCount++;
                                    break;
                                case "accepted":
                                case "on_the_way":
                                case "in_progress":
                                    activeCount++;
                                    // Próximo servicio: el activo con la llegada más cercana
                                    try {
                                        @SuppressWarnings("unchecked")
                                        java.util.Map<String, Object> data =
                                                (java.util.Map<String, Object>) requestSnapshot.getValue();
                                        if (data == null) break;
                                        ServiceRequest request = ServiceRequest.fromMap(data);
                                        request.setRequest_id(requestSnapshot.getKey());
                                        if (next == null || isSooner(request, next)) next = request;
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error leyendo servicio activo", e);
                                    }
                                    break;
                            }
                        }

                        final int finalPendingCount = pendingCount;
                        final int finalActiveCount = activeCount;
                        final ServiceRequest finalNext = next;
                        runOnUiThread(() -> {
                            updateServicesUI(finalPendingCount, finalActiveCount);
                            bindNextService(finalNext);
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar estado de servicios", error.toException());
                    }
                });
    }

    /** Sin agenda va al final; entre agendados, el más cercano. */
    private static boolean isSooner(ServiceRequest a, ServiceRequest b) {
        if (a.getScheduledAt() <= 0) return false;
        return b.getScheduledAt() <= 0 || a.getScheduledAt() < b.getScheduledAt();
    }

    /** Contadores del mockup 13: solicitudes nuevas y servicios activos. */
    private void updateServicesUI(int pendingCount, int activeCount) {
        mTextPendingRequests.setText(String.valueOf(pendingCount));
        mTextActiveServicesCount.setText(String.valueOf(activeCount));
        com.google.android.material.card.MaterialCardView newRequests = findViewById(R.id.card_new_requests);
        // Borde naranja cuando hay solicitudes nuevas por responder
        newRequests.setStrokeColor(ContextCompat.getColor(this,
                pendingCount > 0 ? R.color.ads_brand_text : R.color.ads_line));
        newRequests.setStrokeWidth(getResources().getDimensionPixelSize(
                pendingCount > 0 ? R.dimen.stroke_focus : R.dimen.stroke_control));
        if (pendingCount > 0) animateAttention(mLayoutPendingRequests);
    }

    /** Tarjeta «Próximo servicio» (mockup 13). */
    private void bindNextService(ServiceRequest request) {
        View title = findViewById(R.id.tv_next_service_title);
        View card = findViewById(R.id.card_next_service);
        mNextServiceId = request != null ? request.getRequest_id() : null;
        title.setVisibility(request != null ? View.VISIBLE : View.GONE);
        card.setVisibility(request != null ? View.VISIBLE : View.GONE);
        if (request == null) return;

        String service = request.getService_type() != null ? request.getService_type() : "Servicio";
        String client = request.getClient_name();
        ((TextView) findViewById(R.id.tv_next_service_name)).setText(
                client != null && !client.isEmpty() ? service + " · " + client : service);

        TextView status = findViewById(R.id.tv_next_service_status);
        String state = request.getStatus() != null ? request.getStatus() : "";
        status.setText("on_the_way".equals(state) ? R.string.request_status_on_the_way
                : "in_progress".equals(state) ? R.string.request_status_in_progress
                : R.string.request_status_accepted);

        TextView time = findViewById(R.id.tv_next_service_time);
        if (request.getScheduledAt() > 0) {
            java.util.Locale locale = new java.util.Locale("es", "CO");
            String text = new java.text.SimpleDateFormat("EEE d MMM, h:mm a", locale)
                    .format(new java.util.Date(request.getScheduledAt()));
            time.setText(text.substring(0, 1).toUpperCase(locale) + text.substring(1));
            time.setVisibility(View.VISIBLE);
        } else {
            time.setVisibility(View.GONE);
        }
        TextView address = findViewById(R.id.tv_next_service_address);
        address.setText(request.getAddress());
        address.setVisibility(request.getAddress() != null ? View.VISIBLE : View.GONE);
    }

    private void openNextService() {
        if (mNextServiceId == null) return;
        Intent intent = new Intent(this, ServiceDetailWorkerActivity.class);
        intent.putExtra("request_id", mNextServiceId);
        startActivity(intent);
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

        try {
            Intent intent = new Intent(HomeWorkerActivity.this, PendingRequestsActivity.class);
            intent.putExtra("worker_id", mAuthProvider.getId());
            intent.putExtra("worker_type", mCurrentWorker != null ? mCurrentWorker.getCategoriesDisplay() : "");
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

    private void generateToken() {
        if (mAuthProvider.getId() != null) {
            mTokenProvider.create(mAuthProvider.getId());
        }
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

        Log.d(TAG, "📱 onResume - isVerified: " + isVerified + ", status: " + mVerificationStatus);

        if (isVerified) {
            setWorkerOnlineStatus(true);
            if (isConnected) {
                updateConnectionStatus();
            } else {
                updateConnectionStatusForVerified();
            }
        } else {
            updateConnectionStatusForUnverified();
        }

        updatePerformanceIndicators();
        updateServicesStatus();
        updateNavigationDrawerHeader();

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
        {
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