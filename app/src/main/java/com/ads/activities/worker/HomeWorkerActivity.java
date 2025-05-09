package com.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.ads.activities.worker.fixdepot.FixDepotActivity;
import com.ads.helpers.LogoutHelper;
import com.ads.providers.AuthProvider;
import com.ads.providers.GeofireProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.project.ads.R;

public class HomeWorkerActivity extends AppCompatActivity {
    Button mButtonViewMap2;
    Button mButtonFixDepot;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    AuthProvider mAuthProvider;
    Toolbar mToolbar;
    private FusedLocationProviderClient mFusedLocation;
    private GeofireProvider mGeofireProvider;
    private LocationCallback mLocationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_worker);
        initProviders();

        // Inicializar vistas del Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        mToolbar = findViewById(R.id.toolbar);


        // Configurar Toolbar
        setSupportActionBar(mToolbar);

        // Configurar Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                mToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mButtonViewMap2 = findViewById(R.id.vermapaworker);
        mButtonFixDepot = findViewById(R.id.buttonFixDepot);


        mButtonViewMap2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewMap();
            }

        });

        mButtonFixDepot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToFixDepot();
            }
        });

        // Configurar listener para los items del menú
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    Toast.makeText(HomeWorkerActivity.this, "Inicio", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_profile) {
                    Toast.makeText(HomeWorkerActivity.this, "Perfil", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_settings) {
                    Toast.makeText(HomeWorkerActivity.this, "Configuración", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.action_logout) {
                    logout();
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        // Configurar el color de la barra de estado al color por defecto del sistema
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }
    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider();
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
    }

    private void viewMap() {
        Intent intent = new Intent(HomeWorkerActivity.this, MapWorkerActivity.class);
        startActivity(intent);
    }

    private void goToFixDepot() {
        Intent intent = new Intent(HomeWorkerActivity.this, FixDepotActivity.class);
        startActivity(intent);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeWorkerActivity.this);
            builder.setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro que deseas cerrar sesión?")
                    .setCancelable(false);  // Evita que el diálogo se cierre al tocar fuera

            // Botón positivo (Sí)
            builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        // Llamar a disconnect primero si es necesario
                        try {
                            disconnect();
                        } catch (Exception e) {
                            Toast.makeText(HomeWorkerActivity.this,
                                    "Error en disconnect: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }

                        LogoutHelper.performLogout(HomeWorkerActivity.this, mAuthProvider, null, null);
                    } catch (Exception e) {
                        Toast.makeText(HomeWorkerActivity.this,
                                "Error al cerrar sesión: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                }
            });

            // Botón negativo (No)
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            // Crear y mostrar el diálogo en el UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog dialog = builder.create();
                    if (!isFinishing()) {  // Verifica que la actividad no esté terminando
                        dialog.show();
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(HomeWorkerActivity.this,
                    "Error al mostrar diálogo: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // Modificamos también el método disconnect para hacerlo más seguro
    private void disconnect() {
        try {
            if (mFusedLocation != null && mLocationCallback != null) {
                mFusedLocation.removeLocationUpdates(mLocationCallback);
                if (mGeofireProvider != null && mAuthProvider != null) {
                    String userId = mAuthProvider.getId();
                    if (userId != null) {
                        mGeofireProvider.removeLocation(userId);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this,
                    "Error al desconectar: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}