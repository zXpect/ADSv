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
import com.ads.helpers.LogoutHelper;
import com.ads.providers.AuthProvider;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.project.ads.R;

public class HomeUserActivity extends AppCompatActivity {
    Button mButtonViewMap;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    private AuthProvider mAuthProvider;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Crashlytics y Functions al inicio de la actividad
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        crashlytics = FirebaseCrashlytics.getInstance();


        try {
            setContentView(R.layout.activity_home_user);
            initProviders();
            setupViews();
            setupNavigation();
            setupMapButton();
            setupStatusBar();

            // Registrar información del usuario si está disponible
            if (mAuthProvider != null && mAuthProvider.getId() != null) {
                String userId = mAuthProvider.getId();
                crashlytics.setUserId(userId);
                crashlytics.setCustomKey("user_type", "client");

                // Registrar evento de login para Firebase Functions
                Bundle params = new Bundle();
                params.putString("user_id", userId);
                params.putString("login_method", "email");
                mFirebaseAnalytics.logEvent("user_login", params);
            }

        } catch (Exception e) {
            logError("onCreate", e);
        }
    }
    // Método helper para logging consistente
    private void logError(String methodName, Exception e) {
        // Registrar en Crashlytics
        crashlytics.log("Error en " + methodName);
        crashlytics.recordException(e);

        // Registrar detalles adicionales para análisis
        Bundle params = new Bundle();
        params.putString("error_method", methodName);
        params.putString("error_message", e.getMessage());
        params.putString("error_type", e.getClass().getSimpleName());
        mFirebaseAnalytics.logEvent("app_error", params);

        // Mostrar mensaje al usuario
        Toast.makeText(this, "Error en la aplicación", Toast.LENGTH_SHORT).show();
    }

    private void setupViews() {
        try {
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

        } catch (Exception e) {
            crashlytics.log("Error al configurar las vistas principales");
            crashlytics.recordException(e);
        }
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

            // Registrar navegación para análisis
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
        } catch (Exception e) {
            crashlytics.log("Error al inicializar AuthProvider");
            crashlytics.recordException(e);
            Toast.makeText(this, "Error al inicializar servicios de autenticación", Toast.LENGTH_SHORT).show();
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
    // Añade este método en la clase HomeUserActivity
    private void testCrashlytics() {
        try {
            // Registrar información previa al crash para mejor análisis
            crashlytics.log("Iniciando test de Crashlytics");
            Bundle params = new Bundle();
            params.putString("test_type", "forced_crash");
            params.putString("test_timestamp", String.valueOf(System.currentTimeMillis()));
            mFirebaseAnalytics.logEvent("crashlytics_test", params);

            // Forzar un NullPointerException
            String testString = null;
            int length = testString.length(); // Esto causará un NullPointerException
        } catch (Exception e) {
            // Registrar el error con información adicional
            crashlytics.setCustomKey("test_crash", true);
            crashlytics.setCustomKey("test_time", System.currentTimeMillis());
            logError("testCrashlytics", e);

            // Re-lanzar la excepción para que Crashlytics la capture completamente
            throw new RuntimeException("Test de Crashlytics", e);
        }
    }
}