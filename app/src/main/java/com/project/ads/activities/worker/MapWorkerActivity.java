package com.project.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.project.ads.includes.MyToolbar;
import com.project.ads.models.Worker;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.GeofireProvider;
import com.project.ads.providers.WorkerProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;

public class MapWorkerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapWorkerActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 17f;
    private static final long LOCATION_UPDATE_INTERVAL = 30000;
    private static final long LOCATION_FASTEST_INTERVAL = 15000;

    private GoogleMap mMap;
    private Button mBtnConnect;

    // Providers
    private AuthProvider mAuthProvider;
    private WorkerProvider mWorkerProvider;
    private GeofireProvider mGeofireProvider;
    private FusedLocationProviderClient mFusedLocation;

    // Location
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private LatLng mCurrentLatLng;

    // Estado
    private boolean isConnected = false;
    private boolean isVerified = false;
    private boolean isVerificationChecked = false;
    private String mVerificationStatus = null;
    private Worker mCurrentWorker;

    // Listener
    private ValueEventListener mWorkerDataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_worker);

        initToolbar();
        initProviders();
        initViews();
        initMap();
        setupLocationCallback();
        loadWorkerData();
    }

    private void initToolbar() {
        MyToolbar.showColorToolBarr(this, "Mapa del Trabajador", true);
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mWorkerProvider = new WorkerProvider();
        mGeofireProvider = new GeofireProvider();
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initViews() {
        mBtnConnect = findViewById(R.id.btnconnect);

        // Inicialmente deshabilitado hasta verificar estado
        mBtnConnect.setEnabled(false);
        mBtnConnect.setAlpha(0.5f);
        mBtnConnect.setText(R.string.conectarse);

        mBtnConnect.setOnClickListener(v -> toggleConnection());
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Aplicar estilo oscuro si está habilitado
        if (isDarkModeEnabled()) {
            try {
                boolean success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)
                );
                if (!success) {
                    Log.e(TAG, "Error al aplicar estilo de mapa");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al cargar estilo de mapa", e);
            }
        }

        mLocationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(10);

        startLocationIfPermitted();
    }

    private boolean isDarkModeEnabled() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // Actualizar ubicación solo si está conectado
                        if (isConnected) {
                            updateLocationInFirebase();
                        }

                        // Centrar cámara solo la primera vez
                        if (mMap != null) {
                            moveCamera(mCurrentLatLng);
                        }
                    }
                }
            }
        };
    }

    private void moveCamera(LatLng latLng) {
        if (mMap != null && latLng != null) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(latLng)
                            .zoom(DEFAULT_ZOOM)
                            .build()
            ));
        }
    }

    private void loadWorkerData() {
        if (mAuthProvider.getId() != null) {
            mWorkerDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        mCurrentWorker = snapshot.getValue(Worker.class);

                        // Verificar estado de documentos
                        checkVerificationStatus(snapshot);

                        // Actualizar UI según verificación
                        if (!isVerificationChecked) {
                            isVerificationChecked = true;

                            runOnUiThread(() -> {
                                if (isVerified) {
                                    // Usuario aprobado: Verificar si estaba conectado
                                    checkPreviousConnectionState(snapshot);
                                } else {
                                    // Usuario NO aprobado: Forzar desconexión y deshabilitar
                                    forceDisconnectAndDisable();
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error al cargar datos del trabajador", error.toException());
                    showErrorToast("Error al cargar datos del perfil");
                }
            };

            mWorkerProvider.listenForWorkerChanges(mAuthProvider.getId(), mWorkerDataListener);
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

    private void checkPreviousConnectionState(DataSnapshot snapshot) {
        try {
            // Verificar si isAvailable está en true
            Boolean isAvailable = snapshot.child("isAvailable").getValue(Boolean.class);

            if (isAvailable != null && isAvailable) {
                // Estaba conectado previamente, reconectar
                reconnectWorker();
            } else {
                // No estaba conectado, habilitar botón para conectar
                updateButtonState(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar estado previo", e);
            updateButtonState(false);
        }
    }

    private void reconnectWorker() {
        // Reconectar automáticamente si estaba conectado
        if (checkLocationPermissions()) {
            isConnected = true;
            startLocationUpdates();
            updateButtonState(true);
            showInfoToast("Reconectado automáticamente");
            Log.d(TAG, "Worker reconectado automáticamente");
        } else {
            // Si no hay permisos, desconectar
            forceDisconnect();
        }
    }

    private void forceDisconnectAndDisable() {
        // Desconectar y deshabilitar botón para usuarios no verificados
        isConnected = false;
        stopLocationUpdates();

        // Asegurar que isAvailable sea false
        updateWorkerAvailabilityStatus(false);

        // Deshabilitar botón
        mBtnConnect.setEnabled(false);
        mBtnConnect.setAlpha(0.5f);
        mBtnConnect.setText("Verificación Pendiente");

        showVerificationDialog();

        Log.d(TAG, "Worker desconectado - No verificado");
    }

    private void forceDisconnect() {
        // Solo desconectar sin mostrar diálogo
        isConnected = false;
        stopLocationUpdates();
        updateWorkerAvailabilityStatus(false);
        updateButtonState(false);
    }

    private void toggleConnection() {
        // VALIDACIÓN CRÍTICA: Verificar si está aprobado
        if (!isVerified) {
            showErrorToast("No puedes conectarte hasta que tus documentos sean verificados");
            showVerificationDialog();
            return;
        }

        if (isConnected) {
            disconnectWorker();
        } else {
            connectWorker();
        }
    }

    private void connectWorker() {
        if (!isVerified) {
            showErrorToast("No puedes conectarte hasta que tus documentos sean verificados");
            showVerificationDialog();
            return;
        }

        if (checkLocationPermissions()) {
            try {
                startLocationUpdates();
                isConnected = true;

                // CRÍTICO: Actualizar isAvailable a true en Firebase
                updateWorkerAvailabilityStatus(true);
                setWorkerOnlineStatus(true);

                updateButtonState(true);
                showSuccessToast("¡Conectado exitosamente!");

                // Configurar desconexión automática
                setupAutoDisconnect();

                Log.d(TAG, "Worker conectado - Agregado a active_workers");

            } catch (Exception e) {
                Log.e(TAG, "Error al conectar worker", e);
                showErrorToast("Error al conectar");
            }
        } else {
            requestLocationPermissions();
        }
    }

    private void disconnectWorker() {
        try {
            stopLocationUpdates();
            isConnected = false;

            // CRÍTICO: Actualizar isAvailable a false en Firebase
            updateWorkerAvailabilityStatus(false);
            setWorkerOnlineStatus(true); // Sigue online, solo no disponible

            updateButtonState(false);
            showInfoToast("Desconectado");

            Log.d(TAG, "Worker desconectado - Removido de active_workers");

        } catch (Exception e) {
            Log.e(TAG, "Error al desconectar worker", e);
            showErrorToast("Error al desconectar");
        }
    }

    private void setupAutoDisconnect() {
        if (mAuthProvider.getId() != null) {
            DatabaseReference workerRef = FirebaseDatabase.getInstance()
                    .getReference("User/Trabajadores/" + mAuthProvider.getId());

            workerRef.child("isOnline").onDisconnect().setValue(false);
            workerRef.child("isAvailable").onDisconnect().setValue(false);

            DatabaseReference activeWorkerRef = FirebaseDatabase.getInstance()
                    .getReference("active_workers/" + mAuthProvider.getId());
            activeWorkerRef.onDisconnect().removeValue();
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            try {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                }

                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                Log.d(TAG, "Actualizaciones de ubicación iniciadas");
            } catch (SecurityException e) {
                Log.e(TAG, "Error de permisos al iniciar actualizaciones de ubicación", e);
                showErrorToast("Error al acceder a la ubicación");
            }
        }
    }

    private void stopLocationUpdates() {
        if (mFusedLocation != null && mLocationCallback != null) {
            mFusedLocation.removeLocationUpdates(mLocationCallback);

            // CRÍTICO: Remover de active_workers cuando se desconecta
            if (mAuthProvider.getId() != null) {
                mGeofireProvider.removeLocation(mAuthProvider.getId());
                Log.d(TAG, "Ubicación removida de active_workers");
            }
        }
    }

    private void updateLocationInFirebase() {
        if (mCurrentLatLng != null && mAuthProvider.getId() != null && isConnected) {
            // CRÍTICO: Solo guardar en active_workers si está conectado
            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);

            // Actualizar también en el perfil del worker
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
                            // CRÍTICO: Agregar a active_workers
                            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);
                            Log.d(TAG, "Trabajador agregado a active_workers - isAvailable: true");
                        } else if (!isAvailable) {
                            // CRÍTICO: Remover de active_workers
                            mGeofireProvider.removeLocation(mAuthProvider.getId());
                            Log.d(TAG, "Trabajador removido de active_workers - isAvailable: false");
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error al actualizar disponibilidad", e));
        }
    }

    private void updateButtonState(boolean connected) {
        runOnUiThread(() -> {
            if (connected) {
                mBtnConnect.setText("Desconectarse");
                mBtnConnect.setEnabled(true);
                mBtnConnect.setAlpha(1.0f);
                mBtnConnect.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.error_color));
            } else {
                mBtnConnect.setText(R.string.conectarse);
                mBtnConnect.setEnabled(true);
                mBtnConnect.setAlpha(1.0f);
                mBtnConnect.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
            }
        });
    }

    private void startLocationIfPermitted() {
        if (checkLocationPermissions()) {
            try {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                }

                // Obtener ubicación inicial
                mFusedLocation.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        moveCamera(mCurrentLatLng);
                    }
                });
            } catch (SecurityException e) {
                Log.e(TAG, "Error de permisos al iniciar ubicación", e);
                showErrorToast("Error al acceder a la ubicación");
            }
        } else {
            requestLocationPermissions();
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
                    .setMessage("Esta aplicación necesita acceso a tu ubicación para mostrarte en el mapa")
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
                startLocationIfPermitted();
            } else {
                showErrorToast("Permisos de ubicación necesarios");
            }
        }
    }

    private void showVerificationDialog() {
        String message = getVerificationMessage();

        new AlertDialog.Builder(this)
                .setTitle("Verificación de Documentos Pendiente")
                .setMessage(message + "\n\n¿Deseas revisar el estado de tus documentos?")
                .setPositiveButton("Ver Documentos", (dialog, which) -> {
                    openDocumentsStatus();
                })
                .setNegativeButton("Entendido", null)
                .show();
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

    private void openDocumentsStatus() {
        try {
            Intent intent = new Intent(MapWorkerActivity.this, DocumentStatusActivity.class);
            intent.putExtra("worker_id", mAuthProvider.getId());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir documentos", e);
            showErrorToast("Error al abrir pantalla de documentos");
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Actualizar estado online si está verificado
        if (isVerified) {
            setWorkerOnlineStatus(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // No desconectar automáticamente al pausar
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remover listener
        if (mWorkerDataListener != null && mAuthProvider.getId() != null) {
            mWorkerProvider.removeWorkerListener(mAuthProvider.getId(), mWorkerDataListener);
        }

        // Solo desconectar si la actividad se está finalizando permanentemente
        if (isFinishing()) {
            setWorkerOnlineStatus(false);

            try {
                stopLocationUpdates();

                // Si estaba conectado, desconectar
                if (isConnected) {
                    updateWorkerAvailabilityStatus(false);
                }

                isConnected = false;
            } catch (Exception e) {
                Log.e(TAG, "Error durante destrucción", e);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isConnected) {
            new AlertDialog.Builder(this)
                    .setTitle("Atención")
                    .setMessage("Estás conectado. ¿Deseas salir? Permanecerás conectado.")
                    .setPositiveButton("Salir", (dialog, which) -> {
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}