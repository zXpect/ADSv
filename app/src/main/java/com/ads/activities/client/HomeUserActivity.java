package com.ads.activities.client;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ads.activities.MainActivity;
import com.ads.helpers.LogoutHelper;
import com.ads.models.Worker;
import com.ads.providers.AuthProvider;
import com.ads.providers.GeofireProvider;
import com.ads.providers.WorkerProvider;
import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DatabaseError;
import com.project.ads.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeUserActivity extends AppCompatActivity {
    private static final String TAG = "HomeUserActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final int SEARCH_RADIUS = 10;

    // UI Components
    Button mButtonViewMap;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    private RecyclerView mWorkersRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private EditText mSearchBar;
    private Spinner mFilterSpinner;
    private TextView mEmptyStateText;

    // Data and providers
    private AuthProvider mAuthProvider;
    private GeofireProvider mGeofireProvider;
    private WorkerProvider mWorkerProvider;
    private FusedLocationProviderClient mFusedLocation;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    // Firebase
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics mFirebaseAnalytics;

    // Location and workers
    private LatLng mCurrentLatLng;
    private WorkersAdapter mWorkersAdapter;
    private List<Worker> mWorkersList = new ArrayList<>();
    private Map<String, Worker> mWorkersMap = new HashMap<>();
    private String mCurrentFilter = "Todos los servicios";
    private boolean mIsFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Crashlytics y Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        crashlytics = FirebaseCrashlytics.getInstance();

        try {
            setContentView(R.layout.activity_home_user);
            initProviders();
            setupViews();
            setupNavigation();
            setupRecyclerView();
            setupSwipeRefresh();
            setupFilterSpinner();
            setupSearchBar();
            setupMapButton();
            setupStatusBar();
            setupLocationServices();

            // Registrar información del usuario
            if (mAuthProvider != null && mAuthProvider.getId() != null) {
                String userId = mAuthProvider.getId();
                crashlytics.setUserId(userId);
                crashlytics.setCustomKey("user_type", "client");

                Bundle params = new Bundle();
                params.putString("user_id", userId);
                params.putString("login_method", "email");
                mFirebaseAnalytics.logEvent("user_login", params);
            }

        } catch (Exception e) {
            logError("onCreate", e);
        }
    }

    private void setupViews() {
        try {
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            toolbar = findViewById(R.id.toolbar);
            mWorkersRecyclerView = findViewById(R.id.workers_recycler_view);
            mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
            mSearchBar = findViewById(R.id.search_bar);
            mFilterSpinner = findViewById(R.id.filter_spinner);
            mEmptyStateText = findViewById(R.id.empty_state_text);
            setSupportActionBar(toolbar);

        } catch (Exception e) {
            logError("setupViews", e);
        }
    }

    private void setupRecyclerView() {
        try {
            mWorkersAdapter = new WorkersAdapter();
            mWorkersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mWorkersRecyclerView.setAdapter(mWorkersAdapter);
            mWorkersRecyclerView.setHasFixedSize(true);
        } catch (Exception e) {
            logError("setupRecyclerView", e);
        }
    }

    private void setupSwipeRefresh() {
        try {
            mSwipeRefreshLayout.setColorSchemeResources(
                    R.color.colorPrimary,
                    R.color.colorAccent,
                    R.color.colorPrimaryDark
            );
            mSwipeRefreshLayout.setOnRefreshListener(this::refreshWorkersList);
        } catch (Exception e) {
            logError("setupSwipeRefresh", e);
        }
    }

    private void setupFilterSpinner() {
        try {
            List<String> filterOptions = new ArrayList<>();
            filterOptions.add("Todos los servicios");
            filterOptions.addAll(Arrays.asList(getResources().getStringArray(R.array.service_types)));

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    filterOptions
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mFilterSpinner.setAdapter(adapter);

            mFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selected = parent.getItemAtPosition(position).toString();
                    if (!selected.equals(mCurrentFilter)) {
                        mCurrentFilter = selected;
                        filterWorkers();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mCurrentFilter = "Todos los servicios";
                }
            });
        } catch (Exception e) {
            logError("setupFilterSpinner", e);
        }
    }

    private void setupSearchBar() {
        try {
            // Implementar búsqueda en tiempo real si es necesario
            // Por ahora, solo configuración básica
        } catch (Exception e) {
            logError("setupSearchBar", e);
        }
    }

    private void setupLocationServices() {
        try {
            mFusedLocation = LocationServices.getFusedLocationProviderClient(this);

            mLocationRequest = LocationRequest.create()
                    .setInterval(10000)
                    .setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        if (mIsFirstTime) {
                            mIsFirstTime = false;
                            loadNearbyWorkers();
                        }
                    }
                }
            };

            startLocationUpdates();
        } catch (Exception e) {
            logError("setupLocationServices", e);
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permiso de ubicación requerido")
                    .setMessage("Esta aplicación necesita acceso a la ubicación para mostrar trabajadores cercanos")
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        ActivityCompat.requestPermissions(HomeUserActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_REQUEST_CODE);
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        }
    }

    private void loadNearbyWorkers() {
        if (mCurrentLatLng == null) return;

        try {
            mSwipeRefreshLayout.setRefreshing(true);
            mWorkersMap.clear();

            if (mGeofireProvider == null) {
                mGeofireProvider = new GeofireProvider();
            }

            mGeofireProvider.getActiveWorkers(mCurrentLatLng, SEARCH_RADIUS)
                    .addGeoQueryEventListener(new GeoQueryEventListener() {
                        @Override
                        public void onKeyEntered(String key, GeoLocation location) {
                            loadWorkerData(key, location);
                        }

                        @Override
                        public void onKeyExited(String key) {
                            mWorkersMap.remove(key);
                            updateWorkersList();
                        }

                        @Override
                        public void onKeyMoved(String key, GeoLocation location) {
                            // Actualizar ubicación si es necesario
                        }

                        @Override
                        public void onGeoQueryReady() {
                            mSwipeRefreshLayout.setRefreshing(false);
                            updateWorkersList();
                        }

                        @Override
                        public void onGeoQueryError(DatabaseError error) {
                            mSwipeRefreshLayout.setRefreshing(false);
                            logError("GeoQuery", new Exception(error.getMessage()));
                        }
                    });
        } catch (Exception e) {
            mSwipeRefreshLayout.setRefreshing(false);
            logError("loadNearbyWorkers", e);
        }
    }

    private void loadWorkerData(String workerId, GeoLocation location) {
        if (mWorkerProvider == null) {
            mWorkerProvider = new WorkerProvider();
        }

        mWorkerProvider.getWorker(workerId).addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                Worker worker = dataSnapshot.getValue(Worker.class);
                if (worker != null) {
                    worker.setId(workerId);
                    worker.setLatitude(location.latitude);
                    worker.setLongitude(location.longitude);
                    mWorkersMap.put(workerId, worker);
                    updateWorkersList();
                }
            }
        }).addOnFailureListener(exception -> {
            logError("loadWorkerData", exception);
        });
    }

    private void updateWorkersList() {
        runOnUiThread(() -> {
            mWorkersList.clear();
            mWorkersList.addAll(mWorkersMap.values());
            filterWorkers();
        });
    }

    private void filterWorkers() {
        try {
            List<Worker> filteredList = new ArrayList<>();

            for (Worker worker : mWorkersList) {
                if (mCurrentFilter.equals("Todos los servicios") ||
                        (worker.getWork() != null && worker.getWork().equalsIgnoreCase(mCurrentFilter))) {
                    filteredList.add(worker);
                }
            }

            mWorkersAdapter.updateWorkers(filteredList);

            if (filteredList.isEmpty()) {
                mEmptyStateText.setVisibility(View.VISIBLE);
                mWorkersRecyclerView.setVisibility(View.GONE);
                mEmptyStateText.setText("No hay trabajadores disponibles en tu área");
            } else {
                mEmptyStateText.setVisibility(View.GONE);
                mWorkersRecyclerView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            logError("filterWorkers", e);
        }
    }

    private void refreshWorkersList() {
        if (mCurrentLatLng != null) {
            loadNearbyWorkers();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();
        }
    }

    // Adapter para los trabajadores
    private class WorkersAdapter extends RecyclerView.Adapter<WorkersAdapter.WorkerViewHolder> {
        private List<Worker> workers = new ArrayList<>();

        public void updateWorkers(List<Worker> newWorkers) {
            this.workers.clear();
            this.workers.addAll(newWorkers);
            notifyDataSetChanged();
        }

        @Override
        public WorkerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_worker_card, parent, false);
            return new WorkerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(WorkerViewHolder holder, int position) {
            Worker worker = workers.get(position);
            holder.bind(worker);
        }

        @Override
        public int getItemCount() {
            return workers.size();
        }

        class WorkerViewHolder extends RecyclerView.ViewHolder {
            TextView workerName, workerWork, workerDistance, workerRating;
            ImageView workerImage, workerTypeIcon;
            View cardView;

            WorkerViewHolder(View itemView) {
                super(itemView);
                workerName = itemView.findViewById(R.id.worker_name);
                workerWork = itemView.findViewById(R.id.worker_work);
                workerDistance = itemView.findViewById(R.id.worker_distance);
                workerRating = itemView.findViewById(R.id.worker_rating);
                workerImage = itemView.findViewById(R.id.worker_image);
                workerTypeIcon = itemView.findViewById(R.id.worker_type_icon);
                cardView = itemView.findViewById(R.id.worker_card);
            }

            void bind(Worker worker) {
                workerName.setText(worker.getName() != null ? worker.getName() : "Trabajador");
                workerWork.setText(worker.getWork() != null ? worker.getWork() : "Servicio general");

                // Calcular distancia si tenemos ubicación
                if (mCurrentLatLng != null && worker.getLatitude() != 0 && worker.getLongitude() != 0) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            mCurrentLatLng.latitude, mCurrentLatLng.longitude,
                            worker.getLatitude(), worker.getLongitude(),
                            results
                    );
                    float distanceKm = results[0] / 1000;
                    workerDistance.setText(String.format("%.1f km", distanceKm));
                } else {
                    workerDistance.setText("Calculando...");
                }

                // Mostrar rating si está disponible
                if (worker.getRating() > 0) {
                    workerRating.setText(String.format("★ %.1f", worker.getRating()));
                    workerRating.setVisibility(View.VISIBLE);
                } else {
                    workerRating.setVisibility(View.GONE);
                }

                // Cargar imagen del trabajador
                if (worker.getImage() != null && !worker.getImage().isEmpty()) {
                    Glide.with(HomeUserActivity.this)
                            .load(worker.getImage())
                            .placeholder(R.drawable.logo)
                            .error(R.drawable.logo)
                            .circleCrop()
                            .into(workerImage);
                } else {
                    workerImage.setImageResource(R.drawable.logo);
                }

                // Establecer icono según tipo de trabajo
                setWorkerTypeIcon(worker.getWork(), workerTypeIcon);

                // Click listener para abrir detalles del trabajador
                cardView.setOnClickListener(v -> {
                    try {
                        crashlytics.log("Usuario seleccionó trabajador: " + worker.getId());
                        showWorkerDetailsDialog(worker);
                    } catch (Exception e) {
                        logError("workerCardClick", e);
                    }
                });
            }
        }
    }

    private void setWorkerTypeIcon(String workType, ImageView iconView) {
        int iconResource;

        if (workType == null || workType.trim().isEmpty()) {
            iconResource = R.drawable.icon_worker;
        } else {
            switch (workType.toLowerCase().trim()) {
                case "carpintería":
                    iconResource = R.drawable.icon_carpenter;
                    break;
                case "ferretería":
                    iconResource = R.drawable.icon_ferreteria;
                    break;
                case "pintor":
                    iconResource = R.drawable.icon_painter;
                    break;
                case "electricista":
                    iconResource = R.drawable.icon_electrician;
                    break;
                case "plomería":
                    iconResource = R.drawable.icon_plumber;
                    break;
                case "jardinería":
                    iconResource = R.drawable.icon_gardener;
                    break;
                case "albañilería":
                    iconResource = R.drawable.icon_mason;
                    break;
                default:
                    iconResource = R.drawable.icon_worker;
                    break;
            }
        }

        iconView.setImageResource(iconResource);
    }

    private void showWorkerDetailsDialog(Worker worker) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(worker.getName())
                    .setMessage("Servicio: " + worker.getWork() + "\n" +
                            "¿Deseas solicitar este servicio?")
                    .setPositiveButton("Solicitar", (dialog, which) -> {
                        Intent intent = new Intent(HomeUserActivity.this, ServiceRequestActivity.class);
                        intent.putExtra("workerId", worker.getId());
                        startActivity(intent);
                    })
                    .setNeutralButton("Ver en mapa", (dialog, which) -> {
                        Intent intent = new Intent(HomeUserActivity.this, MapClientActivity.class);
                        intent.putExtra("focusWorkerId", worker.getId());
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } catch (Exception e) {
            logError("showWorkerDetailsDialog", e);
        }
    }

    // Métodos existentes (conservados)
    private void logError(String methodName, Exception e) {
        crashlytics.log("Error en " + methodName);
        crashlytics.recordException(e);

        Bundle params = new Bundle();
        params.putString("error_method", methodName);
        params.putString("error_message", e.getMessage());
        params.putString("error_type", e.getClass().getSimpleName());
        mFirebaseAnalytics.logEvent("app_error", params);

        Toast.makeText(this, "Error en la aplicación", Toast.LENGTH_SHORT).show();
    }

    private void setupNavigation() {
        try {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            navigationView.setNavigationItemSelectedListener(item -> {
                try {
                    handleNavigationItemSelected(item);
                } catch (Exception e) {
                    crashlytics.log("Error en navegación: " + item.getTitle());
                    crashlytics.recordException(e);
                }
                return true;
            });
        } catch (Exception e) {
            crashlytics.log("Error al configurar la navegación");
            crashlytics.recordException(e);
        }
    }

    private void handleNavigationItemSelected(MenuItem item) {
        try {
            String itemName = item.getTitle().toString();
            crashlytics.log("Usuario seleccionó: " + itemName);

            Bundle params = new Bundle();
            params.putString("item_name", itemName);
            mFirebaseAnalytics.logEvent("navigation_selected", params);

            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_faq) {
                startActivity(new Intent(this, WebViewActivity.class));
            } else if (id == R.id.action_logout) {
                logout();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
        } catch (Exception e) {
            logError("handleNavigation", e);
        }
    }

    private void setupMapButton() {
        try {
            mButtonViewMap = findViewById(R.id.vermapa);
            mButtonViewMap.setOnClickListener(v -> {
                crashlytics.log("Usuario intentó abrir el mapa");
                viewMapClient();
            });
        } catch (Exception e) {
            crashlytics.log("Error al configurar el botón del mapa");
            crashlytics.recordException(e);
        }
    }

    private void setupStatusBar() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
            }
        } catch (Exception e) {
            crashlytics.log("Error al configurar la barra de estado");
            crashlytics.recordException(e);
        }
    }

    private void initProviders() {
        try {
            mAuthProvider = new AuthProvider();
            mGeofireProvider = new GeofireProvider();
            mWorkerProvider = new WorkerProvider();
        } catch (Exception e) {
            crashlytics.log("Error al inicializar providers");
            crashlytics.recordException(e);
            Toast.makeText(this, "Error al inicializar servicios", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewMapClient() {
        try {
            crashlytics.log("Abriendo MapClientActivity");
            Intent intent = new Intent(this, MapClientActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            crashlytics.recordException(e);
            Toast.makeText(this, "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            crashlytics.recordException(e);
            super.onBackPressed();
        }
    }

    private void logout() {
        try {
            crashlytics.log("Usuario iniciando proceso de logout");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro que deseas cerrar sesión?")
                    .setCancelable(false)
                    .setPositiveButton("Sí", (dialog, which) -> {
                        try {
                            performLogout();
                        } catch (Exception e) {
                            crashlytics.log("Error en proceso de logout");
                            crashlytics.recordException(e);
                            Toast.makeText(this, "Error al cerrar sesión", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss());

            runOnUiThread(() -> {
                try {
                    if (!isFinishing()) {
                        builder.create().show();
                    }
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    Toast.makeText(this, "Error al mostrar diálogo", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            crashlytics.recordException(e);
            Toast.makeText(this, "Error al procesar logout", Toast.LENGTH_SHORT).show();
        }
    }

    private void performLogout() {
        LogoutHelper.performLogout(this, mAuthProvider, crashlytics, mFirebaseAnalytics);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado. No se pueden mostrar trabajadores cercanos.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFusedLocation != null && mLocationCallback != null) {
            mFusedLocation.removeLocationUpdates(mLocationCallback);
        }
    }
}