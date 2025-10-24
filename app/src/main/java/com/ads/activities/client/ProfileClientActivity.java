package com.ads.activities.client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ads.models.Client;
import com.ads.providers.AuthProvider;
import com.ads.providers.ClientProvider;
import com.google.firebase.database.DataSnapshot;
import com.ads.R;
import com.bumptech.glide.Glide;

public class ProfileClientActivity extends AppCompatActivity {

    private static final String TAG = "ProfileClientActivity";

    // UI Components
    private Toolbar toolbar;
    private ImageView ivProfileImage;
    private TextView tvClientName;
    private TextView tvClientEmail;

    // Stats
    private TextView tvTotalServices;
    private TextView tvMemberSince;

    // Action buttons
    private LinearLayout btnEditProfile;
    private LinearLayout btnChangePassword;

    private ProgressBar progressBar;
    private NestedScrollView layoutContent;

    // Providers
    private AuthProvider mAuthProvider;
    private ClientProvider mClientProvider;

    private String clientId;
    private Client currentClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_client);

        initProviders();
        initViews();
        setupToolbar();
        setupClickListeners();
        loadClientData();
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mClientProvider = new ClientProvider();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvClientName = findViewById(R.id.tvClientName);
        tvClientEmail = findViewById(R.id.tvClientEmail);

        tvTotalServices = findViewById(R.id.tvTotalServices);
        tvMemberSince = findViewById(R.id.tvMemberSince);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        progressBar = findViewById(R.id.progressBar);
        layoutContent = findViewById(R.id.layoutContent);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mi Perfil");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            animateClick(v);
            editProfile();
        });

        btnChangePassword.setOnClickListener(v -> {
            animateClick(v);
            changePassword();
        });
    }

    private void loadClientData() {
        clientId = mAuthProvider.getId();

        if (clientId == null) {
            showError("Error: Usuario no autenticado");
            return;
        }

        showLoading(true);

        mClientProvider.getClient(clientId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        currentClient = snapshot.getValue(Client.class);

                        if (currentClient != null) {
                            updateUI();
                        }
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar datos del cliente", e);
                    showError("Error al cargar datos del perfil");
                    showLoading(false);
                });
    }

    private void updateUI() {
        runOnUiThread(() -> {
            // Información básica
            String fullName = currentClient.getName() + " " + currentClient.getLastName();
            tvClientName.setText(fullName);
            tvClientEmail.setText(currentClient.getEmail());

            // Estadísticas (valores por defecto por ahora)
            tvTotalServices.setText("0");
            tvMemberSince.setText("Octubre 2025");

            // Cargar imagen de perfil
            if (currentClient.getImage() != null && !currentClient.getImage().isEmpty()) {
                // Cargar imagen con Glide
                Glide.with(this)
                        .load(currentClient.getImage())
                        .placeholder(R.drawable.ic_person) // Imagen mientras carga
                        .error(R.drawable.ic_person) // Imagen si hay error
                        .circleCrop() // Hace la imagen circular
                        .into(ivProfileImage);
            } else {
                // Si no hay imagen, mostrar imagen por defecto
                ivProfileImage.setImageResource(R.drawable.ic_person);
            }
        });
    }
    private void editProfile() {
        Toast.makeText(this, "Función de editar perfil en desarrollo", Toast.LENGTH_SHORT).show();
        // TODO: Implementar edición de perfil
        // Intent intent = new Intent(this, EditProfileClientActivity.class);
        // startActivity(intent);
    }

    private void changePassword() {
        Toast.makeText(this, "Función de cambiar contraseña en desarrollo", Toast.LENGTH_SHORT).show();
        // TODO: Implementar cambio de contraseña
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

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        layoutContent.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}