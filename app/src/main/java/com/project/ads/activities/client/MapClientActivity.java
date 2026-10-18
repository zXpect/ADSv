package com.project.ads.activities.client;

import com.project.ads.helpers.CategoryHelper;
import com.project.ads.models.api.CatalogData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.widget.Toast;

import com.project.ads.activities.MainActivity;
import com.project.ads.models.Worker;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.GeofireProvider;
import com.project.ads.providers.TokenProvider;
import com.project.ads.providers.WorkerProvider;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseError;
import com.project.ads.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class MapClientActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapClientActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 17f;
    private static final int SEARCH_RADIUS = 10;

    private GoogleMap mMap;
    private AuthProvider mAuthProvider;
    private GeofireProvider mGeofireProvider;
    private WorkerProvider mWorkerProvider;
    private FusedLocationProviderClient mFusedLocation;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private TokenProvider mTokenProvider;

    private LatLng mCurrentLatLng;
    private Map<String, Marker> mWorkersMarkers = new HashMap<>();
    private final Map<String, Worker> mWorkers = new HashMap<>();
    private boolean mIsFirstTime = true;

    private PlacesClient mPlaces;
    private AutocompleteSupportFragment mAutoComplete;
    private String mOrigin;
    private LatLng mOriginLatLng;
    private ChipGroup mFilterChips;
    private String mCurrentFilter = CategoryHelper.ALL;   // slug del oficio o CategoryHelper.ALL
    private LinearLayout mLegendContainer;

    private FloatingActionButton mFabLegend;
    private MaterialCardView mLegendCard;
    private RecyclerView mLegendRecycler;
    private boolean isLegendVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client);

        initToolbar();
        initProviders();
        initMap();
        initPlaces();
        initFilterSpinner();
        initLegend();
        generateToken();
        initFabMyLocation();

    }

    private void initFabMyLocation() {
        FloatingActionButton fabMyLocation = findViewById(R.id.fab_my_location);

        fabMyLocation.setOnClickListener(v -> {
            if (mCurrentLatLng != null) {
                // Centra la cámara en la ubicación actual
                moveCamera(mCurrentLatLng);
                Toast.makeText(this, "Ubicación actual", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private boolean isDarkModeEnabled() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void initLegend() {
        mFabLegend = findViewById(R.id.fab_legend);
        mLegendCard = findViewById(R.id.legend_card);
        mLegendRecycler = findViewById(R.id.legend_recycler);

        // Configurar RecyclerView
        mLegendRecycler.setLayoutManager(new LinearLayoutManager(this));
        mLegendRecycler.setHasFixedSize(false);

        // Crear y establecer el adaptador
        LegendAdapter adapter = new LegendAdapter(getLegendItems());
        mLegendRecycler.setAdapter(adapter);

        // Configurar el botón FAB
        mFabLegend.setOnClickListener(v -> toggleLegend());
    }

    private List<LegendItem> getLegendItems() {
        List<LegendItem> items = new ArrayList<>();
        // Oficios del catálogo (mismo orden y nombres que el backend)
        for (String slug : CatalogProvider.localSlugs()) {
            items.add(new LegendItem(CatalogProvider.categoryName(slug), CategoryHelper.iconFor(slug)));
        }
        return items;
    }

    private void toggleLegend() {
        if (isLegendVisible) {
            hideLegend();
        } else {
            showLegend();
        }
    }

    private void showLegend() {
        mLegendCard.setVisibility(View.VISIBLE);
        mLegendCard.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
        mFabLegend.setImageResource(R.drawable.ic_close);
        isLegendVisible = true;
    }

    private void hideLegend() {
        mLegendCard.animate()
                .alpha(0f)
                .translationY(mLegendCard.getHeight())
                .setDuration(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(() -> mLegendCard.setVisibility(View.GONE))
                .start();
        mFabLegend.setImageResource(R.drawable.ic_legend);
        isLegendVisible = false;
    }

    private static class LegendItem {
        String text;
        int iconRes;

        LegendItem(String text, int iconRes) {
            this.text = text;
            this.iconRes = iconRes;
        }
    }

    private static class LegendAdapter extends RecyclerView.Adapter<LegendAdapter.ViewHolder> {
        private final List<LegendItem> items;

        LegendAdapter(List<LegendItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.legend_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LegendItem item = items.get(position);
            holder.icon.setImageResource(item.iconRes);
            holder.text.setText(item.text);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView text;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.legend_icon);
                text = itemView.findViewById(R.id.legend_text);
            }
        }
    }

    private void initToolbar() {
        // Encabezado del mockup 56: botón de volver propio, sin toolbar
        findViewById(R.id.btn_map_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_map_worker_close).setOnClickListener(v -> hideWorkerCard());
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider();
        mWorkerProvider = new WorkerProvider();
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
        mTokenProvider = new TokenProvider();
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initPlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        mPlaces = Places.createClient(this);
        setupAutoComplete();
    }

    private void setupAutoComplete() {
        if (mAutoComplete != null) {
            mAutoComplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

            // Adaptar el autocomplete al modo oscuro
            View autocompleteView = mAutoComplete.getView();
            if (autocompleteView != null) {
                // Aplicar color de fondo adaptativo
                autocompleteView.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.background_card)
                );

                // Buscar el EditText dentro del fragment y aplicar colores
                TextView searchText = autocompleteView.findViewById(
                        com.google.android.libraries.places.R.id.places_autocomplete_search_input
                );
                if (searchText != null) {
                    searchText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    searchText.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
                }

                // Aplicar color al botón de limpiar
                ImageView clearButton = autocompleteView.findViewById(
                        com.google.android.libraries.places.R.id.places_autocomplete_clear_button
                );
                if (clearButton != null) {
                    clearButton.setColorFilter(
                            ContextCompat.getColor(this, R.color.text_secondary)
                    );
                }

                // Aplicar color al icono de búsqueda
                ImageView searchButton = autocompleteView.findViewById(
                        com.google.android.libraries.places.R.id.places_autocomplete_search_button
                );
                if (searchButton != null) {
                    searchButton.setColorFilter(
                            ContextCompat.getColor(this, R.color.colorPrimary)
                    );
                }
            }

            mAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    mOrigin = place.getName();
                    mOriginLatLng = place.getLatLng();
                    Log.d(TAG, "Selected place: " + mOrigin + " at " + mOriginLatLng);
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e(TAG, "An error occurred: " + status);
                }
            });
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.setOnMapClickListener(latLng -> hideWorkerCard());

        if (isDarkModeEnabled()) {
            try {
                boolean success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)
                );
                if (!success) {
                    Log.e(TAG, "Style parsing failed.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Can't find style. Error: ", e);
            }
        }

        mLocationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(5);

        setupLocationCallback();
        startLocation();

        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null) {
                showWorkerCard((String) marker.getTag(), marker.getPosition());
            }
            return false;
        });

        mMap.setOnCameraIdleListener(this::getActiveWorkers);
    }

    private void setupLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (mIsFirstTime) {
                        mIsFirstTime = false;
                        moveCamera(mCurrentLatLng);
                        getActiveWorkers();
                    }
                }
            }
        };
    }

    private void moveCamera(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(DEFAULT_ZOOM)
                        .build()
        ));
    }

    private void getActiveWorkers() {
        if (mCurrentLatLng != null) {
            mGeofireProvider.getActiveWorkers(mCurrentLatLng, SEARCH_RADIUS).addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    addWorkerMarker(key, new LatLng(location.latitude, location.longitude));
                }

                @Override
                public void onKeyExited(String key) {
                    removeWorkerMarker(key);
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    moveWorkerMarker(key, new LatLng(location.latitude, location.longitude));
                }

                @Override
                public void onGeoQueryReady() {
                    // Query is complete
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    Log.e(TAG, "Error in GeoQuery: " + error.getMessage());
                }
            });
        }
    }

    private void addWorkerMarker(String workerId, LatLng location) {
        mWorkerProvider.getWorker(workerId).addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                Worker worker = dataSnapshot.getValue(Worker.class);
                if (worker != null && !mWorkersMarkers.containsKey(workerId)) {
                    if (CategoryHelper.matches(worker, mCurrentFilter)) {

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(worker.getName() != null ? worker.getName() : "Unknown Worker")
                                .icon(workerMarkerIcon(CategoryHelper.iconFor(worker))));
                        marker.setTag(workerId);
                        mWorkersMarkers.put(workerId, marker);
                        mWorkers.put(workerId, worker);
                    }
                }
            }
        }).addOnFailureListener(exception -> {
            Log.e(TAG, "Failed to get worker data for ID: " + workerId, exception);
        });
    }

    private void initFilterSpinner() {
        mFilterChips = findViewById(R.id.chip_group_filter);

        // Oficios del catálogo del backend; si no responde, la lista local
        new CatalogProvider().getCategoriesWithFallback(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                if (isFinishing()) return;
                bindFilterChips(CategoryHelper.filterOptions(catalog, getString(R.string.map_filter_all)));
            }

            @Override
            public void onError(String message, int httpStatus) {
                // getCategoriesWithFallback no falla
            }
        });
    }

    /** Un chip por oficio (mockup 56); el primero es «Todos». */
    private void bindFilterChips(List<CatalogData.Category> filterOptions) {
        mFilterChips.removeAllViews();
        for (CatalogData.Category category : filterOptions) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(category.getName());
            chip.setCheckable(true);
            chip.setChecked(category.getSlug().equals(mCurrentFilter));
            chip.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked && !category.getSlug().equals(mCurrentFilter)) {
                    mCurrentFilter = category.getSlug();
                    hideWorkerCard();
                    clearWorkersFromMap();
                    getActiveWorkers();
                }
            });
            mFilterChips.addView(chip);
        }
    }

    private void clearWorkersFromMap() {
        for (Marker marker : mWorkersMarkers.values()) {
            marker.remove();
        }
        mWorkersMarkers.clear();
    }

    private void removeWorkerMarker(String workerId) {
        Marker marker = mWorkersMarkers.remove(workerId);
        if (marker != null) {
            marker.remove();
        }
    }

    private void moveWorkerMarker(String workerId, LatLng newLocation) {
        Marker marker = mWorkersMarkers.get(workerId);
        if (marker != null) {
            marker.setPosition(newLocation);
        }
    }

    /**
     * Pin del trabajador: marker_worker_base con el ícono vectorial del oficio
     * centrado en la cabeza del pin. Los íconos son vectores, así que se
     * dibujan en un Canvas (BitmapFactory.decodeResource no los lee).
     */
    private BitmapDescriptor workerMarkerIcon(int iconResource) {
        Drawable pin = ContextCompat.getDrawable(this, R.drawable.marker_worker_base);
        Drawable icon = ContextCompat.getDrawable(this, iconResource);
        if (pin == null || icon == null) return BitmapDescriptorFactory.defaultMarker();

        int width = pin.getIntrinsicWidth();
        int height = pin.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        pin.setBounds(0, 0, width, height);
        pin.draw(canvas);

        // Cabeza del pin: círculo de centro (24, 22) en el lienzo de 48 x 56
        int centerX = width / 2;
        int centerY = Math.round(height * 22f / 56f);
        int half = Math.round(width * 11f / 48f);
        icon = icon.mutate();
        icon.setTint(ContextCompat.getColor(this, R.color.ads_marker_icon));
        icon.setBounds(centerX - half, centerY - half, centerX + half, centerY + half);
        icon.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void startLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            // El botón de Google se duplica con fab_my_location del diseño
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.location_permission_title)
                    .setMessage(R.string.location_permission_message)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        ActivityCompat.requestPermissions(MapClientActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_REQUEST_CODE);
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(MapClientActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        } else {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show();
        }
    }

    /** Hoja inferior con el técnico del marcador (mockup 56). */
    private void showWorkerCard(String workerId, LatLng position) {
        Worker worker = mWorkers.get(workerId);
        View card = findViewById(R.id.card_map_worker);
        String name = worker != null && worker.getName() != null ? worker.getName() : "";
        if (worker != null && worker.getLastName() != null) name = (name + " " + worker.getLastName()).trim();

        TextView initials = findViewById(R.id.tv_map_worker_initials);
        initials.setText(initialsOf(name));

        String categories = worker != null ? worker.getCategoriesDisplay() : null;
        ((TextView) findViewById(R.id.tv_map_worker_name)).setText(categories != null && !categories.isEmpty()
                ? getString(R.string.map_worker_card_title, name, categories) : name);

        String distance = null;
        if (mCurrentLatLng != null && position != null) {
            float[] meters = new float[1];
            Location.distanceBetween(mCurrentLatLng.latitude, mCurrentLatLng.longitude,
                    position.latitude, position.longitude, meters);
            distance = String.format(new Locale("es", "CO"), "%.1f km", meters[0] / 1000f);
        }
        String meta;
        if (worker != null && worker.getTotalRatings() > 0) {
            String rating = String.format(new Locale("es", "CO"), "%.1f", worker.getRating());
            meta = getResources().getQuantityString(R.plurals.map_worker_ratings,
                    worker.getTotalRatings(), rating, worker.getTotalRatings());
        } else {
            meta = getString(R.string.map_worker_new);
        }
        if (distance != null) meta += " · " + distance;
        ((TextView) findViewById(R.id.tv_map_worker_meta)).setText(meta);

        // «Ver perfil» (mockup 56); la solicitud se hace desde el perfil (mockup 09)
        findViewById(R.id.btn_map_worker_request).setOnClickListener(v -> {
            Intent intent = new Intent(MapClientActivity.this, WorkerProfileActivity.class);
            intent.putExtra(WorkerProfileActivity.EXTRA_WORKER_ID, workerId);
            if (mCurrentLatLng != null && position != null) {
                float[] meters = new float[1];
                Location.distanceBetween(mCurrentLatLng.latitude, mCurrentLatLng.longitude,
                        position.latitude, position.longitude, meters);
                intent.putExtra(WorkerProfileActivity.EXTRA_DISTANCE_KM, meters[0] / 1000f);
            }
            HomeUserActivity.putClientLocation(intent, mCurrentLatLng);
            startActivity(intent);
        });
        card.setVisibility(View.VISIBLE);
    }

    private void hideWorkerCard() {
        findViewById(R.id.card_map_worker).setVisibility(View.GONE);
    }

    private static String initialsOf(String name) {
        StringBuilder initials = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) initials.append(Character.toUpperCase(part.charAt(0)));
        }
        return initials.toString();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void logout() {
        mAuthProvider.logOut();
        Intent intent = new Intent(MapClientActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFusedLocation != null && mLocationCallback != null) {
            mFusedLocation.removeLocationUpdates(mLocationCallback);
        }
    }

    void generateToken() {
        mTokenProvider.create(mAuthProvider.getId());
    }
}