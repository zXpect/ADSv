package com.ads.activities.client;

import androidx.annotation.RequiresApi;
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ads.helpers.LogoutHelper;
import com.ads.models.Worker;
import com.ads.providers.AuthProvider;
import com.ads.providers.ClientProvider;
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
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 2;

    // UI Components
    LinearLayout mButtonViewMap;
    LinearLayout mButtonMyRequests;
    androidx.cardview.widget.CardView mLoadingMoreCard;
    androidx.cardview.widget.CardView mLoadMoreButton;
    LinearLayout mEndOfResults;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    private RecyclerView mWorkersRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private EditText mSearchBar;
    private Spinner mFilterSpinner;
    private Spinner mRadiusSpinner;
    private LinearLayout mEmptyStateLayout;
    private TextView mWorkersCountText;
    private TextView mLoadedWorkersCount;
    private TextView mLoadMoreText;
    private TextView mCurrentRadiusText;
    private TextView mUserGreetingText;
    private ImageView mManualRefreshButton;
    private ImageView mNavHeaderProfileImage;
    private TextView mNavHeaderName;
    private TextView mNavHeaderEmail;

    // Data and providers
    private AuthProvider mAuthProvider;
    private GeofireProvider mGeofireProvider;
    private WorkerProvider mWorkerProvider;
    private ClientProvider mClientProvider;
    private FusedLocationProviderClient mFusedLocation;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    // Firebase
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics mFirebaseAnalytics;

    // Location and workers
    private LatLng mCurrentLatLng;
    private WorkersAdapter mWorkersAdapter;
    private List<Worker> mAllWorkersList = new ArrayList<>();
    private List<Worker> mFilteredWorkersList = new ArrayList<>();
    private Map<String, Worker> mWorkersMap = new HashMap<>();
    private String mCurrentFilter = "Todos los servicios";
    private String mSearchQuery = "";
    private int mCurrentSearchRadius = 10;
    private boolean mIsFirstTime = true;
    private boolean mLocationPermissionDenied = false;

    // Pagination
    private static final int WORKERS_PER_PAGE = 10;
    private int mDisplayedWorkersCount = 0;
    private boolean mIsLoadingMore = false;

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
            loadUserName();
            setupNavigation();
            setupRecyclerView();
            setupSwipeRefresh();
            setupFilterSpinner();
            setupRadiusSpinner();
            setupSearchBar();
            setupClickListeners();
            setupStatusBar();
            setupLocationServices();

            // Animación de entrada
            animateEntranceEffects();

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
            mRadiusSpinner = findViewById(R.id.radius_spinner);
            mEmptyStateLayout = findViewById(R.id.empty_state_text);
            mButtonViewMap = findViewById(R.id.vermapa);
            mButtonMyRequests = findViewById(R.id.my_requests_button);
            mWorkersCountText = findViewById(R.id.workers_count_text);
            mLoadedWorkersCount = findViewById(R.id.loaded_workers_count);
            mCurrentRadiusText = findViewById(R.id.current_radius_text);
            mUserGreetingText = findViewById(R.id.user_greeting_text);

            setSupportActionBar(toolbar);

        } catch (Exception e) {
            logError("setupViews", e);
        }
    }

    private void setupRecyclerView() {
        try {
            mWorkersAdapter = new WorkersAdapter();
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            mWorkersRecyclerView.setLayoutManager(layoutManager);
            mWorkersRecyclerView.setAdapter(mWorkersAdapter);
            mWorkersRecyclerView.setHasFixedSize(false);

            mWorkersRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    if (dy > 0 && getCurrentFocus() != null) {
                        android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                }
            });

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

    private void loadUserName() {
        try {
            if (mAuthProvider != null && mAuthProvider.getId() != null) {
                String userId = mAuthProvider.getId();

                if (mClientProvider == null) {
                    mClientProvider = new ClientProvider();
                }

                // Usar el método getDatabase() de ClientProvider para acceder a la referencia
                mClientProvider.mDataBase.child(userId).get()
                        .addOnSuccessListener(dataSnapshot -> {
                            if (dataSnapshot.exists()) {
                                // Obtener el nombre del cliente
                                String name = dataSnapshot.child("name").getValue(String.class);

                                if (name != null && !name.isEmpty()) {

                                    String firstName = name.trim().split(" ")[0];
                                    mUserGreetingText.setText("Hola, " + firstName + "!");

                                    crashlytics.log("Nombre de usuario cargado: " + firstName);
                                } else {
                                    mUserGreetingText.setText("Hola!");
                                }
                            } else {
                                crashlytics.log("No se encontró información del usuario");
                                mUserGreetingText.setText("Hola!");
                            }
                        })
                        .addOnFailureListener(e -> {
                            crashlytics.log("Error al cargar nombre de usuario: " + e.getMessage());
                            crashlytics.recordException(e);
                            logError("loadUserName", e);
                            mUserGreetingText.setText("Hola!");
                        });
            } else {
                mUserGreetingText.setText("Hola!");
            }
        } catch (Exception e) {
            logError("loadUserName", e);
            mUserGreetingText.setText("Hola!");
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

            final boolean[] isInitializing = {true};

            mFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isInitializing[0]) {
                        isInitializing[0] = false;
                        return;
                    }

                    String selected = parent.getItemAtPosition(position).toString();
                    if (!selected.equals(mCurrentFilter)) {
                        mCurrentFilter = selected;
                        crashlytics.log("Filtro cambiado a: " + selected);

                        // Reset pagination y aplicar filtro
                        mDisplayedWorkersCount = 0;
                        mIsLoadingMore = false;

                        filterAndSearchWorkers();
                        showInfoToast("Filtro: " + selected);
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

    private void setupRadiusSpinner() {
        try {
            List<String> radiusOptions = Arrays.asList(
                    "5 km", "10 km", "15 km", "20 km", "30 km", "50 km"
            );

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    radiusOptions
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mRadiusSpinner.setAdapter(adapter);
            mRadiusSpinner.setSelection(1); // 10 km por defecto

            final boolean[] isInitializing = {true};

            mRadiusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isInitializing[0]) {
                        isInitializing[0] = false;
                        return;
                    }

                    String selected = parent.getItemAtPosition(position).toString();
                    int newRadius = Integer.parseInt(selected.replace(" km", ""));

                    if (newRadius != mCurrentSearchRadius) {
                        mCurrentSearchRadius = newRadius;
                        mCurrentRadiusText.setText(selected);

                        mCurrentRadiusText.animate()
                                .scaleX(1.3f)
                                .scaleY(1.3f)
                                .setDuration(200)
                                .withEndAction(() -> mCurrentRadiusText.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(200)
                                        .start())
                                .start();

                        showInfoToast("Radio actualizado a " + selected);
                        crashlytics.log("Radio cambiado a: " + newRadius + " km");

                        // CORRECCIÓN: Limpiar todo antes de recargar
                        mWorkersMap.clear();
                        mAllWorkersList.clear();
                        mFilteredWorkersList.clear();
                        mDisplayedWorkersCount = 0;

                        refreshWorkersList();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } catch (Exception e) {
            logError("setupRadiusSpinner", e);
        }
    }

    private void setupSearchBar() {
        try {
            mSearchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mSearchQuery = s.toString().toLowerCase().trim();

                    crashlytics.log(String.format(
                            "Búsqueda cambiada a: '%s'",
                            mSearchQuery
                    ));

                    mDisplayedWorkersCount = 0;
                    mIsLoadingMore = false;

                    filterAndSearchWorkers();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } catch (Exception e) {
            logError("setupSearchBar", e);
        }
    }

    private void setupClickListeners() {
        try {
            mButtonViewMap.setOnClickListener(v -> {
                animateClick(v);
                viewMapClient();
            });

            mButtonMyRequests.setOnClickListener(v -> {
                animateClick(v);
                showInfoToast("Función en desarrollo");
            });

        } catch (Exception e) {
            logError("setupClickListeners", e);
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
                mSwipeRefreshLayout,
                mButtonViewMap,
                mButtonMyRequests
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

            // Verificar y solicitar permisos
            checkLocationPermissionAndServices();
        } catch (Exception e) {
            logError("setupLocationServices", e);
        }
    }

    private void checkLocationPermissionAndServices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            checkLocationEnabled();
        }
    }

    private void checkLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            showLocationSettingsDialog();
        } else {
            startLocationUpdates();
        }
    }

    private void showLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Ubicación desactivada")
                .setMessage("Para ver trabajadores cercanos, necesitas activar la ubicación de tu dispositivo.")
                .setPositiveButton("Activar", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    showErrorToast("No se pueden mostrar trabajadores sin ubicación");
                    mEmptyStateLayout.setVisibility(View.VISIBLE);
                })
                .setCancelable(false)
                .show();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());

            // Obtener última ubicación conocida para inicio rápido
            mFusedLocation.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && mIsFirstTime) {
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mIsFirstTime = false;
                    loadNearbyWorkers();
                }
            });
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permiso de ubicación requerido")
                    .setMessage("Esta aplicación necesita acceso a la ubicación para mostrar trabajadores cercanos. Sin este permiso no podrás usar la aplicación correctamente.")
                    .setPositiveButton("Conceder", (dialogInterface, i) -> {
                        ActivityCompat.requestPermissions(HomeUserActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancelar", (dialogInterface, i) -> {
                        mLocationPermissionDenied = true;
                        showErrorToast("Permiso de ubicación denegado");
                        mEmptyStateLayout.setVisibility(View.VISIBLE);
                    })
                    .setCancelable(false)
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        }
    }

    private void loadNearbyWorkers() {
        if (mCurrentLatLng == null) {
            crashlytics.log("loadNearbyWorkers: mCurrentLatLng es null");
            return;
        }

        try {
            crashlytics.log("=== INICIANDO CARGA DE TRABAJADORES ===");
            crashlytics.log("Ubicación: " + mCurrentLatLng.latitude + ", " + mCurrentLatLng.longitude);
            crashlytics.log("Radio: " + mCurrentSearchRadius + " km");

            mSwipeRefreshLayout.setRefreshing(true);

            // CORRECCIÓN CRÍTICA: NO limpiar las listas aquí
            // Solo limpiar el Map para recibir nuevos datos
            mWorkersMap.clear();

            if (mGeofireProvider == null) {
                mGeofireProvider = new GeofireProvider();
            }

            mGeofireProvider.getActiveWorkers(mCurrentLatLng, mCurrentSearchRadius)
                    .addGeoQueryEventListener(new GeoQueryEventListener() {
                        @Override
                        public void onKeyEntered(String key, GeoLocation location) {
                            crashlytics.log("Worker encontrado: " + key);
                            loadWorkerData(key, location);
                        }

                        @Override
                        public void onKeyExited(String key) {
                            crashlytics.log("Worker salió del radio: " + key);
                            mWorkersMap.remove(key);
                            updateWorkersList();
                        }

                        @Override
                        public void onKeyMoved(String key, GeoLocation location) {
                            // Actualizar ubicación si es necesario
                        }

                        @Override
                        public void onGeoQueryReady() {
                            crashlytics.log("GeoQuery completado. Total workers encontrados: " + mWorkersMap.size());
                            mSwipeRefreshLayout.setRefreshing(false);
                            updateWorkersList();
                        }

                        @Override
                        public void onGeoQueryError(DatabaseError error) {
                            crashlytics.log("Error en GeoQuery: " + error.getMessage());
                            mSwipeRefreshLayout.setRefreshing(false);
                            logError("GeoQuery", new Exception(error.getMessage()));
                            showErrorToast("Error al cargar trabajadores");
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

                    if (mCurrentLatLng != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                mCurrentLatLng.latitude, mCurrentLatLng.longitude,
                                location.latitude, location.longitude,
                                results
                        );
                        worker.setDistance(results[0] / 1000);
                    }

                    crashlytics.log("Worker cargado: " + worker.getName() + " - " + worker.getWork());
                    mWorkersMap.put(workerId, worker);

                    // CORRECCIÓN: Actualizar la lista inmediatamente después de agregar
                    updateWorkersList();
                }
            }
        }).addOnFailureListener(exception -> {
            crashlytics.log("Error al cargar worker " + workerId + ": " + exception.getMessage());
            logError("loadWorkerData", exception);
        });
    }

    private void updateWorkersList() {
        runOnUiThread(() -> {
            crashlytics.log(String.format(
                    "=== updateWorkersList ===\nWorkersMap: %d\nmAllWorkersList antes: %d",
                    mWorkersMap.size(),
                    mAllWorkersList.size()
            ));

            // CORRECCIÓN: Actualizar la lista principal con todos los workers del Map
            mAllWorkersList.clear();
            mAllWorkersList.addAll(mWorkersMap.values());

            crashlytics.log("mAllWorkersList después: " + mAllWorkersList.size());

            updateWorkersCountDisplay();

            // CORRECCIÓN: Inicializar displayCount solo si está en 0 Y hay workers
            if (mDisplayedWorkersCount == 0 && mAllWorkersList.size() > 0) {
                mDisplayedWorkersCount = Math.min(WORKERS_PER_PAGE, mAllWorkersList.size());
                crashlytics.log("mDisplayedWorkersCount inicializado a: " + mDisplayedWorkersCount);
            }

            filterAndSearchWorkers();
        });
    }

    private void updateWorkersCountDisplay() {
        int totalWorkers = mAllWorkersList.size();
        mWorkersCountText.setText(String.valueOf(totalWorkers));

        Bundle params = new Bundle();
        params.putInt("total_workers", totalWorkers);
        params.putInt("search_radius", mCurrentSearchRadius);
        mFirebaseAnalytics.logEvent("workers_loaded", params);
    }

    private void filterAndSearchWorkers() {
        try {
            crashlytics.log("=== filterAndSearchWorkers ===");
            crashlytics.log("Total workers: " + mAllWorkersList.size());
            crashlytics.log("Filtro: " + mCurrentFilter);
            crashlytics.log("Búsqueda: " + mSearchQuery);

            List<Worker> filteredList = new ArrayList<>();

            for (Worker worker : mAllWorkersList) {
                boolean matchesFilter = false;

                if (mCurrentFilter.equals("Todos los servicios")) {
                    matchesFilter = true;
                } else if (worker.getWork() != null) {
                    String workerWork = worker.getWork().trim();
                    String currentFilter = mCurrentFilter.trim();
                    matchesFilter = workerWork.equalsIgnoreCase(currentFilter);
                }

                boolean matchesSearch = mSearchQuery.isEmpty();
                if (!matchesSearch) {
                    if (worker.getName() != null) {
                        matchesSearch = worker.getName().toLowerCase().contains(mSearchQuery);
                    }
                    if (!matchesSearch && worker.getWork() != null) {
                        matchesSearch = worker.getWork().toLowerCase().contains(mSearchQuery);
                    }
                }

                if (matchesFilter && matchesSearch) {
                    filteredList.add(worker);
                }
            }

            // Ordenar por distancia
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                filteredList.sort((w1, w2) -> Float.compare(w1.getDistance(), w2.getDistance()));
            }

            mFilteredWorkersList = filteredList;

            crashlytics.log("Filtrados: " + filteredList.size());

            // CORRECCIÓN: Ajustar displayCount según los resultados filtrados
            if (mDisplayedWorkersCount == 0 && filteredList.size() > 0) {
                mDisplayedWorkersCount = Math.min(WORKERS_PER_PAGE, filteredList.size());
            } else if (mDisplayedWorkersCount > filteredList.size()) {
                mDisplayedWorkersCount = filteredList.size();
            }

            crashlytics.log("Mostrando: " + mDisplayedWorkersCount);

            displayPaginatedWorkers();

        } catch (Exception e) {
            logError("filterAndSearchWorkers", e);
        }
    }

    private void displayPaginatedWorkers() {
        try {
            int totalFiltered = mFilteredWorkersList.size();

            crashlytics.log(String.format(
                    "=== displayPaginatedWorkers ===\nMostrando: %d\nTotal filtrados: %d",
                    mDisplayedWorkersCount,
                    totalFiltered
            ));

            if (mDisplayedWorkersCount > totalFiltered) {
                mDisplayedWorkersCount = totalFiltered;
            }

            List<Worker> workersToDisplay = new ArrayList<>();
            if (mDisplayedWorkersCount > 0 && totalFiltered > 0) {
                // CORRECCIÓN: Asegurar que el índice no exceda el tamaño
                int endIndex = Math.min(mDisplayedWorkersCount, totalFiltered);
                workersToDisplay = new ArrayList<>(
                        mFilteredWorkersList.subList(0, endIndex)
                );
            }

            crashlytics.log("Enviando al adapter: " + workersToDisplay.size() + " trabajadores");

            mWorkersAdapter.updateWorkers(workersToDisplay);
            mLoadedWorkersCount.setText(String.valueOf(workersToDisplay.size()));

            updatePaginationUI(totalFiltered);

            Bundle params = new Bundle();
            params.putInt("displayed", workersToDisplay.size());
            params.putInt("total_filtered", totalFiltered);
            params.putString("filter", mCurrentFilter);
            mFirebaseAnalytics.logEvent("pagination_display", params);

        } catch (Exception e) {
            logError("displayPaginatedWorkers", e);
        }
    }

    private void updatePaginationUI(int totalFiltered) {
        runOnUiThread(() -> {
            try {
                if (totalFiltered == 0) {
                    mEmptyStateLayout.setVisibility(View.VISIBLE);
                    mWorkersRecyclerView.setVisibility(View.GONE);
                    mWorkersAdapter.clearFooter();
                    return;
                }

                mEmptyStateLayout.setVisibility(View.GONE);
                mWorkersRecyclerView.setVisibility(View.VISIBLE);

                if (mIsLoadingMore) {
                    mWorkersAdapter.showLoadingFooter();
                    return;
                }

                if (mDisplayedWorkersCount < totalFiltered) {
                    int remaining = totalFiltered - mDisplayedWorkersCount;
                    mWorkersAdapter.showLoadMoreFooter(remaining);
                    return;
                }

                if (totalFiltered > WORKERS_PER_PAGE) {
                    mWorkersAdapter.showEndFooter();
                } else {
                    mWorkersAdapter.clearFooter();
                }
            } catch (Exception e) {
                logError("updatePaginationUI", e);
            }
        });
    }

    private void loadMoreWorkers() {
        if (mIsLoadingMore) {
            crashlytics.log("loadMoreWorkers: Ya está cargando");
            return;
        }

        int totalFiltered = mFilteredWorkersList.size();
        if (mDisplayedWorkersCount >= totalFiltered) {
            crashlytics.log("loadMoreWorkers: Ya se mostraron todos");
            updatePaginationUI(totalFiltered);
            return;
        }

        try {
            mIsLoadingMore = true;
            int previousCount = mDisplayedWorkersCount;

            crashlytics.log(String.format(
                    "loadMoreWorkers: Mostrando=%d, Total=%d",
                    previousCount,
                    totalFiltered
            ));

            mWorkersAdapter.showLoadingFooter();

            new android.os.Handler().postDelayed(() -> {
                try {
                    int newTotal = Math.min(
                            mDisplayedWorkersCount + WORKERS_PER_PAGE,
                            totalFiltered
                    );

                    mDisplayedWorkersCount = newTotal;
                    mIsLoadingMore = false;

                    displayPaginatedWorkers();

                    if (previousCount > 0 && previousCount < mDisplayedWorkersCount) {
                        mWorkersRecyclerView.postDelayed(() -> {
                            mWorkersRecyclerView.smoothScrollToPosition(previousCount);
                        }, 100);
                    }

                    int loaded = mDisplayedWorkersCount - previousCount;
                    showSuccessToast(String.format("%d trabajadores más cargados", loaded));

                    crashlytics.log(String.format(
                            "loadMoreWorkers completado: Ahora mostrando %d de %d",
                            mDisplayedWorkersCount,
                            totalFiltered
                    ));

                } catch (Exception e) {
                    mIsLoadingMore = false;
                    logError("loadMoreWorkers_handler", e);
                    showErrorToast("Error al cargar más trabajadores");
                    displayPaginatedWorkers();
                }

            }, 800);

        } catch (Exception e) {
            mIsLoadingMore = false;
            logError("loadMoreWorkers", e);
            showErrorToast("Error al cargar más trabajadores");
            displayPaginatedWorkers();
        }
    }

    private void resetPagination() {
        crashlytics.log("resetPagination llamado");

        int totalFiltered = mFilteredWorkersList.size();
        if (totalFiltered > 0) {
            mDisplayedWorkersCount = Math.min(WORKERS_PER_PAGE, totalFiltered);
        } else {
            mDisplayedWorkersCount = 0;
        }

        mIsLoadingMore = false;

        if (mWorkersAdapter != null) {
            mWorkersAdapter.clearFooter();
        }

        crashlytics.log(String.format(
                "Paginación reseteada: mDisplayedWorkersCount=%d",
                mDisplayedWorkersCount
        ));
    }

    private void refreshWorkersList() {
        if (mCurrentLatLng != null) {
            // CORRECCIÓN: Limpiar todo antes de recargar
            mWorkersMap.clear();
            mAllWorkersList.clear();
            mFilteredWorkersList.clear();
            mDisplayedWorkersCount = 0;
            mIsLoadingMore = false;

            loadNearbyWorkers();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
            showInfoToast("Obteniendo ubicación...");
        }
    }

    // Adapter para los trabajadores
    private class WorkersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_WORKER = 0;
        private static final int VIEW_TYPE_LOADING = 1;
        private static final int VIEW_TYPE_LOAD_MORE = 2;
        private static final int VIEW_TYPE_END = 3;

        private List<Worker> workers = new ArrayList<>();
        private boolean showLoading = false;
        private boolean showLoadMore = false;
        private boolean showEnd = false;
        private int remainingWorkers = 0;

        public void updateWorkers(List<Worker> newWorkers) {
            crashlytics.log("Adapter.updateWorkers: Recibiendo " + newWorkers.size() + " trabajadores");
            this.workers.clear();
            this.workers.addAll(newWorkers);
            notifyDataSetChanged();
            crashlytics.log("Adapter.updateWorkers: Lista actualizada, total items: " + getItemCount());
        }

        @Override
        public int getItemViewType(int position) {
            if (position < workers.size()) {
                return VIEW_TYPE_WORKER;
            } else if (showLoading) {
                return VIEW_TYPE_LOADING;
            } else if (showLoadMore) {
                return VIEW_TYPE_LOAD_MORE;
            } else if (showEnd) {
                return VIEW_TYPE_END;
            }
            return VIEW_TYPE_WORKER;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_loading_footer, parent, false);
                return new LoadingViewHolder(view);
            } else if (viewType == VIEW_TYPE_LOAD_MORE) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_load_more_footer, parent, false);
                return new LoadMoreViewHolder(view);
            } else if (viewType == VIEW_TYPE_END) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_end_footer, parent, false);
                return new EndViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_worker_card, parent, false);
                return new WorkerViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof WorkerViewHolder) {
                Worker worker = workers.get(position);
                ((WorkerViewHolder) holder).bind(worker, position);
            } else if (holder instanceof LoadMoreViewHolder) {
                ((LoadMoreViewHolder) holder).bind(remainingWorkers);
            }
        }

        @Override
        public int getItemCount() {
            int count = workers.size();
            if (showLoading || showLoadMore || showEnd) {
                count++;
            }
            return count;
        }

        // ViewHolder para trabajadores
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

            void bind(Worker worker, int position) {
                cardView.setAlpha(0f);
                cardView.setTranslationY(30f);
                cardView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setStartDelay(position * 50L)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();

                workerName.setText(worker.getName() != null ? worker.getName() : "Trabajador");
                workerWork.setText(worker.getWork() != null ? worker.getWork() : "Servicio general");
                workerDistance.setText(String.format("%.1f km", worker.getDistance()));

                if (worker.getRating() > 0) {
                    workerRating.setText(String.format("★ %.1f", worker.getRating()));
                    workerRating.setVisibility(View.VISIBLE);
                } else {
                    workerRating.setVisibility(View.GONE);
                }

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

                setWorkerTypeIcon(worker.getWork(), workerTypeIcon);

                cardView.setOnClickListener(v -> {
                    try {
                        animateClick(v);
                        crashlytics.log("Usuario seleccionó trabajador: " + worker.getId());
                        showWorkerDetailsDialog(worker);
                    } catch (Exception e) {
                        logError("workerCardClick", e);
                    }
                });
            }
        }

        class LoadingViewHolder extends RecyclerView.ViewHolder {
            LoadingViewHolder(View itemView) {
                super(itemView);
            }
        }

        class LoadMoreViewHolder extends RecyclerView.ViewHolder {
            TextView loadMoreText;
            View loadMoreButton;

            LoadMoreViewHolder(View itemView) {
                super(itemView);
                loadMoreButton = itemView.findViewById(R.id.load_more_card);
                loadMoreText = itemView.findViewById(R.id.load_more_text);

                loadMoreButton.setOnClickListener(v -> {
                    animateClick(v);
                    loadMoreWorkers();
                });
            }

            void bind(int remaining) {
                int nextBatch = Math.min(WORKERS_PER_PAGE, remaining);
                String text = String.format("Cargar %d más (%d restantes)", nextBatch, remaining);
                loadMoreText.setText(text);
            }
        }

        class EndViewHolder extends RecyclerView.ViewHolder {
            EndViewHolder(View itemView) {
                super(itemView);
            }
        }

        public void clearFooter() {
            showLoading = false;
            showLoadMore = false;
            showEnd = false;
            notifyDataSetChanged();
        }

        public void showLoadingFooter() {
            showLoading = true;
            showLoadMore = false;
            showEnd = false;
            notifyDataSetChanged();
        }

        public void showLoadMoreFooter(int remaining) {
            showLoading = false;
            showLoadMore = true;
            showEnd = false;
            this.remainingWorkers = remaining;
            notifyDataSetChanged();
        }

        public void showEndFooter() {
            showLoading = false;
            showLoadMore = false;
            showEnd = true;
            notifyDataSetChanged();
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
            String message = "Servicio: " + worker.getWork() + "\n" +
                    "Distancia: " + String.format("%.1f km", worker.getDistance()) + "\n" +
                    "¿Deseas solicitar este servicio?";

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(worker.getName())
                    .setMessage(message)
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

    private void logError(String methodName, Exception e) {
        crashlytics.log("Error en " + methodName);
        crashlytics.recordException(e);

        Bundle params = new Bundle();
        params.putString("error_method", methodName);
        params.putString("error_message", e.getMessage());
        params.putString("error_type", e.getClass().getSimpleName());
        mFirebaseAnalytics.logEvent("app_error", params);

        showErrorToast("Error en la aplicación");
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

            // Configurar el header del navigation drawer
            setupNavigationHeader();

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
    private void setupNavigationHeader() {
        try {
            View headerView = navigationView.getHeaderView(0);
            mNavHeaderProfileImage = headerView.findViewById(R.id.nav_header_profile_image);
            mNavHeaderName = headerView.findViewById(R.id.nav_header_name);
            mNavHeaderEmail = headerView.findViewById(R.id.nav_header_email);

            loadUserDataToNavHeader();
        } catch (Exception e) {
            crashlytics.log("Error al configurar navigation header");
            crashlytics.recordException(e);
            logError("setupNavigationHeader", e);
        }
    }
    private void loadUserDataToNavHeader() {
        try {
            if (mAuthProvider == null || mAuthProvider.getId() == null) {
                crashlytics.log("loadUserDataToNavHeader: AuthProvider o userId es null");
                setDefaultNavHeaderValues();
                return;
            }

            String userId = mAuthProvider.getId();

            // Cargar el email desde FirebaseAuth
            if (mAuthProvider.mAuth.getCurrentUser() != null &&
                    mAuthProvider.mAuth.getCurrentUser().getEmail() != null) {
                String email = mAuthProvider.mAuth.getCurrentUser().getEmail();
                mNavHeaderEmail.setText(email);
                crashlytics.log("Email cargado en nav header: " + email);
            } else {
                mNavHeaderEmail.setText("usuario@email.com");
            }

            // Cargar nombre, apellido y foto desde ClientProvider
            if (mClientProvider == null) {
                mClientProvider = new ClientProvider();
            }

            mClientProvider.mDataBase.child(userId).get()
                    .addOnSuccessListener(dataSnapshot -> {
                        if (dataSnapshot.exists()) {
                            // Cargar nombre y apellido
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String lastName = dataSnapshot.child("lastName").getValue(String.class);

                            StringBuilder fullName = new StringBuilder();
                            if (name != null && !name.isEmpty()) {
                                fullName.append(name);
                            }
                            if (lastName != null && !lastName.isEmpty()) {
                                if (fullName.length() > 0) {
                                    fullName.append(" ");
                                }
                                fullName.append(lastName);
                            }

                            if (fullName.length() > 0) {
                                mNavHeaderName.setText(fullName.toString());
                                crashlytics.log("Nombre completo cargado en nav header: " + fullName);
                            } else {
                                mNavHeaderName.setText("Usuario");
                            }

                            // Cargar foto de perfil
                            String imageUrl = dataSnapshot.child("image").getValue(String.class);
                            loadNavHeaderImage(imageUrl);

                        } else {
                            crashlytics.log("No se encontró información del usuario en la base de datos");
                            setDefaultNavHeaderValues();
                        }
                    })
                    .addOnFailureListener(e -> {
                        crashlytics.log("Error al cargar datos del usuario en nav header: " + e.getMessage());
                        crashlytics.recordException(e);
                        logError("loadUserDataToNavHeader", e);
                        setDefaultNavHeaderValues();
                    });

        } catch (Exception e) {
            logError("loadUserDataToNavHeader", e);
            setDefaultNavHeaderValues();
        }
    }

    private void loadNavHeaderImage(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.logo)
                        .error(R.drawable.logo)
                        .circleCrop()
                        .into(mNavHeaderProfileImage);
                crashlytics.log("Foto de perfil cargada en nav header");
            } else {
                // Si no hay imagen, usar el logo por defecto
                mNavHeaderProfileImage.setImageResource(R.drawable.logo);
                crashlytics.log("No hay imagen de perfil, usando logo por defecto");
            }
        } catch (Exception e) {
            logError("loadNavHeaderImage", e);
            mNavHeaderProfileImage.setImageResource(R.drawable.logo);
        }
    }

    private void setDefaultNavHeaderValues() {
        if (mNavHeaderName != null) {
            mNavHeaderName.setText("Usuario");
        }
        if (mNavHeaderEmail != null) {
            // Intentar obtener el email de FirebaseAuth como último recurso
            if (mAuthProvider != null &&
                    mAuthProvider.mAuth.getCurrentUser() != null &&
                    mAuthProvider.mAuth.getCurrentUser().getEmail() != null) {
                mNavHeaderEmail.setText(mAuthProvider.mAuth.getCurrentUser().getEmail());
            } else {
                mNavHeaderEmail.setText("usuario@email.com");
            }
        }
        if (mNavHeaderProfileImage != null) {
            mNavHeaderProfileImage.setImageResource(R.drawable.logo);
        }
    }

    public void refreshNavHeader() {
        if (mNavHeaderProfileImage != null && mNavHeaderName != null && mNavHeaderEmail != null) {
            loadUserDataToNavHeader();
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
                showInfoToast("Ya estás en Inicio");
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileClientActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else if (id == R.id.nav_settings) {
                showInfoToast("Función de configuración en desarrollo");
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

    private void setupStatusBar() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.setStatusBarColor(getResources().getColor(R.color.colorPrimary));
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
            mClientProvider = new ClientProvider();
        } catch (Exception e) {
            crashlytics.log("Error al inicializar providers");
            crashlytics.recordException(e);
            showErrorToast("Error al inicializar servicios");
        }
    }

    private void viewMapClient() {
        try {
            crashlytics.log("Abriendo MapClientActivity");
            Intent intent = new Intent(this, MapClientActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            crashlytics.recordException(e);
            showErrorToast("No se pudo abrir el mapa");
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Salir de la aplicación")
                        .setMessage("¿Deseas salir?")
                        .setPositiveButton("Salir", (dialog, which) -> {
                            finish();
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
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
                            showErrorToast("Error al cerrar sesión");
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
                    showErrorToast("Error al mostrar diálogo");
                }
            });
        } catch (Exception e) {
            crashlytics.recordException(e);
            showErrorToast("Error al procesar logout");
        }
    }

    private void performLogout() {
        LogoutHelper.performLogout(this, mAuthProvider, crashlytics, mFirebaseAnalytics);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                crashlytics.log("Permiso de ubicación concedido");
                checkLocationEnabled();
            } else {
                mLocationPermissionDenied = true;
                crashlytics.log("Permiso de ubicación denegado");

                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Usuario marcó "No volver a preguntar"
                    new AlertDialog.Builder(this)
                            .setTitle("Permiso requerido")
                            .setMessage("Para usar esta aplicación necesitas conceder el permiso de ubicación desde la configuración del dispositivo.")
                            .setPositiveButton("Ir a configuración", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> {
                                showErrorToast("No se pueden mostrar trabajadores sin ubicación");
                                mEmptyStateLayout.setVisibility(View.VISIBLE);
                            })
                            .show();
                } else {
                    showErrorToast("Permiso de ubicación denegado");
                    mEmptyStateLayout.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            // Verificar si el usuario activó la ubicación
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isLocationEnabled) {
                crashlytics.log("Usuario activó la ubicación");
                showSuccessToast("Ubicación activada");
                startLocationUpdates();
            } else {
                crashlytics.log("Usuario no activó la ubicación");
                showErrorToast("No se pueden mostrar trabajadores sin ubicación");
                mEmptyStateLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Verificar si se concedieron permisos mientras la app estaba en segundo plano
        if (mLocationPermissionDenied) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionDenied = false;
                checkLocationEnabled();
            }
        }

        // Solo actualizar contadores si ya tenemos ubicación y no es la primera vez
        if (mCurrentLatLng != null && !mIsFirstTime) {
            updateWorkersCountDisplay();
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