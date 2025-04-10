package com.ads.activities.client;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ads.activities.MainActivity;
import com.ads.models.Worker;
import com.ads.providers.AuthProvider;
import com.ads.providers.GeofireProvider;
import com.ads.providers.TokenProvider;
import com.ads.providers.WorkerProvider;
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
    private boolean mIsFirstTime = true;

    private PlacesClient mPlaces;
    private AutocompleteSupportFragment mAutoComplete;
    private String mOrigin;
    private LatLng mOriginLatLng;
    private Spinner mFilterSpinner;
    private String mCurrentFilter = "todos"; // Default filter value
    private LinearLayout mLegendContainer;
    private static final Map<String, Integer> WORKER_ICONS = new HashMap<>();

    static {
        WORKER_ICONS.put("Carpintería", R.drawable.icon_carpenter);
        WORKER_ICONS.put("Ferretería", R.drawable.icon_ferreteria);
        WORKER_ICONS.put("Pintor", R.drawable.icon_painter);
        WORKER_ICONS.put("Electricista", R.drawable.icon_electrician);
        WORKER_ICONS.put("Plomería", R.drawable.icon_plumber);
        WORKER_ICONS.put("Jardinería", R.drawable.icon_gardener);
        WORKER_ICONS.put("Albañilería", R.drawable.icon_mason);
    }
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


// Hacer la barra de estado transparente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }


    private void initLegend() {
        mFabLegend = findViewById(R.id.fab_legend);
        mLegendCard = findViewById(R.id.legend_card);
        mLegendRecycler = findViewById(R.id.legend_recycler);

        // Configurar RecyclerView
        mLegendRecycler.setLayoutManager(new LinearLayoutManager(this));
        mLegendRecycler.setHasFixedSize(true);

        // Crear y establecer el adaptador
        LegendAdapter adapter = new LegendAdapter(getLegendItems());
        mLegendRecycler.setAdapter(adapter);

        // Configurar el botón FAB
        mFabLegend.setOnClickListener(v -> toggleLegend());
    }

    private List<LegendItem> getLegendItems() {
        List<LegendItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : WORKER_ICONS.entrySet()) {
            items.add(new LegendItem(entry.getKey(), entry.getValue()));
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

    // Clase para los items de la leyenda
    private static class LegendItem {
        String text;
        int iconRes;

        LegendItem(String text, int iconRes) {
            this.text = text;
            this.iconRes = iconRes;
        }
    }

    // Adaptador para la leyenda
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
        setSupportActionBar(findViewById(R.id.toolbar_color));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.map_client_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true); // Habilita el botón de regreso
        }
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
        mAutoComplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.places);
        if (mAutoComplete != null) {
            mAutoComplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
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
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mLocationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(5);

        setupLocationCallback();
        startLocation();

        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null) {
                showWorkerInfoDialog((String) marker.getTag());
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
                    // Apply filter
                    if (mCurrentFilter.equals("Todos los servicios") ||
                            worker.getWork().equalsIgnoreCase(mCurrentFilter)) {
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(worker.getName())
                                .icon(getIconForWorkerType(worker.getWork())));
                        marker.setTag(workerId);
                        mWorkersMarkers.put(workerId, marker);
                    }
                }
            }
        });
    }
    private void initFilterSpinner() {
        mFilterSpinner = findViewById(R.id.spinner_filter);

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("Todos los servicios");
        filterOptions.addAll(Arrays.asList(getResources().getStringArray(R.array.service_types)));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item, // Custom layout for items
                filterOptions
        ) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(position == mFilterSpinner.getSelectedItemPosition() ?
                        getResources().getColor(R.color.colorPrimaryLight) :
                        Color.WHITE);
                return view;
            }
        };

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item); // Custom layout for dropdown
        mFilterSpinner.setAdapter(adapter);

        mFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (!selected.equals(mCurrentFilter)) {
                    mCurrentFilter = selected;
                    clearWorkersFromMap();
                    getActiveWorkers();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCurrentFilter = "Todos los servicios";
            }
        });
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

    private BitmapDescriptor getIconForWorkerType(String workerType) {
        int iconResource;
        switch (workerType.toLowerCase()) {
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
        return resizeMapIcon(iconResource, 60, 60);
    }

    private BitmapDescriptor resizeMapIcon(int iconResource, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), iconResource);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    private void startLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
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

    private void showWorkerInfoDialog(String workerId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.worker_info_title)
                .setMessage(R.string.worker_info_message)
                .setPositiveButton(R.string.request_service, (dialog, which) -> {
                    Intent intent = new Intent(MapClientActivity.this, ServiceRequestActivity.class);
                    intent.putExtra("workerId", workerId);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Cierra la actividad y regresa a la anterior
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
    void generateToken(){
        mTokenProvider.create(mAuthProvider.getId());
    }


}
