package com.ads.activities.client;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ads.providers.RequestProvider;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.ads.R;

public class RatingActivity extends AppCompatActivity {
    private static final String TAG = "RatingActivity";

    // UI Components
    private Toolbar mToolbar;
    private TextView mWorkerName;
    private TextView mServiceType;
    private RatingBar mRatingBar;
    private EditText mCommentEditText;
    private Button mSubmitButton;
    private ProgressBar mProgressBar;

    // Data
    private String mRequestId;
    private String mWorkerId;
    private String mWorkerNameStr;
    private String mServiceTypeStr;

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
            initViews();
            setupToolbar();
            setupStatusBar();
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

        if (mRequestId == null || mWorkerId == null) {
            showError("Error: Datos incompletos");
            finish();
        }
    }

    private void initViews() {
        mToolbar = findViewById(R.id.toolbar);
        mWorkerName = findViewById(R.id.tv_worker_name_rating);
        mServiceType = findViewById(R.id.tv_service_type_rating);
        mRatingBar = findViewById(R.id.rating_bar);
        mCommentEditText = findViewById(R.id.et_rating_comment);
        mSubmitButton = findViewById(R.id.btn_submit_rating);
        mProgressBar = findViewById(R.id.rating_progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Calificar Servicio");
        }
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private void setupRatingBar() {
        // Mostrar información del trabajador
        if (mWorkerNameStr != null) {
            mWorkerName.setText(mWorkerNameStr);
        }
        if (mServiceTypeStr != null) {
            mServiceType.setText("Servicio: " + mServiceTypeStr);
        }

        // Configurar rating bar
        mRatingBar.setRating(0);
        mRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            // Habilitar botón solo si hay rating
            mSubmitButton.setEnabled(rating > 0);
        });

        // Deshabilitar botón inicialmente
        mSubmitButton.setEnabled(false);
    }

    private void setupSubmitButton() {
        mSubmitButton.setOnClickListener(v -> submitRating());
    }

    private void submitRating() {
        float rating = mRatingBar.getRating();

        if (rating == 0) {
            showError("Por favor selecciona una calificación");
            return;
        }

        String comment = mCommentEditText.getText().toString().trim();

        // Mostrar loading
        mProgressBar.setVisibility(View.VISIBLE);
        mSubmitButton.setEnabled(false);

        logInfo("Enviando calificación: " + rating + " estrellas");

        mRequestProvider.rateRequest(mRequestId, rating, comment)
                .addOnSuccessListener(aVoid -> {
                    mProgressBar.setVisibility(View.GONE);

                    logEvent("service_rated", rating);

                    Toast.makeText(this,
                            "¡Gracias por tu calificación!",
                            Toast.LENGTH_LONG).show();

                    // Cerrar actividad y volver atrás
                    finish();
                })
                .addOnFailureListener(e -> {
                    mProgressBar.setVisibility(View.GONE);
                    mSubmitButton.setEnabled(true);

                    logError("submitRating", e);
                    showError("Error al enviar calificación. Intenta de nuevo.");
                });
    }

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