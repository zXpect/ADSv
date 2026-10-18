package com.project.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import com.project.ads.models.Worker;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.WorkerProvider;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;

// ===== AGREGAR ESTOS IMPORTS =====
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
// ==================================

public class ProfileWorkerActivity extends AppCompatActivity {

    private static final String TAG = "ProfileWorkerActivity";

    // UI Components
    private Toolbar toolbar;
    private ImageView ivProfileImage;
    private TextView tvWorkerName;
    private TextView tvWorkerType;
    private TextView tvWorkerEmail;
    private TextView tvWorkerPhone;
    private TextView tvVerificationBadge;
    private CardView cardVerificationBadge;

    // Stats
    private TextView tvRating;
    private TextView tvTotalServices;
    private TextView tvPricePerHour;
    private TextView tvMemberSince;

    // Action buttons
    private LinearLayout btnEditProfile;
    private LinearLayout btnDocuments;
    private LinearLayout btnBankInfo;
    private LinearLayout btnChangePassword;

    private ProgressBar progressBar;
    private NestedScrollView layoutContent;

    // Providers
    private AuthProvider mAuthProvider;
    private WorkerProvider mWorkerProvider;

    private String workerId;
    private Worker currentWorker;
    private String verificationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_worker);

        initProviders();
        initViews();
        setupToolbar();
        setupClickListeners();
        loadWorkerData();
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mWorkerProvider = new WorkerProvider();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvWorkerName = findViewById(R.id.tvWorkerName);
        tvWorkerType = findViewById(R.id.tvWorkerType);
        tvWorkerEmail = findViewById(R.id.tvWorkerEmail);
        tvWorkerPhone = findViewById(R.id.tvWorkerPhone);
        tvVerificationBadge = findViewById(R.id.tvVerificationBadge);
        cardVerificationBadge = findViewById(R.id.cardVerificationBadge);

        tvRating = findViewById(R.id.tvRating);
        tvTotalServices = findViewById(R.id.tvTotalServices);
        tvPricePerHour = findViewById(R.id.tvPricePerHour);
        tvMemberSince = findViewById(R.id.tvMemberSince);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnDocuments = findViewById(R.id.btnDocuments);
        btnBankInfo = findViewById(R.id.btnBankInfo);
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

    /**
     * Antes de cerrar sesión deja al trabajador no disponible y fuera del mapa,
     * como hacía el menú lateral del inicio.
     */
    private void logoutWorker() {
        String id = new com.project.ads.providers.AuthProvider().getId();
        if (id == null) {
            com.project.ads.helpers.LogoutHelper.performLogout(this, new com.project.ads.providers.AuthProvider(), null, null);
            return;
        }
        com.project.ads.providers.WorkerProvider workers = new com.project.ads.providers.WorkerProvider();
        new com.project.ads.providers.GeofireProvider().removeLocation(id);
        com.google.android.gms.tasks.Tasks.whenAllComplete(
                workers.updateWorkerAvailability(id, false),
                workers.updateWorkerOnlineStatus(id, false))
                .addOnCompleteListener(task -> com.project.ads.helpers.LogoutHelper.performLogout(
                        this, new com.project.ads.providers.AuthProvider(), null, null));
    }

    private void setupClickListeners() {
        findViewById(R.id.btnLogout).setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.profile_logout)
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> logoutWorker())
                .setNegativeButton("No", null)
                .show());

        btnEditProfile.setOnClickListener(v -> {
            animateClick(v);
            editProfile();
        });

        btnDocuments.setOnClickListener(v -> {
            animateClick(v);
            openDocuments();
        });

        btnBankInfo.setOnClickListener(v -> {
            animateClick(v);
            openBankInfo();
        });

        btnChangePassword.setOnClickListener(v -> {
            animateClick(v);
            changePassword();
        });

        // ===== AGREGAR CLICK EN LA IMAGEN PARA CAMBIARLA =====
        ivProfileImage.setOnClickListener(v -> {
            animateClick(v);
            changeProfileImage();
        });
        // ====================================================
    }

    private void loadWorkerData() {
        workerId = mAuthProvider.getId();

        if (workerId == null) {
            showError("Error: Usuario no autenticado");
            return;
        }

        showLoading(true);

        mWorkerProvider.getWorker(workerId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        currentWorker = snapshot.getValue(Worker.class);

                        if (currentWorker != null) {
                            updateUI();
                            checkVerificationStatus(snapshot);
                        }
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar datos del trabajador", e);
                    showError("Error al cargar datos del perfil");
                    showLoading(false);
                });
    }

    private void updateUI() {
        runOnUiThread(() -> {
            // Información básica
            tvWorkerName.setText(currentWorker.getFullName());
            String categories = currentWorker.getCategoriesDisplay();
            tvWorkerType.setText(categories != null ? categories : getString(R.string.no_specialty));
            tvWorkerEmail.setText(currentWorker.getEmail());
            tvWorkerPhone.setText(currentWorker.getPhone() != null ?
                    currentWorker.getPhone() : "No registrado");

            // Estadísticas
            tvRating.setText(currentWorker.getFormattedRating());
            tvTotalServices.setText(String.valueOf(currentWorker.getTotalRatings()));
            tvPricePerHour.setText("$" + String.format("%.0f", currentWorker.getPricePerHour()));

            // Fecha de registro
            tvMemberSince.setText("Enero 2025");

            // ===== CARGAR IMAGEN DE PERFIL =====
            loadProfileImage();
            // ===================================
        });
    }

    // ===== AGREGAR ESTE NUEVO MÉTODO =====
    private void loadProfileImage() {
        if (currentWorker == null || ivProfileImage == null) {
            return;
        }

        try {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            if (currentWorker.hasImage()) {
                String imageUrl = currentWorker.getImage();

                // Verificar que la URL sea válida
                if (imageUrl != null && !imageUrl.trim().isEmpty() &&
                        !imageUrl.equalsIgnoreCase("default") &&
                        !imageUrl.equalsIgnoreCase("null")) {

                    Log.d(TAG, "Cargando imagen del perfil: " + imageUrl);

                    Glide.with(this)
                            .load(imageUrl)
                            .apply(options)
                            .into(ivProfileImage);
                } else {
                    // Si la URL no es válida, usar imagen por defecto
                    Log.d(TAG, "URL de imagen no válida, usando imagen por defecto");
                    ivProfileImage.setImageResource(R.drawable.ic_person);
                }
            } else {
                // No tiene imagen, usar por defecto
                Log.d(TAG, "Worker sin imagen, usando imagen por defecto");
                ivProfileImage.setImageResource(R.drawable.ic_person);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar imagen del perfil", e);
            ivProfileImage.setImageResource(R.drawable.ic_person);
        }
    }
    // =====================================

    // ===== AGREGAR ESTE MÉTODO PARA CAMBIAR LA IMAGEN =====
    private void changeProfileImage() {
        new AlertDialog.Builder(this)
                .setTitle("Cambiar foto de perfil")
                .setMessage("Esta función estará disponible próximamente en la edición de perfil.")
                .setPositiveButton("Entendido", null)
                .show();

        // TODO: Implementar cambio de imagen
        // Cuando implementes EditProfileWorkerActivity, aquí abrirías esa actividad
        // Intent intent = new Intent(this, EditProfileWorkerActivity.class);
        // startActivity(intent);
    }
    // =====================================================

    private void checkVerificationStatus(DataSnapshot snapshot) {
        try {
            DataSnapshot verificationSnapshot = snapshot.child("verificationStatus");

            if (verificationSnapshot.exists() && verificationSnapshot.child("status").exists()) {
                verificationStatus = verificationSnapshot.child("status").getValue(String.class);
                updateVerificationBadge();
            } else {
                verificationStatus = "incomplete";
                updateVerificationBadge();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar estado", e);
            verificationStatus = "incomplete";
            updateVerificationBadge();
        }
    }

    private void updateVerificationBadge() {
        runOnUiThread(() -> {
            if (verificationStatus == null) {
                cardVerificationBadge.setVisibility(View.GONE);
                return;
            }

            cardVerificationBadge.setVisibility(View.VISIBLE);

            switch (verificationStatus.toLowerCase()) {
                case "approved":
                    tvVerificationBadge.setText("✓ Verificado");
                    tvVerificationBadge.setTextColor(getResources().getColor(R.color.success_color, getTheme()));
                    cardVerificationBadge.setCardBackgroundColor(
                            getResources().getColor(R.color.success_light, getTheme()));
                    break;

                case "documents_submitted":
                case "pending":
                    tvVerificationBadge.setText("⏳ En Revisión");
                    tvVerificationBadge.setTextColor(getResources().getColor(R.color.warning_color, getTheme()));
                    cardVerificationBadge.setCardBackgroundColor(
                            getResources().getColor(R.color.warning_light, getTheme()));
                    break;

                case "rejected":
                    tvVerificationBadge.setText("❌ Rechazado");
                    tvVerificationBadge.setTextColor(getResources().getColor(R.color.error_color, getTheme()));
                    cardVerificationBadge.setCardBackgroundColor(
                            getResources().getColor(R.color.error_light, getTheme()));
                    break;

                case "incomplete":
                default:
                    tvVerificationBadge.setText("📋 Pendiente");
                    tvVerificationBadge.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                    cardVerificationBadge.setCardBackgroundColor(
                            getResources().getColor(R.color.background_light, getTheme()));
                    break;
            }
        });
    }

    private void editProfile() {
        Toast.makeText(this, "Función de editar perfil en desarrollo", Toast.LENGTH_SHORT).show();
        // TODO: Implementar edición de perfil
        // Intent intent = new Intent(this, EditProfileWorkerActivity.class);
        // startActivity(intent);
    }

    private void openDocuments() {
        try {
            Intent intent = new Intent(this, DocumentStatusActivity.class);
            intent.putExtra("worker_id", workerId);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir documentos", e);
            showError("Error al abrir pantalla de documentos");
        }
    }

    private void openBankInfo() {
        Toast.makeText(this, "Función de información bancaria en desarrollo", Toast.LENGTH_SHORT).show();
        // TODO: Implementar información bancaria
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

    // ===== AGREGAR ESTE MÉTODO PARA RECARGAR CUANDO VUELVAS DE EDITAR =====
    @Override
    protected void onResume() {
        super.onResume();

        // Recargar datos si ya tenemos un worker cargado
        // Esto es útil para cuando regreses de editar el perfil
        if (currentWorker != null) {
            loadWorkerData();
        }
    }
    // =====================================================================
}