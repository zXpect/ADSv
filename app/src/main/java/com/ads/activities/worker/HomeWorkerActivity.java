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
import android.content.DialogInterface;
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
import android.widget.Button;
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
import com.ads.providers.WorkerProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;

public class HomeWorkerActivity extends AppCompatActivity {

    private static final String TAG = "HomeWorkerActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 30000; // 30 segundos
    private static final long LOCATION_FASTEST_INTERVAL = 15000; // 15 segundos

    // Botones existentes
    Button mButtonViewMap2;
    Button mButtonFixDepot;

    // Nuevos componentes
    Button mButtonConnect;
    Button mButtonDisconnect;
    View mConnectionIndicator;
    TextView mTextConnectionStatus;
    TextView mTextRating;
    TextView mTextCompletedServices;
    TextView mTextMonthlyEarnings;
    TextView mTextActiveServices;
    TextView mTextPendingRequests;
    TextView mTextWorkerName;
    TextView mTextWorkerType;
    ImageView mImageWorkerProfile;

    // Componentes existentes
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

    // Variables de estado
    private boolean isConnected = false;
    private Worker mCurrentWorker;
    private LatLng mCurrentLatLng;
    private Handler mUIUpdateHandler;
    private ValueEventListener mWorkerDataListener;

    private LinearLayout mLayoutPendingRequests;

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
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mWorkerProvider = new WorkerProvider();
        mGeofireProvider = new GeofireProvider();
        mTokenProvider = new TokenProvider();
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initViews() {
        // Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        mToolbar = findViewById(R.id.toolbar);

        // Botones de navegación
        mButtonViewMap2 = findViewById(R.id.vermapaworker);
        mButtonFixDepot = findViewById(R.id.buttonFixDepot);

        // Componentes de conexión
        mButtonConnect = findViewById(R.id.buttonConnect);
        mButtonDisconnect = findViewById(R.id.buttonDisconnect);
        mConnectionIndicator = findViewById(R.id.connectionIndicator);
        mTextConnectionStatus = findViewById(R.id.textConnectionStatus);

        // Información del trabajador
        mTextWorkerName = findViewById(R.id.textWorkerName);
        mTextWorkerType = findViewById(R.id.textWorkerType);
        mImageWorkerProfile = findViewById(R.id.imageWorkerProfile);

        // Indicadores de desempeño
        mTextRating = findViewById(R.id.textRating);
        mTextCompletedServices = findViewById(R.id.textCompletedServices);
        mTextMonthlyEarnings = findViewById(R.id.textMonthlyEarnings);

        // Gestión de servicios
        mTextActiveServices = findViewById(R.id.textActiveServices);
        mTextPendingRequests = findViewById(R.id.textPendingRequests);
        mLayoutPendingRequests = findViewById(R.id.layoutPendingRequests);

    }

