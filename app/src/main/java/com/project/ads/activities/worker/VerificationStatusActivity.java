package com.project.ads.activities.worker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.providers.AuthProvider;

/**
 * Estado de verificación del trabajador · mockups 34 (en revisión), 35 (rechazada)
 * y 36 (aprobada). Solo lee User/Trabajadores/<uid>/verificationStatus, el mismo dato
 * que cambia el panel admin; la pantalla se actualiza sola cuando el admin decide.
 * Con EXTRA_FROM_REGISTRATION los botones llevan al inicio del trabajador.
 */
public class VerificationStatusActivity extends AppCompatActivity {

    public static final String EXTRA_FROM_REGISTRATION = "from_registration";

    private DatabaseReference mStatusRef;
    private ValueEventListener mStatusListener;
    private boolean mFromRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_status);

        mFromRegistration = getIntent().getBooleanExtra(EXTRA_FROM_REGISTRATION, false);
        String uid = new AuthProvider().getId();
        if (uid == null) {
            finish();
            return;
        }
        mStatusRef = FirebaseDatabase.getInstance().getReference()
                .child("User").child("Trabajadores").child(uid).child("verificationStatus");
        mStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.child("status").getValue(String.class);
                String reason = snapshot.child("rejectionReason").getValue(String.class);
                render(status != null ? status.trim().toLowerCase() : "incomplete", reason);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                render("incomplete", null);
            }
        };
        mStatusRef.addValueEventListener(mStatusListener);
    }

    private void render(String status, String reason) {
        findViewById(R.id.layout_verify_content).setVisibility(View.VISIBLE);
        ImageView icon = findViewById(R.id.iv_verify_icon);
        TextView title = findViewById(R.id.tv_verify_title);
        TextView message = findViewById(R.id.tv_verify_message);
        MaterialButton primary = findViewById(R.id.btn_verify_primary);
        MaterialButton secondary = findViewById(R.id.btn_verify_secondary);
        View steps = findViewById(R.id.card_verify_steps);
        View reasonCard = findViewById(R.id.card_verify_reason);
        View chip = findViewById(R.id.tv_verify_chip);

        steps.setVisibility(View.GONE);
        reasonCard.setVisibility(View.GONE);
        chip.setVisibility(View.GONE);
        secondary.setVisibility(View.GONE);
        primary.setVisibility(View.VISIBLE);

        switch (status) {
            case "approved":
            case "verified":
                icon.setImageResource(R.drawable.ic_ads_verify_approved);
                title.setText(R.string.verify_approved_title);
                message.setText(R.string.verify_approved_message);
                chip.setVisibility(View.VISIBLE);
                primary.setText(R.string.verify_start);
                primary.setOnClickListener(v -> goHome());
                break;
            case "rejected":
                icon.setImageResource(R.drawable.ic_ads_verify_rejected);
                title.setText(R.string.verify_rejected_title);
                message.setText(R.string.verify_rejected_message);
                reasonCard.setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.tv_verify_reason)).setText(
                        reason != null && !reason.trim().isEmpty() ? reason : getString(R.string.verify_reason_missing));
                primary.setText(R.string.verify_reupload);
                primary.setOnClickListener(v -> openDocuments());
                secondary.setVisibility(View.VISIBLE);
                secondary.setOnClickListener(v -> goHome());
                break;
            case "documents_submitted":
            case "pending":
                icon.setImageResource(R.drawable.ic_ads_verify_pending);
                title.setText(R.string.verify_pending_title);
                message.setText(R.string.verify_pending_message);
                steps.setVisibility(View.VISIBLE);
                bindSteps();
                primary.setText(R.string.verify_ok);
                primary.setOnClickListener(v -> goHome());
                break;
            default:
                // Sin documentos todavía (omitió el paso 3 del registro)
                icon.setImageResource(R.drawable.ic_ads_verify_rejected);
                title.setText(R.string.verify_incomplete_title);
                message.setText(R.string.verify_incomplete_message);
                primary.setText(R.string.verify_upload);
                primary.setOnClickListener(v -> openDocuments());
                secondary.setVisibility(View.VISIBLE);
                secondary.setText(R.string.verify_later);
                secondary.setOnClickListener(v -> goHome());
                break;
        }
    }

    /** Documentos enviados (hecho) → En revisión (actual) → Perfil aprobado (pendiente). */
    private void bindSteps() {
        findViewById(R.id.verify_dot_1).setBackgroundResource(R.drawable.bg_ads_step_done);
        findViewById(R.id.verify_check_1).setVisibility(View.VISIBLE);
        findViewById(R.id.verify_line_1).setBackgroundColor(ContextCompat.getColor(this, R.color.ads_brand_text));
        findViewById(R.id.verify_dot_2).setBackgroundResource(R.drawable.bg_ads_step_current);
        findViewById(R.id.verify_dot_3).setBackgroundResource(R.drawable.bg_ads_step_todo);
        for (int id : new int[]{R.id.verify_label_1, R.id.verify_label_2}) {
            TextViewCompat.setTextAppearance(findViewById(id), R.style.TextAppearance_ADS_BodySmall_Strong);
        }
        ((TextView) findViewById(R.id.verify_label_3)).setTextColor(ContextCompat.getColor(this, R.color.ads_muted));
    }

    private void openDocuments() {
        Intent intent = new Intent(this, DocumentStatusActivity.class);
        intent.putExtra("worker_id", new AuthProvider().getId());
        startActivity(intent);
    }

    private void goHome() {
        if (mFromRegistration) {
            Intent intent = new Intent(this, HomeWorkerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        goHome();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mStatusRef != null && mStatusListener != null) mStatusRef.removeEventListener(mStatusListener);
    }
}
