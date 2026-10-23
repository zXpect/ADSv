package com.project.ads.activities.client;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DatabaseError;
import com.project.ads.R;
import com.project.ads.helpers.RankingHelper;
import com.project.ads.helpers.CategoryHelper;
import com.project.ads.models.Worker;
import com.project.ads.providers.GeofireProvider;
import com.project.ads.providers.WorkerProvider;
import com.project.ads.helpers.DisplayNameHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Técnicos disponibles · mockup 08 (docs/mockups/Trabajadores.dc.html).
 * Lista de técnicos cercanos de un oficio, ordenada por distancia y paginada.
 * La búsqueda (ubicación + Geofire + datos del trabajador) es la que antes vivía
 * en HomeUserActivity; se movió sin cambiar su lógica.
 */
public class WorkersActivity extends AppCompatActivity {
    private static final String TAG = "WorkersActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 2;

    /** Slug del oficio (CategoryHelper.ALL si no viene). */
    public static final String EXTRA_CATEGORY = "category";
    /** Nombre visible del oficio. */
    public static final String EXTRA_CATEGORY_NAME = "category_name";
    /** Problema elegido en TasksActivity (mockup 07); solo se muestra como título. */
    public static final String EXTRA_TASK_NAME = "task_name";
    /** Texto buscado desde el inicio (nombre u oficio del técnico). */
    public static final String EXTRA_QUERY = "query";

    private static final int SEARCH_RADIUS_KM = 10;
    private static final int WORKERS_PER_PAGE = 10;

    private RecyclerView mWorkersRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayout mEmptyStateLayout;
    private TextView mWorkersCountText;

    private GeofireProvider mGeofireProvider;
    private WorkerProvider mWorkerProvider;
    private FusedLocationProviderClient mFusedLocation;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private FirebaseCrashlytics crashlytics;

    private LatLng mCurrentLatLng;
    private WorkersAdapter mWorkersAdapter;
    private final List<Worker> mAllWorkersList = new ArrayList<>();
    private List<Worker> mFilteredWorkersList = new ArrayList<>();
    private final Map<String, Worker> mWorkersMap = new HashMap<>();
    private String mCurrentFilter = CategoryHelper.ALL;
    private String mSearchQuery = "";
    private boolean mIsFirstTime = true;
    private boolean mLocationPermissionDenied = false;