    private void setupNavigationDrawer() {
        setSupportActionBar(mToolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                mToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    Toast.makeText(HomeWorkerActivity.this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_profile) {
                    openProfile();
                } else if (id == R.id.nav_settings) {
                    openSettings();
                } else if (id == R.id.action_logout) {
                    logout();
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    private void setupClickListeners() {
        mLayoutPendingRequests.setOnClickListener(v -> viewPendingRequests());
        mButtonViewMap2.setOnClickListener(v -> viewMap());
        mButtonFixDepot.setOnClickListener(v -> goToFixDepot());
        mButtonConnect.setOnClickListener(v -> connectWorker());
        mButtonDisconnect.setOnClickListener(v -> disconnectWorker());
    }

    private void viewPendingRequests() {
        if (mAuthProvider == null || mAuthProvider.getId() == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el texto actual para verificar si hay solicitudes
        String currentText = mTextPendingRequests.getText().toString();

        if (currentText.equals("No hay solicitudes pendientes") ||
                currentText.equals("Error al cargar")) {
            Toast.makeText(this, "No hay solicitudes pendientes para mostrar", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(HomeWorkerActivity.this, PendingRequestsActivity.class);
            intent.putExtra("worker_id", mAuthProvider.getId());
            intent.putExtra("worker_type", mCurrentWorker != null ? mCurrentWorker.getWork() : "");
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir solicitudes pendientes", e);
            Toast.makeText(this, "Error al abrir solicitudes pendientes", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void setupLocationCallback() {
        mLocationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(10); // Solo actualizar si se mueve más de 10 metros

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
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error al cargar datos del trabajador", error.toException());
                    Toast.makeText(HomeWorkerActivity.this,
                            "Error al cargar datos del perfil", Toast.LENGTH_SHORT).show();
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

                // Aquí podrías cargar la imagen del perfil si tienes una librería como Glide
                // Glide.with(this).load(mCurrentWorker.getImage()).into(mImageWorkerProfile);
            });
        }
    }

    private void connectWorker() {
        if (checkLocationPermissions()) {
            try {
                startLocationUpdates();
                isConnected = true;

                // Actualizar estado en Firebase
                updateWorkerOnlineStatus(true);
                updateWorkerAvailabilityStatus(true);

                updateConnectionStatus();
                Toast.makeText(this, "¡Te has conectado exitosamente!", Toast.LENGTH_SHORT).show();

                Log.d(TAG, "Worker conectado exitosamente");

            } catch (Exception e) {
                Log.e(TAG, "Error al conectar worker", e);
                Toast.makeText(this, "Error al conectar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            requestLocationPermissions();
        }
    }

    private void disconnectWorker() {
        try {
            stopLocationUpdates();
            isConnected = false;

            // Actualizar estado en Firebase
            updateWorkerOnlineStatus(false);
            updateWorkerAvailabilityStatus(false);

            updateConnectionStatus();
            Toast.makeText(this, "Te has desconectado exitosamente", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Worker desconectado exitosamente");

        } catch (Exception e) {
            Log.e(TAG, "Error al desconectar worker", e);
            Toast.makeText(this, "Error al desconectar: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

            // Remover ubicación de Geofire
            if (mAuthProvider.getId() != null) {
                mGeofireProvider.removeLocation(mAuthProvider.getId());
            }
        }
    }

    private void updateLocationInFirebase() {
        if (mCurrentLatLng != null && mAuthProvider.getId() != null) {
            // Actualizar en Geofire para búsquedas por proximidad
            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);

            // Actualizar en el perfil del worker
            mWorkerProvider.updateWorkerLocation(
                    mAuthProvider.getId(),
                    mCurrentLatLng.latitude,
                    mCurrentLatLng.longitude
            ).addOnFailureListener(e ->
                    Log.e(TAG, "Error al actualizar ubicación", e)
            );
        }
    }

    private void updateWorkerOnlineStatus(boolean isOnline) {
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
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error al actualizar disponibilidad", e)
                    );
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
                Toast.makeText(this, "Permisos de ubicación necesarios para conectarse", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateConnectionStatus() {
        runOnUiThread(() -> {
            if (isConnected) {
                mTextConnectionStatus.setText("Conectado");
                mTextConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
                mConnectionIndicator.setBackgroundResource(R.drawable.circle_green);
                mButtonConnect.setEnabled(false);
                mButtonDisconnect.setEnabled(true);
            } else {
                mTextConnectionStatus.setText("Desconectado");
                mTextConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                mConnectionIndicator.setBackgroundResource(R.drawable.circle_red);
                mButtonConnect.setEnabled(true);
                mButtonDisconnect.setEnabled(false);
            }
        });
    }

    private void updatePerformanceIndicators() {
        if (mCurrentWorker != null) {
            runOnUiThread(() -> {
                mTextRating.setText("⭐ " + mCurrentWorker.getFormattedRating());
                mTextCompletedServices.setText(String.valueOf(mCurrentWorker.getTotalRatings()));

                // Calcular ganancias estimadas (esto debería venir de una base de datos real)
                double estimatedEarnings = mCurrentWorker.getPricePerHour() * mCurrentWorker.getTotalRatings() * 2; // Estimación
                mTextMonthlyEarnings.setText("$" + String.format("%.0f", estimatedEarnings));
            });
        }
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

        // Obtener solicitudes pendientes dirigidas a este trabajador
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
                        int completedToday = 0;

                        long todayStart = getTodayStartTimestamp();

                        if (snapshot.exists()) {
                            for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                                String status = requestSnapshot.child("status").getValue(String.class);
                                Long timestamp = requestSnapshot.child("timestamp").getValue(Long.class);

                                if (status != null) {
                                    switch (status.toLowerCase()) {
                                        case "pending":
                                            pendingCount++;
                                            break;
                                        case "accepted":
                                        case "in_progress":
                                            activeCount++;
                                            break;
                                        case "completed":
                                            // Contar solo los completados hoy
                                            if (timestamp != null && timestamp >= todayStart) {
                                                completedToday++;
                                            }
                                            break;
                                    }
                                }
                            }
                        }

                        // Actualizar UI en el hilo principal
                        final int finalPendingCount = pendingCount;
                        final int finalActiveCount = activeCount;
                        final int finalCompletedToday = completedToday;

                        runOnUiThread(() -> updateServicesUI(finalPendingCount, finalActiveCount, finalCompletedToday));
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

        // También verificar solicitudes generales (sin worker_id específico) que coincidan con el tipo de trabajo
        checkGeneralRequests();
    }

    private long getTodayStartTimestamp() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void setupRealTimeServicesUpdates() {
        if (mAuthProvider == null || mAuthProvider.getId() == null) {
            return;
        }

        String workerId = mAuthProvider.getId();

        // Listener para cambios en tiempo real
        ValueEventListener servicesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateServicesStatus(); // Reutilizar la lógica existente
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error en listener de servicios en tiempo real", error.toException());
            }
        };

        // Agregar listener para solicitudes de este trabajador
        FirebaseDatabase.getInstance()
                .getReference()
                .child("requests")
                .orderByChild("worker_id")
                .equalTo(workerId)
                .addValueEventListener(servicesListener);
    }



    private void updateServicesUI(int pendingCount, int activeCount, int completedToday) {
        try {
            if (mTextActiveServices != null) {
                if (activeCount > 0) {
                    mTextActiveServices.setText(activeCount + " servicio(s) activo(s)");
                    mTextActiveServices.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, getTheme()));
                } else {
                    mTextActiveServices.setText("No hay servicios activos");
                    mTextActiveServices.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
                }
            }

            if (mTextPendingRequests != null && mLayoutPendingRequests != null) {
                if (pendingCount > 0) {
                    mTextPendingRequests.setText(pendingCount + " solicitud(es) pendiente(s) - Toca para ver");
                    mTextPendingRequests.setTextColor(getResources().getColor(android.R.color.holo_blue_dark, getTheme()));

                    // Habilitar el click y darle un estilo más visible
                    mLayoutPendingRequests.setEnabled(true);
                    mLayoutPendingRequests.setAlpha(1.0f);
                } else {
                    mTextPendingRequests.setText("No hay solicitudes pendientes");
                    mTextPendingRequests.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));

                    // Deshabilitar el click y reducir opacidad
                    mLayoutPendingRequests.setEnabled(false);
                    mLayoutPendingRequests.setAlpha(0.6f);
                }
            }

            // Actualizar también los servicios completados si tienes esa vista
            if (mTextCompletedServices != null && completedToday > 0) {
                String currentText = mTextCompletedServices.getText().toString();
                // Solo actualizar si no hay datos del worker cargados
                if (currentText.equals("0") || currentText.isEmpty()) {
                    mTextCompletedServices.setText(String.valueOf(completedToday));
                }
            }

            Log.d(TAG, "UI actualizada - Activos: " + activeCount + ", Pendientes: " + pendingCount +
                    ", Completados hoy: " + completedToday);

        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar UI de servicios", e);
        }
    }


    private void checkGeneralRequests() {
        if (mCurrentWorker == null || mCurrentWorker.getWork() == null) {
            return;
        }

        String workerServiceType = mCurrentWorker.getWork().toLowerCase();

        FirebaseDatabase.getInstance()
                .getReference()
                .child("requests")
                .orderByChild("status")
                .equalTo("pending")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int generalRequestsCount = 0;

                        if (snapshot.exists()) {
                            for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                                String serviceType = requestSnapshot.child("service_type").getValue(String.class);
                                String assignedWorkerId = requestSnapshot.child("worker_id").getValue(String.class);

                                // Solo contar solicitudes que:
                                // 1. No tengan worker_id asignado O sea para este trabajador
                                // 2. Coincidan con el tipo de servicio del trabajador
                                if (serviceType != null &&
                                        serviceType.toLowerCase().equals(workerServiceType) &&
                                        (assignedWorkerId == null || assignedWorkerId.isEmpty() ||
                                                assignedWorkerId.equals(mAuthProvider.getId()))) {
                                    generalRequestsCount++;
                                }
                            }
                        }

                        final int finalGeneralCount = generalRequestsCount;
                        runOnUiThread(() -> {
                            if (mTextPendingRequests != null && finalGeneralCount > 0) {
                                String currentText = mTextPendingRequests.getText().toString();
                                if (currentText.equals("No hay solicitudes pendientes") ||
                                        currentText.equals("Error al cargar")) {
                                    mTextPendingRequests.setText(finalGeneralCount + " solicitud(es) disponible(s)");
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al verificar solicitudes generales", error.toException());
                    }
                });
    }

    private void startPeriodicUIUpdates() {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateServicesStatus();
                mUIUpdateHandler.postDelayed(this, 30000); // Actualizar cada 30 segundos
            }
        };
        mUIUpdateHandler.post(updateRunnable);
    }

