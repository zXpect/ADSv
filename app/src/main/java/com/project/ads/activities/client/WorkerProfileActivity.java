package com.project.ads.activities.client;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.project.ads.R;
import com.project.ads.helpers.SearchTimer;
import com.project.ads.providers.ApiCallback;
import com.project.ads.models.api.WorkerStats;
import com.project.ads.models.Worker;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.providers.WorkerProvider;

import java.util.Locale;

/**
 * Perfil del técnico · mockup 09 (docs/mockups/Perfil.dc.html).
 * Foto o iniciales, verificación, calificación, distancia, descripción y oficios.
 * «Solicitar servicio» abre la solicitud (mockup 10) con este técnico.
 * «Problemas que resuelve» no se muestra: hoy no se guardan tareas por técnico.
 */
public class WorkerProfileActivity extends AppCompatActivity {

    public static final String EXTRA_WORKER_ID = "workerId";
    /** Distancia al cliente en km (opcional). */
    public static final String EXTRA_DISTANCE_KM = "distance_km";

    private String mWorkerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_profile);

        mWorkerId = getIntent().getStringExtra(EXTRA_WORKER_ID);
        if (TextUtils.isEmpty(mWorkerId)) {
            finish();
            return;
        }

        findViewById(R.id.btn_profile_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_profile_request).setOnClickListener(v -> openRequest());

        float distance = getIntent().getFloatExtra(EXTRA_DISTANCE_KM, -1f);
        ((TextView) findViewById(R.id.tv_profile_distance)).setText(distance >= 0
                ? String.format(new Locale("es", "CO"), "%.1f km", distance) : "—");

        loadWorker();
    }

    private void loadWorker() {
        new WorkerProvider().getWorker(mWorkerId)
                .addOnSuccessListener(snapshot -> {
                    Worker worker = snapshot.exists() ? snapshot.getValue(Worker.class) : null;
                    if (worker == null) {
                        Toast.makeText(this, "No se pudo cargar el perfil del técnico", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    bind(worker);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No se pudo cargar el perfil del técnico", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bind(Worker worker) {
        Locale locale = new Locale("es", "CO");
        String name = ((worker.getName() != null ? worker.getName() : "") + " "
                + (worker.getLastName() != null ? worker.getLastName() : "")).trim();
        if (name.isEmpty()) name = getString(R.string.request_the_worker);
        ((TextView) findViewById(R.id.tv_profile_name)).setText(name);

        // Foto si existe; si no, iniciales sobre navy
        ((TextView) findViewById(R.id.tv_profile_initials)).setText(initialsOf(name));
        ImageView photo = findViewById(R.id.iv_profile_photo);
        if (!TextUtils.isEmpty(worker.getImage())) {
            photo.setVisibility(View.VISIBLE);
            Glide.with(this).load(worker.getImage()).circleCrop().into(photo);
        }

        findViewById(R.id.tv_profile_verified).setVisibility(
                "approved".equals(worker.getVerificationStatusString()) ? View.VISIBLE : View.GONE);

        // Calificación y, mientras llegan los trabajos realizados, el número de calificaciones
        TextView rating = findViewById(R.id.tv_profile_rating);
        TextView ratings = findViewById(R.id.tv_profile_ratings);
        if (worker.getTotalRatings() > 0) {
            rating.setText(String.format(locale, "%.1f", worker.getRating()));
            ratings.setText(String.valueOf(worker.getTotalRatings()));
        } else {
            rating.setText(R.string.profile_new);
            rating.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            ratings.setText("0");
        }
        loadCompletedJobs(ratings);

        String description = worker.getDescription();
        boolean hasDescription = !TextUtils.isEmpty(description) && !description.trim().isEmpty();
        findViewById(R.id.tv_profile_about_title).setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        TextView about = findViewById(R.id.tv_profile_about);
        about.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        if (hasDescription) about.setText(description.trim());

        // Oficios del técnico
        ChipGroup categories = findViewById(R.id.chip_group_profile_categories);
        categories.removeAllViews();
        for (String slug : worker.getCategorySlugs()) {
            String categoryName = CatalogProvider.categoryName(slug);
            if (TextUtils.isEmpty(categoryName)) continue;
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.view_profile_category_chip, categories, false);
            chip.setText(categoryName);
            categories.addView(chip);
        }
        findViewById(R.id.tv_profile_categories_title)
                .setVisibility(categories.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void openRequest() {
        SearchTimer.markSelected(this);
        Intent intent = new Intent(this, ServiceRequestActivity.class);
        intent.putExtra("workerId", mWorkerId);
        if (getIntent().hasExtra(ServiceRequestActivity.EXTRA_LAT)) {
            intent.putExtra(ServiceRequestActivity.EXTRA_LAT, getIntent().getDoubleExtra(ServiceRequestActivity.EXTRA_LAT, 0));
            intent.putExtra(ServiceRequestActivity.EXTRA_LNG, getIntent().getDoubleExtra(ServiceRequestActivity.EXTRA_LNG, 0));
        }
        startActivity(intent);
    }

    private static String initialsOf(String name) {
        StringBuilder initials = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) initials.append(Character.toUpperCase(part.charAt(0)));
        }
        return initials.toString();
    }

    /** «Trabajos» del mockup 09: solicitudes completadas, con o sin calificación (HU-11). */
    private void loadCompletedJobs(TextView count) {
        new WorkerProvider().getStats(mWorkerId, new ApiCallback<WorkerStats>() {
            @Override
            public void onSuccess(WorkerStats data) {
                if (isFinishing() || data == null) return;
                count.setText(String.valueOf(data.getCompletedJobs()));
                ((TextView) findViewById(R.id.tv_profile_ratings_label)).setText(R.string.profile_jobs_label);
            }

            @Override
            public void onError(String message, int httpStatus) {
                // Se queda el número de calificaciones
            }
        });
    }
}
