package com.ads.activities.client;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.ads.models.ServiceRequest;
import com.ads.providers.AuthProvider;
import com.ads.providers.RequestProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.ads.R;

public class RequestDetailClientActivity  extends AppCompatActivity {
    private static final String TAG = "RequestDetailActivity";

    // UI Components
    private Toolbar mToolbar;
    private ImageView mWorkerImage;
    private TextView mServiceType, mStatus, mAddress, mDescription;
    private TextView mWorkerName, mWorkerPhone, mRequestDate;
    private TextView mTimelineStep1, mTimelineStep2, mTimelineStep3, mTimelineStep4, mTimelineStep5;
    private View mTimelineLine1, mTimelineLine2, mTimelineLine3, mTimelineLine4;
    private ImageView mTimelineIcon1, mTimelineIcon2, mTimelineIcon3, mTimelineIcon4, mTimelineIcon5;
    private CardView mWorkerCard;
    private LinearLayout mActionsLayout;
    private MaterialButton mPrimaryActionButton, mSecondaryActionButton;
    private ProgressBar mProgressBar;

    // Data
    private String mRequestId;
    private ServiceRequest mRequest;
    private ValueEventListener mRequestListener;

    // Providers
    private AuthProvider mAuthProvider;
    private RequestProvider mRequestProvider;
    private FirebaseCrashlytics mCrashlytics;
    private FirebaseAnalytics mAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail_client);

        try {
            initFirebase();
            initProviders();

            mRequestId = getIntent().getStringExtra("request_id");
            if (mRequestId == null) {
                showError("Error: ID de solicitud no válido");
                finish();
                return;
            }

            initViews();
            setupToolbar();
            setupStatusBar();

            loadRequestDetails();

        } catch (Exception e) {
            logError("onCreate", e);
            showError("Error al cargar detalles");
            finish();
        }
    }

    private void initFirebase() {
        mCrashlytics = FirebaseCrashlytics.getInstance();
        mAnalytics = FirebaseAnalytics.getInstance(this);
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mRequestProvider = new RequestProvider(this);
    }

    private void initViews() {
        mToolbar = findViewById(R.id.toolbar);
        mWorkerImage = findViewById(R.id.iv_worker_photo);
        mServiceType = findViewById(R.id.tv_service_type);
        mStatus = findViewById(R.id.tv_status);
        mAddress = findViewById(R.id.tv_address);
        mDescription = findViewById(R.id.tv_description);
        mWorkerName = findViewById(R.id.tv_worker_name);
        mWorkerPhone = findViewById(R.id.tv_worker_phone);
        mRequestDate = findViewById(R.id.tv_request_date);
        mWorkerCard = findViewById(R.id.worker_card);
        mActionsLayout = findViewById(R.id.actions_layout);
        mPrimaryActionButton = findViewById(R.id.btn_primary_action);
        mSecondaryActionButton = findViewById(R.id.btn_secondary_action);
        mProgressBar = findViewById(R.id.detail_progress_bar);

        // Timeline
        mTimelineStep1 = findViewById(R.id.tv_timeline_step1);
        mTimelineStep3 = findViewById(R.id.tv_timeline_step3);
        mTimelineStep4 = findViewById(R.id.tv_timeline_step4);
        mTimelineStep5 = findViewById(R.id.tv_timeline_step5);


        mTimelineLine2 = findViewById(R.id.timeline_line2);
        mTimelineLine3 = findViewById(R.id.timeline_line3);
        mTimelineLine4 = findViewById(R.id.timeline_line4);

        mTimelineIcon1 = findViewById(R.id.timeline_icon1);
        mTimelineIcon3 = findViewById(R.id.timeline_icon3);
        mTimelineIcon4 = findViewById(R.id.timeline_icon4);
        mTimelineIcon5 = findViewById(R.id.timeline_icon5);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalles de Solicitud");
        }
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private void loadRequestDetails() {
        mProgressBar.setVisibility(View.VISIBLE);

        mRequestListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mProgressBar.setVisibility(View.GONE);

                if (dataSnapshot.exists()) {
                    mRequest = dataSnapshot.getValue(ServiceRequest.class);
                    if (mRequest != null) {
                        mRequest.setRequest_id(dataSnapshot.getKey());
                        updateUI();
                    } else {
                        showError("Error al cargar datos");
                        finish();
                    }
                } else {
                    showError("Solicitud no encontrada");
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mProgressBar.setVisibility(View.GONE);
                logError("onCancelled", new Exception(databaseError.getMessage()));
                showError("Error al cargar solicitud");
            }
        };

        mRequestProvider.listenToRequest(mRequestId, mRequestListener);
    }

    private void updateUI() {
        // Información básica
        mServiceType.setText(mRequest.getService_type());
        mStatus.setText(mRequest.getStatusDisplayText());
        mStatus.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor(mRequest.getStatusColor())
                )
        );
        mAddress.setText(mRequest.getAddress());
        mDescription.setText(mRequest.getDescription());
        mRequestDate.setText(mRequest.getFormattedTimestamp());

        // Información del trabajador
        if (mRequest.hasWorkerAssigned()) {
            mWorkerCard.setVisibility(View.VISIBLE);
            mWorkerName.setText(mRequest.getWorker_name());
            mWorkerPhone.setText(mRequest.getWorker_phone() != null ?
                    mRequest.getWorker_phone() : "No disponible");

            if (mRequest.getWorker_image() != null && !mRequest.getWorker_image().isEmpty()) {
                Glide.with(this)
                        .load(mRequest.getWorker_image())
                        .placeholder(R.drawable.logo)
                        .circleCrop()
                        .into(mWorkerImage);
            }

            // Click para llamar
            mWorkerPhone.setOnClickListener(v -> callWorker());
        } else {
            mWorkerCard.setVisibility(View.GONE);
        }

        // Timeline
        updateTimeline();

        // Botones de acción
        updateActionButtons();
    }

    private void updateTimeline() {
        int currentStage = mRequest.getCurrentStage();

        updateTimelineStep(1, "Solicitado", currentStage >= 1,
                mTimelineIcon1, mTimelineStep1, null);
        updateTimelineStep(2, "Aceptado", currentStage >= 2,
                mTimelineIcon2, mTimelineStep2, mTimelineLine1);
        updateTimelineStep(3, "En camino", currentStage >= 3,
                mTimelineIcon3, mTimelineStep3, mTimelineLine2);
        updateTimelineStep(4, "En progreso", currentStage >= 4,
                mTimelineIcon4, mTimelineStep4, mTimelineLine3);
        updateTimelineStep(5, "Completado", currentStage >= 5,
                mTimelineIcon5, mTimelineStep5, mTimelineLine4);
    }

    private void updateTimelineStep(int step, String label, boolean completed,
                                    ImageView icon, TextView text, View line) {
        text.setText(label);

        if (completed) {
            icon.setColorFilter(getResources().getColor(R.color.success_color));
            text.setTextColor(getResources().getColor(R.color.text_primary));
            if (line != null) {
                line.setBackgroundColor(getResources().getColor(R.color.success_color));
            }
        } else {
            icon.setColorFilter(getResources().getColor(R.color.gray_medium));
            text.setTextColor(getResources().getColor(R.color.text_secondary));
            if (line != null) {
                line.setBackgroundColor(getResources().getColor(R.color.gray_medium));
            }
        }
    }

    private void updateActionButtons() {
        mActionsLayout.setVisibility(View.VISIBLE);

        String status = mRequest.getStatus();
        if (status == null) {
            mActionsLayout.setVisibility(View.GONE);
            return;
        }

        switch (status.toLowerCase()) {
            case "pending":
            case "accepted":
                // Puede cancelar
                mPrimaryActionButton.setText("Cancelar Solicitud");
                mPrimaryActionButton.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                getResources().getColor(R.color.error_color)
                        )
                );
                mPrimaryActionButton.setOnClickListener(v -> showCancelDialog());
                mPrimaryActionButton.setVisibility(View.VISIBLE);
                mSecondaryActionButton.setVisibility(View.GONE);
                break;

            case "completed":
                if (mRequest.isIs_rated()) {
                    // Ya calificó
                    mPrimaryActionButton.setText("Ver Calificación");
                    mPrimaryActionButton.setOnClickListener(v -> showRatingInfo());
                } else {
                    // Puede calificar
                    mPrimaryActionButton.setText("Calificar Servicio");
                    mPrimaryActionButton.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    getResources().getColor(R.color.colorPrimary)
                            )
                    );
                    mPrimaryActionButton.setOnClickListener(v -> openRatingActivity());
                }
                mPrimaryActionButton.setVisibility(View.VISIBLE);
                mSecondaryActionButton.setVisibility(View.GONE);
                break;

            case "on_the_way":
            case "in_progress":
                // Mostrar opciones de contacto
                mPrimaryActionButton.setText("Llamar Trabajador");
                mPrimaryActionButton.setOnClickListener(v -> callWorker());
                mSecondaryActionButton.setText("Ver en Mapa");
                mSecondaryActionButton.setOnClickListener(v -> viewOnMap());
                mPrimaryActionButton.setVisibility(View.VISIBLE);
                mSecondaryActionButton.setVisibility(View.VISIBLE);
                break;

            default:
                mActionsLayout.setVisibility(View.GONE);
                break;
        }
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar Solicitud")
                .setMessage("¿Estás seguro de que deseas cancelar esta solicitud?")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                    cancelRequest("Cancelado por el cliente");
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelRequest(String reason) {
        mProgressBar.setVisibility(View.VISIBLE);

        mRequestProvider.cancelRequest(mRequestId, reason)
                .addOnSuccessListener(aVoid -> {
                    mProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Solicitud cancelada", Toast.LENGTH_SHORT).show();
                    logEvent("request_cancelled");
                })
                .addOnFailureListener(e -> {
                    mProgressBar.setVisibility(View.GONE);
                    logError("cancelRequest", e);
                    showError("Error al cancelar solicitud");
                });
    }

    private void openRatingActivity() {
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("request_id", mRequestId);
        intent.putExtra("worker_id", mRequest.getWorker_id());
        intent.putExtra("worker_name", mRequest.getWorker_name());
        intent.putExtra("service_type", mRequest.getService_type());
        startActivity(intent);
    }

    private void showRatingInfo() {
        String message = "Calificación: " + mRequest.getClient_rating() + " estrellas";
        if (mRequest.getClient_comment() != null && !mRequest.getClient_comment().isEmpty()) {
            message += "\n\nComentario: " + mRequest.getClient_comment();
        }

        new AlertDialog.Builder(this)
                .setTitle("Tu Calificación")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void callWorker() {
        if (mRequest.getWorker_phone() != null && !mRequest.getWorker_phone().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + mRequest.getWorker_phone()));
            startActivity(intent);
        } else {
            showError("Teléfono no disponible");
        }
    }

    private void viewOnMap() {
        // TODO: Implementar vista en mapa
        Toast.makeText(this, "Función de mapa en desarrollo", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logError(String method, Exception e) {
        Log.e(TAG, "Error en " + method, e);
        mCrashlytics.recordException(e);
    }

    private void logEvent(String eventName) {
        Bundle params = new Bundle();
        params.putString("request_id", mRequestId);
        mAnalytics.logEvent(eventName, params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRequestListener != null) {
            mRequestProvider.removeRequestListener(mRequestId, mRequestListener);
        }
    }
}