    private void openProfile() {
        // Intent intent = new Intent(this, ProfileWorkerActivity.class);
        // startActivity(intent);
        Toast.makeText(this, "Función de perfil en desarrollo", Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        // Intent intent = new Intent(this, SettingsWorkerActivity.class);
        // startActivity(intent);
        Toast.makeText(this, "Función de configuración en desarrollo", Toast.LENGTH_SHORT).show();
    }

    private void viewMap() {
        Intent intent = new Intent(HomeWorkerActivity.this, MapWorkerActivity.class);
        startActivity(intent);
    }

    private void goToFixDepot() {
        Intent intent = new Intent(HomeWorkerActivity.this, FixDepotActivity.class);
        startActivity(intent);
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

                    LogoutHelper.performLogout(this, mAuthProvider, null, null);
                } catch (Exception e) {
                    Log.e(TAG, "Error durante logout", e);
                    Toast.makeText(this, "Error al cerrar sesión: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "Error al mostrar diálogo: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
        updatePerformanceIndicators();
        updateServicesStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // No desconectar automáticamente cuando la app va a background
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Limpiar listeners
        if (mWorkerDataListener != null && mAuthProvider.getId() != null) {
            mWorkerProvider.removeWorkerListener(mAuthProvider.getId(), mWorkerDataListener);
        }

        // Parar actualizaciones periódicas
        if (mUIUpdateHandler != null) {
            mUIUpdateHandler.removeCallbacksAndMessages(null);
        }

        // Solo desconectar si la app se está cerrando completamente
        if (isConnected && isFinishing()) {
            try {
                stopLocationUpdates();
                updateWorkerOnlineStatus(false);
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
            // Mostrar diálogo de confirmación para salir
            new AlertDialog.Builder(this)
                    .setTitle("Salir de la aplicación")
                    .setMessage("¿Deseas salir? Si estás conectado, se desconectará automáticamente.")
                    .setPositiveButton("Salir", (dialog, which) -> {
                        if (isConnected) {
                            disconnectWorker();
                        }
                        super.onBackPressed();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }
}