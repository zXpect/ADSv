package com.ads.activities.client;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.ads.activities.MainActivity;
import com.ads.providers.AuthProvider;
import com.google.android.material.navigation.NavigationView;
import com.project.ads.R;

public class HomeUserActivity extends AppCompatActivity {
    Button mButtonViewMap;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    private AuthProvider mAuthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_user);

        initProviders();

        // Inicializar vistas del Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        // Configurar Toolbar
        setSupportActionBar(toolbar);

        // Configurar Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Mantener tu código existente del botón
        mButtonViewMap = findViewById(R.id.vermapa);
        mButtonViewMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewMapClient();
            }
        });

        // Configurar listener para los items del menú
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    Toast.makeText(HomeUserActivity.this, "Inicio", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_profile) {
                    Toast.makeText(HomeUserActivity.this, "Perfil", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_settings) {
                    Toast.makeText(HomeUserActivity.this, "Configuración", Toast.LENGTH_SHORT).show();
                }else if (id == R.id.action_logout) {
                   logout();
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        // Barra de estado trasnpaerente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();

    }

    private void viewMapClient() {
        Intent intent = new Intent(HomeUserActivity.this, MapClientActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // Manejo del botón atrás cuando el drawer está abierto
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    private void logout() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeUserActivity.this);
            builder.setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro que deseas cerrar sesión?")
                    .setCancelable(false);  // Evita que el diálogo se cierre al tocar fuera

            // Botón positivo (Sí)
            builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // Luego hacemos logout
                        if (mAuthProvider != null) {
                            mAuthProvider.logOut();
                        }

                        // Finalmente navegamos al MainActivity
                        Intent intent = new Intent(HomeUserActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpia el stack de actividades
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(HomeUserActivity.this,
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
            Toast.makeText(HomeUserActivity.this,
                    "Error al mostrar diálogo: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }


}