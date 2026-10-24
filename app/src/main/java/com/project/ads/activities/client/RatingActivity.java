package com.project.ads.activities.client;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.project.ads.models.api.RatingData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.RequestProvider;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.project.ads.R;
import com.project.ads.helpers.AdsSnackbar;
import com.project.ads.helpers.Haptics;

/**
 * Calificar servicio · mockup 30 (docs/mockups/Calificar.dc.html).
 * Cinco estrellas enteras (lo que acepta el backend) y comentario opcional.
 */
public class RatingActivity extends AppCompatActivity {
    private static final String TAG = "RatingActivity";

    private static final int[] STAR_IDS = {R.id.btn_star_1, R.id.btn_star_2, R.id.btn_star_3,
            R.id.btn_star_4, R.id.btn_star_5};
    private static final int[] RATING_LABELS = {R.string.rating_label_1, R.string.rating_label_2,
            R.string.rating_label_3, R.string.rating_label_4, R.string.rating_label_5};

    // UI Components
    private TextView mWorkerName;
    private TextView mRatingLabel;
    private int mRating = 0;
    private EditText mCommentEditText;
    private Button mSubmitButton;
    private ProgressBar mProgressBar;

    // Data
    private String mRequestId;
    private String mWorkerId;
    private String mWorkerNameStr;
    private String mServiceTypeStr;
    private double mExistingRating;

    // Providers
    private RequestProvider mRequestProvider;
    private FirebaseCrashlytics mCrashlytics;
    private FirebaseAnalytics mAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        try {
            initFirebase();
            initProviders();
            getIntentData();

            // Si ya existe calificación, mostrar diálogo y cerrar
            if (mExistingRating > 0) {
                showAlreadyRatedDialog();
                return;
            }

            initViews();
            setupRatingBar();
            setupSubmitButton();

        } catch (Exception e) {
            logError("onCreate", e);
            showError("Error al inicializar");
            finish();
        }
    }

    private void initFirebase() {
        mCrashlytics = FirebaseCrashlytics.getInstance();
        mAnalytics = FirebaseAnalytics.getInstance(this);
    }

    private void initProviders() {
        mRequestProvider = new RequestProvider(this);
    }

    private void getIntentData() {
        mRequestId = getIntent().getStringExtra("request_id");
        mWorkerId = getIntent().getStringExtra("worker_id");
        mWorkerNameStr = getIntent().getStringExtra("worker_name");
        mServiceTypeStr = getIntent().getStringExtra("service_type");
        mExistingRating = getIntent().getDoubleExtra("existing_rating", 0.0);

        if (mRequestId == null || mWorkerId == null) {
            showError("Error: Datos incompletos");
            finish();
        }
    }

    private void showAlreadyRatedDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Servicio Ya Calificado")
                .setMessage("Ya calificaste este servicio con " +
                        String.format("%.1f", mExistingRating) + " estrellas.")
                .setPositiveButton("Entendido", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void initViews() {
        mWorkerName = findViewById(R.id.tv_worker_name_rating);
        mRatingLabel = findViewById(R.id.tv_rating_label);
        findViewById(R.id.btn_rating_close).setOnClickListener(v -> finish());
        mCommentEditText = findViewById(R.id.et_rating_comment);
        mSubmitButton = findViewById(R.id.btn_submit_rating);
        mProgressBar = findViewById(R.id.rating_progress_bar);
    }

    private void setupRatingBar() {
        // «¿Cómo te fue con Carlos?» y sus iniciales
        String name = mWorkerNameStr != null && !mWorkerNameStr.trim().isEmpty()
                ? mWorkerNameStr.trim() : getString(R.string.request_the_worker);
        mWorkerName.setText(getString(R.string.rating_title, name.split(" ")[0]));
        StringBuilder initials = new StringBuilder();
        for (String part : name.split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) initials.append(Character.toUpperCase(part.charAt(0)));
        }
        ((TextView) findViewById(R.id.tv_rating_initials)).setText(initials.toString());

        // Estrellas enteras de 1 a 5 (lo que acepta el backend)
        for (int i = 0; i < STAR_IDS.length; i++) {
            final int value = i + 1;
            findViewById(STAR_IDS[i]).setOnClickListener(v -> {
                Haptics.tick(v);
                setRating(value);
            });
        }
        setRating(0);
    }

    private void setRating(int rating) {
        mRating = rating;
        for (int i = 0; i < STAR_IDS.length; i++) {
            ImageButton star = findViewById(STAR_IDS[i]);
            star.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                    i < rating ? R.color.ads_brand : R.color.ads_line)));
        }
        mRatingLabel.setText(rating > 0 ? getString(RATING_LABELS[rating - 1]) : "");
        mSubmitButton.setEnabled(rating > 0);
    }

    private void setupSubmitButton() {
        mSubmitButton.setOnClickListener(v -> submitRating());
    }

    private void submitRating() {
        float rating = mRating;

        if (rating == 0) {
            showError("Por favor selecciona una calificación");
            return;
        }

        String comment = mCommentEditText.getText().toString().trim();

        // Mostrar loading
        mProgressBar.setVisibility(View.VISIBLE);
        mSubmitButton.setEnabled(false);

        logInfo("Enviando calificación: " + rating + " estrellas");

        // El backend exige el servicio pagado y recalcula el promedio del trabajador
        mRequestProvider.rateService(mRequestId, Math.round(rating), comment, new ApiCallback<RatingData>() {
            @Override
            public void onSuccess(RatingData data) {
                mProgressBar.setVisibility(View.GONE);

                logEvent("service_rated", rating);

                Toast.makeText(RatingActivity.this,
                        "¡Gracias por tu calificación!",
                        Toast.LENGTH_LONG).show();

                // Cerrar actividad y volver atrás
                finish();
            }

            @Override
            public void onError(String message, int httpStatus) {
                mProgressBar.setVisibility(View.GONE);
                mSubmitButton.setEnabled(true);

                logInfo("Error al calificar (HTTP " + httpStatus + ")");
                AdsSnackbar.show(RatingActivity.this, message);
            }
        });
    }

    // Toast: algunos errores cierran la pantalla justo después
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logInfo(String message) {
        Log.d(TAG, message);
        mCrashlytics.log(message);
    }

    private void logError(String method, Exception e) {
        Log.e(TAG, "Error en " + method, e);
        mCrashlytics.recordException(e);
    }

    private void logEvent(String eventName, float rating) {
        Bundle params = new Bundle();
        params.putString("request_id", mRequestId);
        params.putString("worker_id", mWorkerId);
        params.putFloat("rating", rating);
        mAnalytics.logEvent(eventName, params);
    }
}