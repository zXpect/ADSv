package com.project.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.database.Query;
import com.project.ads.models.ServiceRequest;
import com.project.ads.helpers.DisplayNameHelper;
import com.project.ads.helpers.MapPinHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mapa del trabajador · mockups 57 (conectado) y 58 (desconectado).
 * La conexión, Geofire y la verificación no cambian; la hoja inferior muestra la
 * solicitud nueva más reciente (con su distancia si la solicitud tiene lat/lng)
 * o el aviso del estado.
 */
public class MapWorkerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapWorkerActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 17f;
    private static final long LOCATION_UPDATE_INTERVAL = 30000;
    private static final long LOCATION_FASTEST_INTERVAL = 15000;

    private GoogleMap mMap;
    private Button mBtnConnect;
    private LinearLayout mToggle;
    private MaterialSwitch mSwitch;
    private TextView mConnectionLabel;
    private TextView mSubtitle;
    private View mGroupRequest;
    private TextView mRequestTitle;
    private TextView mRequestMeta;
    private TextView mSheetTitle;
    private TextView mSheetMessage;

    // Solicitudes nuevas (status pending) dirigidas a este trabajador
    private final List<PendingPin> mPending = new ArrayList<>();
    private final List<Marker> mMarkers = new ArrayList<>();
    private PendingPin mSelected;
    private BitmapDescriptor mClientPin;
    private Query mRequestsQuery;
    private ValueEventListener mRequestsListener;

    private static class PendingPin {
        ServiceRequest request;
        LatLng position;
    }

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

        initProviders();
        initViews();
        initMap();
        setupLocationCallback();
        loadWorkerData();
        listenPendingRequests();
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mWorkerProvider = new WorkerProvider();
        mGeofireProvider = new GeofireProvider();
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initViews() {
        mBtnConnect = findViewById(R.id.btnconnect);
        mToggle = findViewById(R.id.btn_connection_toggle);
        mSwitch = findViewById(R.id.switch_connection);
        mConnectionLabel = findViewById(R.id.tv_connection_label);
        mSubtitle = findViewById(R.id.tv_map_subtitle);
        mGroupRequest = findViewById(R.id.group_sheet_request);
        mRequestTitle = findViewById(R.id.tv_sheet_request_title);
        mRequestMeta = findViewById(R.id.tv_sheet_request_meta);
        mSheetTitle = findViewById(R.id.tv_sheet_title);
        mSheetMessage = findViewById(R.id.tv_sheet_message);

        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        // Inicialmente deshabilitado hasta verificar estado
        mBtnConnect.setEnabled(false);
        mToggle.setEnabled(false);
        mToggle.setOnClickListener(v -> toggleConnection());
        mBtnConnect.setOnClickListener(v -> onSheetAction());
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
        mMap.getUiSettings().setZoomControlsEnabled(false);
        // La hoja inferior se monta 24 dp sobre el mapa
        mMap.setPadding(0, 0, 0, Math.round(24 * getResources().getDisplayMetrics().density));
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof PendingPin) {
                mSelected = (PendingPin) tag;
                renderState();
            }
            return false;
        });

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
                        renderState();
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
                        } else {
                            runOnUiThread(() -> syncAvailability(snapshot));
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

    /**
     * isAvailable en Firebase es el estado único de conexión: si el inicio u otra
     * pantalla lo cambia, el interruptor del mapa lo sigue sin volver a escribirlo.
     */
    private void syncAvailability(DataSnapshot snapshot) {
        if (!isVerified) return;
        boolean available = Boolean.TRUE.equals(snapshot.child("isAvailable").getValue(Boolean.class));
        if (available == isConnected) return;
        if (available && !checkLocationPermissions()) return;
        isConnected = available;
        if (available) {
            startLocationUpdates();
        } else if (mFusedLocation != null && mLocationCallback != null) {
            // Quien desconectó ya quitó la ubicación de active_workers
            mFusedLocation.removeLocationUpdates(mLocationCallback);
        }
        renderState();
    }

    private void reconnectWorker() {
        // Ya estaba conectado (desde el inicio o una visita anterior al mapa): mostrarlo así
        if (checkLocationPermissions()) {
            isConnected = true;
            startLocationUpdates();
            updateButtonState(true);
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

        renderState();

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
        runOnUiThread(this::renderState);
    }

    /** Píldora, subtítulo y hoja inferior según verificación, conexión y solicitudes nuevas. */
    private void renderState() {
        if (!isVerificationChecked) return;
        boolean on = isVerified && isConnected;

        mToggle.setEnabled(true);
        mToggle.setBackgroundResource(on ? R.drawable.bg_ads_pill_on : R.drawable.bg_ads_pill_off);
        mSwitch.setChecked(on);
        mConnectionLabel.setText(on ? R.string.map_worker_connected : R.string.map_worker_disconnected);
        mConnectionLabel.setTextColor(ContextCompat.getColor(this, on ? R.color.ads_on_navy : R.color.ads_text));
        mToggle.setContentDescription(getString(on ? R.string.map_worker_connected : R.string.map_worker_disconnected));
        mSubtitle.setText(on ? R.string.map_worker_subtitle_on : R.string.map_worker_subtitle_off);

        PendingPin shown = null;
        if (on && !mPending.isEmpty()) {
            shown = mPending.contains(mSelected) ? mSelected : mPending.get(0);
        }
        mGroupRequest.setVisibility(shown != null ? View.VISIBLE : View.GONE);
        mSheetTitle.setVisibility(shown != null ? View.GONE : View.VISIBLE);
        mSheetMessage.setVisibility(shown != null ? View.GONE : View.VISIBLE);
        mBtnConnect.setEnabled(true);
        mBtnConnect.setVisibility(View.VISIBLE);

        if (!isVerified) {
            mSheetTitle.setText(R.string.map_worker_unverified_title);
            mSheetMessage.setText(getVerificationMessage());
            mBtnConnect.setText(R.string.map_worker_view_documents);
        } else if (!isConnected) {
            mSheetTitle.setText(R.string.map_worker_off_title);
            mSheetMessage.setText(R.string.map_worker_off_message);
            mBtnConnect.setText(R.string.map_worker_connect);
        } else if (shown != null) {
            ServiceRequest request = shown.request;
            String client = request.getClient_name() != null ? DisplayNameHelper.format(request.getClient_name()) : getString(R.string.inbox_client_default);
            String service = request.getService_type() != null ? request.getService_type() : getString(R.string.history_service_default);
            mRequestTitle.setText(client + " · " + service);
            mRequestMeta.setText(requestMeta(shown));
            mBtnConnect.setText(R.string.map_worker_view_request);
        } else {
            mSheetTitle.setText(R.string.map_worker_on_title);
            mSheetMessage.setText(R.string.map_worker_on_message);
            mBtnConnect.setVisibility(View.GONE);
        }
        renderMarkers();
    }

    /** "A 1,2 km de ti" si hay coordenadas; si no, la dirección o hace cuánto llegó. */
    private String requestMeta(PendingPin pin) {
        if (pin.position != null && mCurrentLatLng != null) {
            float[] meters = new float[1];
            Location.distanceBetween(mCurrentLatLng.latitude, mCurrentLatLng.longitude,
                    pin.position.latitude, pin.position.longitude, meters);
            return getString(R.string.map_worker_distance,
                    String.format(new Locale("es", "CO"), "%.1f", meters[0] / 1000f));
        }
        String address = pin.request.getAddress();
        return address != null && !address.isEmpty() ? address : pin.request.getTimeAgo();
    }

    private void onSheetAction() {
        if (!isVerificationChecked) return;
        if (!isVerified) {
            openDocumentsStatus();
        } else if (!isConnected) {
            toggleConnection();
        } else if (!mPending.isEmpty()) {
            PendingPin pin = mPending.contains(mSelected) ? mSelected : mPending.get(0);
            Intent intent = new Intent(this, RequestDetailActivity.class);
            intent.putExtra("request_id", pin.request.getRequest_id());
            startActivity(intent);
        }
    }

    /** Pines de las solicitudes nuevas, solo mientras está conectado. */
    private void renderMarkers() {
        if (mMap == null) return;
        for (Marker marker : mMarkers) marker.remove();
        mMarkers.clear();
        if (!isVerified || !isConnected) return;
        for (PendingPin pin : mPending) {
            if (pin.position == null) continue;
            if (mClientPin == null) mClientPin = MapPinHelper.fromVector(this, R.drawable.pin_cliente);
            // Ubicación del cliente: punto con halo, centrado en la coordenada
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(pin.position)
                    .title(pin.request.getService_type())
                    .anchor(0.5f, 0.5f)
                    .icon(mClientPin));
            if (marker != null) {
                marker.setTag(pin);
                mMarkers.add(marker);
            }
        }
    }

    /** Misma consulta que el inicio y la bandeja: requests filtrado por el propio uid. */
    private void listenPendingRequests() {
        String workerId = mAuthProvider.getId();
        if (workerId == null) return;
        mRequestsQuery = FirebaseDatabase.getInstance().getReference().child("requests")
                .orderByChild("worker_id").equalTo(workerId);
        mRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PendingPin> pending = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (!"pending".equals(child.child("status").getValue(String.class))) continue;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (data == null) continue;
                        PendingPin pin = new PendingPin();
                        pin.request = ServiceRequest.fromMap(data);
                        pin.request.setRequest_id(child.getKey());
                        Double lat = child.child("lat").getValue(Double.class);
                        Double lng = child.child("lng").getValue(Double.class);
                        if (lat != null && lng != null) pin.position = new LatLng(lat, lng);
                        pending.add(pin);
                    } catch (Exception e) {
                        Log.e(TAG, "Error leyendo solicitud " + child.getKey(), e);
                    }
                }
                // La más reciente primero
                java.util.Collections.sort(pending, (a, b) ->
                        Long.compare(b.request.getTimestamp(), a.request.getTimestamp()));
                mPending.clear();
                mPending.addAll(pending);
                renderState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar solicitudes nuevas: " + error.getMessage());
            }
        };
        mRequestsQuery.addValueEventListener(mRequestsListener);
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
            new MaterialAlertDialogBuilder(this)
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

        new MaterialAlertDialogBuilder(this)
                .setTitle("Verificación de Documentos Pendiente")
                .setMessage(message + "\n\n¿Deseas revisar el estado de tus documentos?")
                .setPositiveButton("Ver Documentos", (dialog, which) -> {
                    openDocumentsStatus();
                })
                .setNegativeButton("Entendido", null)
                .show();
    }

    private String getVerificationMessage() {
        if (mVerificationStatus == null) return getString(R.string.map_worker_unverified_incomplete);
        switch (mVerificationStatus.toLowerCase()) {
            case "documents_submitted":
            case "pending":
                return getString(R.string.map_worker_unverified_pending);
            case "rejected":
                return getString(R.string.map_worker_unverified_rejected);
            case "approved":
                return getString(R.string.map_worker_unverified_approved);
            case "incomplete":
            default:
                return getString(R.string.map_worker_unverified_incomplete);
        }
    }

    private void openDocumentsStatus() {
        try {
            Intent intent = new Intent(MapWorkerActivity.this, DocumentStatusActivity.class);
            intent.putExtra("worker_id", mAuthProvider.getId());
            startActivity(intent);
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

        if (mRequestsQuery != null && mRequestsListener != null) {
            mRequestsQuery.removeEventListener(mRequestsListener);
        }

        // Remover listener
        if (mWorkerDataListener != null && mAuthProvider.getId() != null) {
            mWorkerProvider.removeWorkerListener(mAuthProvider.getId(), mWorkerDataListener);
        }

        // Salir del mapa no desconecta: el inicio queda debajo, sigue la misma
        // conexión (isAvailable) y publica la ubicación. Solo se sueltan las
        // actualizaciones de ubicación de esta pantalla.
        if (mFusedLocation != null && mLocationCallback != null) {
            mFusedLocation.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onBackPressed() {
        if (isConnected) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Atención")
                    .setMessage("Estás conectado. ¿Deseas salir? Permanecerás conectado.")
                    .setPositiveButton("Salir", (dialog, which) -> {
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            finish();
        }
    }
}