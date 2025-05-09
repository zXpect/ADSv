package com.ads.activities.worker.fixdepot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.project.ads.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StoreFinderActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Toolbar mToolbar;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private Spinner mSortSpinner;
    private ChipGroup mToolFilterChipGroup;
    private RecyclerView mStoreRecyclerView;
    private StoreAdapter mStoreAdapter;
    private Button mFilterApplyButton;

    private List<Store> mStores;
    private List<Store> mFilteredStores;
    private String mSelectedTool = ""; // For filtering
    private int mSortOption = 0; // 0: distance, 1: price low to high, 2: price high to low, 3: rating

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_finder);

        // Initialize toolbar
        mToolbar = findViewById(R.id.toolbar_color);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Find Stores");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize the location client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize sorting spinner
        mSortSpinner = findViewById(R.id.spinnerSort);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSortSpinner.setAdapter(adapter);

        // Initialize tool filter chip group
        mToolFilterChipGroup = findViewById(R.id.chipGroupToolFilter);

        // Initialize store list
        mStoreRecyclerView = findViewById(R.id.recyclerViewStores);
        mStoreRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize store data (in a real app, this would come from a database or API)
        initializeStoreData();

        // Set up the RecyclerView with the store adapter
        mFilteredStores = new ArrayList<>(mStores);
        mStoreAdapter = new StoreAdapter(mFilteredStores);
        mStoreRecyclerView.setAdapter(mStoreAdapter);

        // Filter apply button
        mFilterApplyButton = findViewById(R.id.buttonApplyFilter);

        setupListeners();
        setupToolChips();
    }

    private void setupListeners() {
        // Sort spinner listener
        mSortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSortOption = position;
                applySortAndFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Filter apply button listener
        mFilterApplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySortAndFilter();
            }
        });
    }

    private void setupToolChips() {
        // Add tool filter chips
        String[] tools = {"Hammer", "Screwdriver Set", "Power Drill", "Wrench Set",
                "Measuring Tape", "Saw", "Level", "Pliers", "Electric Screwdriver"};

        for (String tool : tools) {
            Chip chip = new Chip(this);
            chip.setText(tool);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    mSelectedTool = tool;
                } else if (mSelectedTool.equals(tool)) {
                    mSelectedTool = "";
                }
            });
            mToolFilterChipGroup.addView(chip);
        }
    }

    private void initializeStoreData() {
        // In a real app, this data would come from a database or API
        mStores = new ArrayList<>();

        // Sample store data with coordinates, ratings, and prices for tools
        mStores.add(new Store("Hardware City", new LatLng(19.432608, -99.133209), 4.5f,
                createToolPrices("Hammer", 15.99f, "Screwdriver Set", 25.99f, "Power Drill", 99.99f)));

        mStores.add(new Store("Tools & More", new LatLng(19.436054, -99.141264), 4.2f,
                createToolPrices("Hammer", 12.99f, "Wrench Set", 35.50f, "Measuring Tape", 8.99f)));

        mStores.add(new Store("Builder's Supply", new LatLng(19.427375, -99.167442), 4.7f,
                createToolPrices("Power Drill", 89.99f, "Saw", 45.75f, "Level", 18.50f)));

        mStores.add(new Store("Handyman's Corner", new LatLng(19.418904, -99.155470), 4.0f,
                createToolPrices("Pliers", 14.25f, "Electric Screwdriver", 49.99f, "Hammer", 19.99f)));

        mStores.add(new Store("Pro Tools", new LatLng(19.444651, -99.151178), 4.8f,
                createToolPrices("Screwdriver Set", 29.99f, "Power Drill", 119.99f, "Wrench Set", 42.50f)));
    }

    private void applySortAndFilter() {
        // First filter by selected tool
        mFilteredStores = new ArrayList<>();

        for (Store store : mStores) {
            if (mSelectedTool.isEmpty() || store.hasToolForSale(mSelectedTool)) {
                mFilteredStores.add(store);
            }
        }

        // Then sort based on selected option
        switch (mSortOption) {
            case 0: // Distance (closest first)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                final LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                sortStoresByDistance(myLocation);
                            }
                        }
                    });
                }
                break;
            case 1: // Price (low to high)
                if (!mSelectedTool.isEmpty()) {
                    Collections.sort(mFilteredStores, new Comparator<Store>() {
                        @Override
                        public int compare(Store s1, Store s2) {
                            float price1 = s1.getToolPrice(mSelectedTool);
                            float price2 = s2.getToolPrice(mSelectedTool);
                            return Float.compare(price1, price2);
                        }
                    });
                }
                break;
            case 2: // Price (high to low)
                if (!mSelectedTool.isEmpty()) {
                    Collections.sort(mFilteredStores, new Comparator<Store>() {
                        @Override
                        public int compare(Store s1, Store s2) {
                            float price1 = s1.getToolPrice(mSelectedTool);
                            float price2 = s2.getToolPrice(mSelectedTool);
                            return Float.compare(price2, price1);
                        }
                    });
                }
                break;
            case 3: // Rating (highest first)
                Collections.sort(mFilteredStores, new Comparator<Store>() {
                    @Override
                    public int compare(Store s1, Store s2) {
                        return Float.compare(s2.getRating(), s1.getRating());
                    }
                });
                break;
        }

        // Update the map markers
        updateMapMarkers();

        // Update the RecyclerView
        mStoreAdapter.updateStores(mFilteredStores);
    }

    private void sortStoresByDistance(final LatLng myLocation) {
        Collections.sort(mFilteredStores, new Comparator<Store>() {
            @Override
            public int compare(Store s1, Store s2) {
                float[] results1 = new float[1];
                float[] results2 = new float[1];

                Location.distanceBetween(
                        myLocation.latitude, myLocation.longitude,
                        s1.getLocation().latitude, s1.getLocation().longitude,
                        results1);

                Location.distanceBetween(
                        myLocation.latitude, myLocation.longitude,
                        s2.getLocation().latitude, s2.getLocation().longitude,
                        results2);

                return Float.compare(results1[0], results2[0]);
            }
        });

        mStoreAdapter.updateStores(mFilteredStores);
    }

    private void updateMapMarkers() {
        if (mMap != null) {
            mMap.clear();

            for (Store store : mFilteredStores) {
                mMap.addMarker(new MarkerOptions()
                        .position(store.getLocation())
                        .title(store.getName()));
            }

            // If there are stores to show, center the map on the first one
            if (!mFilteredStores.isEmpty()) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mFilteredStores.get(0).getLocation(), 12f));
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            // Get current location and move camera there
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f));

                        // Initial sorting by distance
                        sortStoresByDistance(currentLocation);
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Show all stores initially
        updateMapMarkers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry map setup
                if (mMap != null) {
                    onMapReady(mMap);
                }
            } else {
                Toast.makeText(this, "Location permission is required to show nearby stores", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Helper method to create a map of tool prices
    private java.util.Map<String, Float> createToolPrices(Object... keyValuePairs) {
        java.util.Map<String, Float> prices = new java.util.HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            prices.put((String) keyValuePairs[i], (Float) keyValuePairs[i + 1]);
        }
        return prices;
    }

    // Store model class
    public static class Store {
        private String name;
        private LatLng location;
        private float rating;
        private java.util.Map<String, Float> toolPrices;

        public Store(String name, LatLng location, float rating, java.util.Map<String, Float> toolPrices) {
            this.name = name;
            this.location = location;
            this.rating = rating;
            this.toolPrices = toolPrices;
        }

        public String getName() {
            return name;
        }

        public LatLng getLocation() {
            return location;
        }

        public float getRating() {
            return rating;
        }

        public boolean hasToolForSale(String toolName) {
            return toolPrices.containsKey(toolName);
        }

        public float getToolPrice(String toolName) {
            if (toolPrices.containsKey(toolName)) {
                return toolPrices.get(toolName);
            }
            return Float.MAX_VALUE; // Return a high value if tool not found
        }

        public java.util.Map<String, Float> getToolPrices() {
            return toolPrices;
        }
    }
}