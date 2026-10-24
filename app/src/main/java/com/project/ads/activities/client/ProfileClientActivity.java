package com.project.ads.activities.client;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

import com.project.ads.models.Client;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.ClientProvider;
import com.google.firebase.database.DataSnapshot;
import com.project.ads.R;
import com.project.ads.activities.NotificationsActivity;
import com.project.ads.activities.PqrListActivity;
import com.project.ads.activities.MyDataActivity;
import com.project.ads.activities.LegalActivity;
import com.project.ads.helpers.ClientNavHelper;
import com.project.ads.helpers.NotificationPermissionHelper;
import com.project.ads.helpers.DisplayNameHelper;
import com.project.ads.helpers.UnreadNotificationsWatcher;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

/**
 * Mi cuenta del cliente · mockup 37 (docs/mockups/MiCuenta.dc.html).
 * Pestaña de la barra inferior. PQR espera su backend.
 */
public class ProfileClientActivity extends AppCompatActivity {

    private final UnreadNotificationsWatcher mUnreadWatcher =
            new UnreadNotificationsWatcher(this::showUnreadCount);

    private static final String TAG = "ProfileClientActivity";

    // UI Components
    private ImageView ivProfileImage;
    private TextView tvInitials;
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
        setupClickListeners();
        ClientNavHelper.setup(this, R.id.nav_client_account);
        loadClientData();
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mClientProvider = new ClientProvider();
    }

    private void initViews() {
        tvInitials = findViewById(R.id.tv_profile_client_initials);
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

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            animateClick(v);
            editProfile();
        });

        btnChangePassword.setOnClickListener(v -> {
            animateClick(v);
            changePassword();
        });

        findViewById(R.id.row_account_notifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.row_account_pqr).setOnClickListener(v -> {
            Intent intent = new Intent(this, PqrListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        findViewById(R.id.row_account_history).setOnClickListener(v -> {
            Intent intent = new Intent(this, MyRequestsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        findViewById(R.id.row_account_terms).setOnClickListener(v ->
                startActivity(LegalActivity.intent(this, LegalActivity.DOCUMENT_TERMS)));

        ivProfileImage.setOnClickListener(v -> {
            animateClick(v);
            changeProfileImage();
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.profile_logout)
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) ->
                        com.project.ads.helpers.LogoutHelper.performLogout(this, mAuthProvider, null, null))
                .setNegativeButton("No", null)
                .show());
    }

    private void changeProfileImage() {
        new AlertDialog.Builder(this)
                .setTitle("Cambiar foto de perfil")
                .setMessage("Esta función estará disponible próximamente en la edición de perfil.")
                .setPositiveButton("Entendido", null)
                .show();
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
            String fullName = DisplayNameHelper.format(currentClient.getName() + " " + currentClient.getLastName());
            tvClientName.setText(fullName);
            tvInitials.setText(initialsOf(fullName));
            tvClientEmail.setText(currentClient.getEmail());

            // Estadísticas (valores por defecto por ahora)
            tvTotalServices.setText("0");
            tvMemberSince.setText("Octubre 2025");
            
            loadProfileImage();

        });
    }

    private void loadProfileImage() {
        if (currentClient == null || ivProfileImage == null) {
            return;
        }

        try {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            String imageUrl = currentClient.getImage();

            // Verificar que la URL sea válida
            if (imageUrl != null && !imageUrl.trim().isEmpty() &&
                    !imageUrl.equalsIgnoreCase("default") &&
                    !imageUrl.equalsIgnoreCase("null")) {

                Log.d(TAG, "Cargando imagen del perfil: " + imageUrl);

                ivProfileImage.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(imageUrl)
                        .apply(options)
                        .into(ivProfileImage);
            } else {
                // Si la URL no es válida, usar imagen por defecto
                Log.d(TAG, "Cliente sin foto, se muestran las iniciales");
                ivProfileImage.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar imagen del perfil", e);
            ivProfileImage.setImageResource(R.drawable.ic_person);
        }
    }

    private void editProfile() {
        startActivity(new Intent(this, MyDataActivity.class));
    }

    private static String initialsOf(String name) {
        StringBuilder initials = new StringBuilder();
        if (name == null) return "";
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && !"null".equals(part) && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.toString();
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    @Override
    protected void onStart() {
        super.onStart();
        mUnreadWatcher.start();
    }

    @Override
    protected void onStop() {
        mUnreadWatcher.stop();
        super.onStop();
    }

    /** Conteo de notificaciones sin leer en la fila «Notificaciones» (mockups 37 y 51). */
    private void showUnreadCount(int count) {
        android.widget.TextView badge = findViewById(R.id.tv_account_notifications_count);
        badge.setText(UnreadNotificationsWatcher.badgeLabel(count));
        badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        badge.setContentDescription(getResources().getQuantityString(
                R.plurals.account_notifications_unread, count, count));
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationPermissionHelper.bindEnableRow(this, findViewById(R.id.row_account_enable_notifications));
        if (currentClient != null) {
            loadClientData();
        }
    }
}