    private int mDisplayedWorkersCount = 0;
    private boolean mIsLoadingMore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workers);
        crashlytics = FirebaseCrashlytics.getInstance();

        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (category != null) mCurrentFilter = category;
        String query = getIntent().getStringExtra(EXTRA_QUERY);
        if (query != null) mSearchQuery = query.toLowerCase(Locale.ROOT).trim();

        mGeofireProvider = new GeofireProvider();
        mWorkerProvider = new WorkerProvider();

        setupViews();
        setupRecyclerView();
        setupLocationServices();
    }

    private void setupViews() {
        mWorkersRecyclerView = findViewById(R.id.workers_recycler_view);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mEmptyStateLayout = findViewById(R.id.empty_state_text);
        mWorkersCountText = findViewById(R.id.workers_count_text);

        findViewById(R.id.btn_workers_back).setOnClickListener(v -> finish());

        // Encabezado: oficio y problema elegidos (mockup 08), o lo que se buscó
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        String taskName = getIntent().getStringExtra(EXTRA_TASK_NAME);
        TextView title = findViewById(R.id.tv_workers_title);
        if (taskName != null) {
            ((TextView) findViewById(R.id.tv_workers_label)).setText(categoryName);
            title.setText(taskName);
        } else if (!mSearchQuery.isEmpty()) {
            title.setText(getString(R.string.workers_search_title, getIntent().getStringExtra(EXTRA_QUERY)));
        } else {
            title.setText(categoryName != null ? categoryName : getString(R.string.workers_all_title));
        }

        // Lista / Mapa: el mapa es la pantalla del mapa del cliente
        findViewById(R.id.btn_workers_map).setOnClickListener(v ->
                startActivity(new Intent(this, MapClientActivity.class)));

        mSwipeRefreshLayout.setColorSchemeResources(R.color.ads_brand);
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshWorkersList);
    }

    private void setupRecyclerView() {
        mWorkersAdapter = new WorkersAdapter();
        mWorkersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mWorkersRecyclerView.setAdapter(mWorkersAdapter);
    }

    // ---------- Ubicación ----------

    private void setupLocationServices() {
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (mIsFirstTime) {
                        mIsFirstTime = false;
                        loadNearbyWorkers();
                    }
                }
            }
        };
        checkLocationPermissionAndServices();
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
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!enabled) {
            showLocationSettingsDialog();
        } else {
            startLocationUpdates();
        }
    }

    private void showLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Ubicación desactivada")
                .setMessage("Para ver trabajadores cercanos, necesitas activar la ubicación de tu dispositivo.")
                .setPositiveButton("Activar", (dialog, which) -> startActivityForResult(
                        new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LOCATION_SETTINGS_REQUEST_CODE))
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    showToast("No se pueden mostrar trabajadores sin ubicación");
                    mEmptyStateLayout.setVisibility(View.VISIBLE);
                })
                .setCancelable(false)
                .show();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        mSwipeRefreshLayout.setRefreshing(true);
        mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        // Última ubicación conocida para un inicio rápido
        mFusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && mIsFirstTime) {
                mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mIsFirstTime = false;
                loadNearbyWorkers();
            }
        });
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permiso de ubicación requerido")
                    .setMessage("Esta aplicación necesita acceso a la ubicación para mostrar trabajadores cercanos.")
                    .setPositiveButton("Conceder", (d, i) -> ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE))
                    .setNegativeButton("Cancelar", (d, i) -> {
                        mLocationPermissionDenied = true;
                        showToast("Permiso de ubicación denegado");
                        mEmptyStateLayout.setVisibility(View.VISIBLE);
                    })
                    .setCancelable(false)
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    // ---------- Técnicos cercanos ----------

    private void loadNearbyWorkers() {
        if (mCurrentLatLng == null) return;
        mSwipeRefreshLayout.setRefreshing(true);
        mWorkersMap.clear();

        mGeofireProvider.getActiveWorkers(mCurrentLatLng, SEARCH_RADIUS_KM)
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
                    }

                    @Override
                    public void onGeoQueryReady() {
                        mSwipeRefreshLayout.setRefreshing(false);
                        updateWorkersList();
                    }

                    @Override
                    public void onGeoQueryError(DatabaseError error) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        crashlytics.log(TAG + " GeoQuery: " + error.getMessage());
                        showToast("Error al cargar trabajadores");
                    }
                });
    }

    private void loadWorkerData(String workerId, GeoLocation location) {
        mWorkerProvider.getWorker(workerId).addOnSuccessListener(dataSnapshot -> {
            if (!dataSnapshot.exists()) return;
            Worker worker = dataSnapshot.getValue(Worker.class);
            if (!Worker.isSearchable(worker, dataSnapshot.child("isActive").getValue(Boolean.class))) {
                // Pudo verse antes de que lo suspendieran o le quitaran la verificación
                if (mWorkersMap.remove(workerId) != null) updateWorkersList();
                return;
            }
            worker.setId(workerId);
            worker.setLatitude(location.latitude);
            worker.setLongitude(location.longitude);
            if (mCurrentLatLng != null) {
                float[] results = new float[1];
                Location.distanceBetween(mCurrentLatLng.latitude, mCurrentLatLng.longitude,
                        location.latitude, location.longitude, results);
                worker.setDistance(results[0] / 1000);
            }
            mWorkersMap.put(workerId, worker);
            updateWorkersList();
        }).addOnFailureListener(e -> crashlytics.recordException(e));
    }

    private void updateWorkersList() {
        runOnUiThread(() -> {
            mAllWorkersList.clear();
            mAllWorkersList.addAll(mWorkersMap.values());
            if (mDisplayedWorkersCount == 0 && !mAllWorkersList.isEmpty()) {
                mDisplayedWorkersCount = Math.min(WORKERS_PER_PAGE, mAllWorkersList.size());
            }
            filterAndSearchWorkers();
        });
    }

    private void filterAndSearchWorkers() {
        List<Worker> filtered = new ArrayList<>();
        for (Worker worker : mAllWorkersList) {
            boolean matchesFilter = CategoryHelper.matches(worker, mCurrentFilter);
            boolean matchesSearch = mSearchQuery.isEmpty();
            if (!matchesSearch && worker.getName() != null) {
                matchesSearch = worker.getName().toLowerCase(Locale.ROOT).contains(mSearchQuery);
            }
            String categories = worker.getCategoriesDisplay();
            if (!matchesSearch && categories != null) {
                matchesSearch = categories.toLowerCase(Locale.ROOT).contains(mSearchQuery);
            }
            if (matchesFilter && matchesSearch) filtered.add(worker);
        }
        // Ranking orientativo: cercanía y calificación ponderadas (D13, HU-10)
        Collections.sort(filtered, RankingHelper.BY_SCORE);
        mFilteredWorkersList = filtered;

        if (mDisplayedWorkersCount == 0 && !filtered.isEmpty()) {
            mDisplayedWorkersCount = Math.min(WORKERS_PER_PAGE, filtered.size());
        } else if (mDisplayedWorkersCount > filtered.size()) {
            mDisplayedWorkersCount = filtered.size();
        }

        mWorkersCountText.setText(getResources().getQuantityString(
                R.plurals.workers_count, filtered.size(), filtered.size()));
        displayPaginatedWorkers();
    }

    private void displayPaginatedWorkers() {
        int total = mFilteredWorkersList.size();
        int end = Math.min(mDisplayedWorkersCount, total);
        mWorkersAdapter.updateWorkers(new ArrayList<>(mFilteredWorkersList.subList(0, Math.max(end, 0))));
        updatePaginationUI(total);
    }

    private void updatePaginationUI(int totalFiltered) {
        if (totalFiltered == 0) {
            mEmptyStateLayout.setVisibility(mIsFirstTime ? View.GONE : View.VISIBLE);
            mWorkersRecyclerView.setVisibility(View.GONE);
            mWorkersAdapter.clearFooter();
            return;
        }
        mEmptyStateLayout.setVisibility(View.GONE);
        mWorkersRecyclerView.setVisibility(View.VISIBLE);
        if (mIsLoadingMore) {
            mWorkersAdapter.showLoadingFooter();
        } else if (mDisplayedWorkersCount < totalFiltered) {
            mWorkersAdapter.showLoadMoreFooter(totalFiltered - mDisplayedWorkersCount);
        } else if (totalFiltered > WORKERS_PER_PAGE) {
            mWorkersAdapter.showEndFooter();
        } else {
            mWorkersAdapter.clearFooter();
        }
    }

    private void loadMoreWorkers() {
        int total = mFilteredWorkersList.size();
        if (mIsLoadingMore || mDisplayedWorkersCount >= total) return;
        mIsLoadingMore = true;
        mWorkersAdapter.showLoadingFooter();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mDisplayedWorkersCount = Math.min(mDisplayedWorkersCount + WORKERS_PER_PAGE, total);
            mIsLoadingMore = false;
            displayPaginatedWorkers();
        }, 400);
    }

    private void refreshWorkersList() {
        if (mCurrentLatLng == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            showToast("Obteniendo ubicación...");
            return;
        }
        mWorkersMap.clear();
        mAllWorkersList.clear();
        mFilteredWorkersList.clear();
        mDisplayedWorkersCount = 0;
        mIsLoadingMore = false;
        loadNearbyWorkers();
    }

    /** Perfil del técnico (mockup 09); desde ahí se solicita el servicio. */
    private void openProfile(Worker worker) {
        Intent intent = new Intent(this, WorkerProfileActivity.class);
        intent.putExtra(WorkerProfileActivity.EXTRA_WORKER_ID, worker.getId());
        intent.putExtra(WorkerProfileActivity.EXTRA_DISTANCE_KM, worker.getDistance());
        HomeUserActivity.putClientLocation(intent, mCurrentLatLng);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static String initialsOf(String name) {
        StringBuilder initials = new StringBuilder();
        if (name == null) return "";
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) initials.append(Character.toUpperCase(part.charAt(0)));
        }
        return initials.toString();
    }

    // ---------- Adapter ----------

    private class WorkersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_WORKER = 0;
        private static final int VIEW_TYPE_LOADING = 1;
        private static final int VIEW_TYPE_LOAD_MORE = 2;
        private static final int VIEW_TYPE_END = 3;

        private final List<Worker> workers = new ArrayList<>();
        private boolean showLoading, showLoadMore, showEnd;
        private int remainingWorkers;

        void updateWorkers(List<Worker> newWorkers) {
            workers.clear();
            workers.addAll(newWorkers);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (position < workers.size()) return VIEW_TYPE_WORKER;
            if (showLoading) return VIEW_TYPE_LOADING;
            if (showLoadMore) return VIEW_TYPE_LOAD_MORE;
            if (showEnd) return VIEW_TYPE_END;
            return VIEW_TYPE_WORKER;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_LOADING) {
                return new SimpleViewHolder(inflater.inflate(R.layout.item_loading_footer, parent, false));
            } else if (viewType == VIEW_TYPE_LOAD_MORE) {
                return new LoadMoreViewHolder(inflater.inflate(R.layout.item_load_more_footer, parent, false));
            } else if (viewType == VIEW_TYPE_END) {
                return new SimpleViewHolder(inflater.inflate(R.layout.item_end_footer, parent, false));
            }
            return new WorkerViewHolder(inflater.inflate(R.layout.item_worker_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof WorkerViewHolder) {
                ((WorkerViewHolder) holder).bind(workers.get(position));
            } else if (holder instanceof LoadMoreViewHolder) {
                ((LoadMoreViewHolder) holder).bind(remainingWorkers);
            }
        }

        @Override
        public int getItemCount() {
            return workers.size() + (showLoading || showLoadMore || showEnd ? 1 : 0);
        }

        void clearFooter() {
            showLoading = showLoadMore = showEnd = false;
            notifyDataSetChanged();
        }

        void showLoadingFooter() {
            showLoading = true;
            showLoadMore = showEnd = false;
            notifyDataSetChanged();
        }

        void showLoadMoreFooter(int remaining) {
            showLoadMore = true;
            showLoading = showEnd = false;
            remainingWorkers = remaining;
            notifyDataSetChanged();
        }

        void showEndFooter() {
            showEnd = true;
            showLoading = showLoadMore = false;
            notifyDataSetChanged();
        }

        /** Tarjeta del técnico (mockup 08): avatar, nombre, distancia, oficios y calificación. */
        class WorkerViewHolder extends RecyclerView.ViewHolder {
            final TextView workerName, workerWork, workerDistance, workerRating, workerInitials;
            final ImageView workerImage;
            final View cardView;

            WorkerViewHolder(View itemView) {
                super(itemView);
                workerName = itemView.findViewById(R.id.worker_name);
                workerWork = itemView.findViewById(R.id.worker_work);
                workerDistance = itemView.findViewById(R.id.worker_distance);
                workerRating = itemView.findViewById(R.id.worker_rating);
                workerInitials = itemView.findViewById(R.id.worker_initials);
                workerImage = itemView.findViewById(R.id.worker_image);
                cardView = itemView.findViewById(R.id.worker_card);
            }

            void bind(Worker worker) {
                Locale locale = new Locale("es", "CO");
                String name = worker.getName() != null ? worker.getName() : "Trabajador";
                if (worker.getLastName() != null) name = (name + " " + worker.getLastName()).trim();
                workerName.setText(DisplayNameHelper.format(name));
                String categories = worker.getCategoriesDisplay();
                workerWork.setText(categories != null ? categories : "Servicio general");
                workerDistance.setText(String.format(locale, "%.1f km", worker.getDistance()));

                if (worker.getTotalRatings() > 0) {
                    workerRating.setText(getResources().getQuantityString(R.plurals.workers_rating,
                            worker.getTotalRatings(), String.format(locale, "%.1f", worker.getRating()),
                            worker.getTotalRatings()));
                } else {
                    workerRating.setText(R.string.map_worker_new);
                }

                // Foto si existe; si no, iniciales sobre navy
                workerInitials.setText(initialsOf(name));
                if (worker.getImage() != null && !worker.getImage().isEmpty()) {
                    workerImage.setVisibility(View.VISIBLE);
                    Glide.with(WorkersActivity.this).load(worker.getImage()).circleCrop().into(workerImage);
                } else {
                    workerImage.setVisibility(View.GONE);
                }

                cardView.setOnClickListener(v -> openProfile(worker));
            }
        }

        class LoadMoreViewHolder extends RecyclerView.ViewHolder {
            final TextView loadMoreText;

            LoadMoreViewHolder(View itemView) {
                super(itemView);
                loadMoreText = itemView.findViewById(R.id.load_more_text);
                itemView.findViewById(R.id.load_more_card).setOnClickListener(v -> loadMoreWorkers());
            }

            void bind(int remaining) {
                int nextBatch = Math.min(WORKERS_PER_PAGE, remaining);
                loadMoreText.setText(String.format(Locale.ROOT, "Cargar %d más (%d restantes)", nextBatch, remaining));
            }
        }

        class SimpleViewHolder extends RecyclerView.ViewHolder {
            SimpleViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    // ---------- Permisos ----------

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_REQUEST_CODE) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationEnabled();
        } else {
            mLocationPermissionDenied = true;
            showToast("Permiso de ubicación denegado");
            mEmptyStateLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != LOCATION_SETTINGS_REQUEST_CODE) return;
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            startLocationUpdates();
        } else {
            showToast("No se pueden mostrar trabajadores sin ubicación");
            mEmptyStateLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationPermissionDenied && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionDenied = false;
            checkLocationEnabled();
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
