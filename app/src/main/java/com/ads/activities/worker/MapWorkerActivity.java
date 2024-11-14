package com.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.ads.activities.MainActivity;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.project.ads.R;

public class MapWorkerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapWorkerActivity";
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final int SETTINGS_REQUEST_CODE = 2;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocation;
    private LatLng mCurrentLatLng;
    private Marker mUserMarker;
    private Button mButtonConnect;
    private boolean isConnect = false;

    private GeofireProvider mGeofireProvider;
    private AuthProvider mAuthProvider;
    private WorkerProvider mWorkerProvider;
    private String mWorkerType;
    private TokenProvider mTokenProvider;

    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    updateMarkerLocation();
                    updateLocation();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_worker);

        initProviders();
        initUI();
        initMap();
        generateToken();


        // Asegúrate de que la barra de estado sea completamente transparente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void initProviders() {
        mGeofireProvider = new GeofireProvider();
        mAuthProvider = new AuthProvider();
        mWorkerProvider = new WorkerProvider();
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
        mTokenProvider = new TokenProvider();
    }

    private void initUI() {
        setSupportActionBar(findViewById(R.id.toolbar_color));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.map_worker_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mButtonConnect = findViewById(R.id.btnconnect);
        mButtonConnect.setOnClickListener(v -> toggleConnection());
    }

    private void initMap() {
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mMapFragment != null) {
            mMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mLocationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(5);

        getWorkerType();
        startLocation();
    }

    private void getWorkerType() {
        mWorkerProvider.getWorker(mAuthProvider.getId()).addOnSuccessListener(dataSnapshot -> {
            Worker worker = dataSnapshot.getValue(Worker.class);
            if (worker != null) {
                mWorkerType = worker.getWork();
                updateWorkerMarker();
            } else {
                Log.e(TAG, "No se encontró el trabajador.");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error al obtener datos del trabajador", e));
    }

    private void updateWorkerMarker() {
        if (mUserMarker != null && mWorkerType != null) {
            mUserMarker.setIcon(getIconForWorkerType(mWorkerType));
        }
    }

    private void updateMarkerLocation() {
        if (mUserMarker == null) {
            mUserMarker = mMap.addMarker(new MarkerOptions()
                    .position(mCurrentLatLng)
                    .title("Mi ubicación")
                    .icon(getIconForWorkerType(mWorkerType)));
        } else {
            mUserMarker.setPosition(mCurrentLatLng);
        }

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(mCurrentLatLng)
                        .zoom(17f)
                        .build()
        ));
    }

    private BitmapDescriptor getIconForWorkerType(String workerType) {
        int iconResource;
        if (workerType == null) {
            iconResource = R.drawable.icon_worker; // Icono por defecto
        } else {
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
        }
        return resizeMapIcon(iconResource, 60, 60);
    }

    private BitmapDescriptor resizeMapIcon(int iconResource, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), iconResource);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    private void startLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            } else {
                checkLocationPermissions();
            }
        } else {
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("Esta aplicación requiere los permisos de ubicación")
                        .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(MapWorkerActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE))
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(MapWorkerActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    private void toggleConnection() {
        if (isConnect) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        startLocation();
        mButtonConnect.setText("Desconectarse");
        isConnect = true;
    }


    private void disconnect() {
        if (mFusedLocation != null) {
            mFusedLocation.removeLocationUpdates(mLocationCallback);
            mGeofireProvider.removeLocation(mAuthProvider.getId());
            mButtonConnect.setText("Conectarse");
            isConnect = false;
        } else {
            Toast.makeText(this, "No se puede desconectar", Toast.LENGTH_SHORT).show();
        }
    }


    private void updateLocation() {
        if (mAuthProvider.exitSession() && mCurrentLatLng != null) {
            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                }
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show();
            }
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
        disconnect();
        mAuthProvider.logOut();
        Intent intent = new Intent(MapWorkerActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    void generateToken(){
        mTokenProvider.create(mAuthProvider.getId());
    }
}