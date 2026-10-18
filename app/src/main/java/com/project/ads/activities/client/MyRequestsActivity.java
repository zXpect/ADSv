package com.project.ads.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.project.ads.R;
import com.project.ads.helpers.ReasonHelper;
import com.project.ads.models.ServiceRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyRequestsActivity extends AppCompatActivity {

    private static final String TAG = "MyRequestsActivity";

    // UI Components
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private LinearLayout mEmptyState;
    private SwipeRefreshLayout mSwipeRefresh;
    private ImageButton mBtnBack;
    private TextView mTvTitle;

    // Data
    private String mClientId;
    private List<ServiceRequest> mRequests;
    private RequestsAdapter mAdapter;

    // Firebase
    private DatabaseReference mDatabaseReference;
    private ValueEventListener mRequestsListener;
    private Query mRequestsQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        // Obtener client ID
        mClientId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (mClientId == null || mClientId.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadMyRequests();
    }

    private void initViews() {
        mRecyclerView = findViewById(R.id.recycler_my_requests);
        mProgressBar = findViewById(R.id.progress_bar);
        mEmptyState = findViewById(R.id.empty_state);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);
        mBtnBack = findViewById(R.id.btn_back);
        mTvTitle = findViewById(R.id.tv_title);

        mBtnBack.setOnClickListener(v -> finish());

        mSwipeRefresh.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );
        mSwipeRefresh.setOnRefreshListener(this::loadMyRequests);

        mTvTitle.setText(R.string.history_title);
    }

    private void setupRecyclerView() {
        mRequests = new ArrayList<>();
        mAdapter = new RequestsAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(false);
    }

    private void loadMyRequests() {
        showLoading(true);
        Log.d(TAG, "Cargando solicitudes para cliente: " + mClientId);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // Remover listener anterior si existe
        if (mRequestsListener != null && mRequestsQuery != null) {
            mRequestsQuery.removeEventListener(mRequestsListener);
        }

        // Las reglas de Firebase solo permiten leer requests filtrado por el propio uid
        mRequestsQuery = mDatabaseReference.child("requests")
                .orderByChild("client_id")
                .equalTo(mClientId);

        mRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ServiceRequest> requests = new ArrayList<>();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Map<String, Object> requestData = (Map<String, Object>) snapshot.getValue();
                            if (requestData != null) {
                                ServiceRequest request = ServiceRequest.fromMap(requestData);
                                request.setRequest_id(snapshot.getKey());

                                // Solo agregar solicitudes de este cliente
                                if (request != null && isMyRequest(request)) {
                                    requests.add(request);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parseando solicitud: " + snapshot.getKey(), e);
                        }
                    }
                }

                // Activas primero (mockup 41) y, dentro de cada grupo, la más reciente primero
                Collections.sort(requests, (r1, r2) -> {
                    int byGroup = Boolean.compare(isActive(r2), isActive(r1));
                    return byGroup != 0 ? byGroup : Long.compare(r2.getTimestamp(), r1.getTimestamp());
                });

                updateUI(requests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error de base de datos: " + databaseError.getMessage());
                showLoading(false);
                Toast.makeText(MyRequestsActivity.this,
                        "Error al cargar solicitudes", Toast.LENGTH_SHORT).show();
            }
        };

        mRequestsQuery.addValueEventListener(mRequestsListener);
    }

    private boolean isMyRequest(ServiceRequest request) {
        return request != null &&
                request.getClient_id() != null &&
                request.getClient_id().equals(mClientId);
    }

    private void updateUI(List<ServiceRequest> requests) {
        runOnUiThread(() -> {
            mRequests.clear();
            mRequests.addAll(requests);
            mAdapter.notifyDataSetChanged();

            showLoading(false);
            mSwipeRefresh.setRefreshing(false);

            if (requests.isEmpty()) {
                mRecyclerView.setVisibility(View.GONE);
                mEmptyState.setVisibility(View.VISIBLE);
            } else {
                mRecyclerView.setVisibility(View.VISIBLE);
                mEmptyState.setVisibility(View.GONE);
            }
        });
    }

    /** Solicitud en curso: todavía no termina ni se canceló. */
    private static boolean isActive(ServiceRequest request) {
        String status = request.getStatus();
        return status != null && !status.equals("completed")
                && !status.equals("cancelled") && !status.equals("rejected");
    }

    private void showLoading(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ========== ADAPTER ==========

    private class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {

        @NonNull
        @Override
        public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_request, parent, false);
            return new RequestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
            ServiceRequest request = mRequests.get(position);
            holder.bind(request);
        }

        @Override
        public int getItemCount() {
            return mRequests.size();
        }

        class RequestViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            TextView tvSection, tvServiceType, tvWorkerName, tvStatus, tvDetail;

            RequestViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_request);
                tvSection = itemView.findViewById(R.id.tv_section);
                tvServiceType = itemView.findViewById(R.id.tv_service_type);
                tvWorkerName = itemView.findViewById(R.id.tv_worker_name);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvDetail = itemView.findViewById(R.id.tv_timestamp);
            }

            void bind(ServiceRequest request) {
                int position = getBindingAdapterPosition();
                boolean active = isActive(request);

                // Etiqueta de sección en el primer ítem de cada grupo
                boolean firstOfGroup = position <= 0
                        || isActive(mRequests.get(position - 1)) != active;
                tvSection.setVisibility(firstOfGroup ? View.VISIBLE : View.GONE);
                tvSection.setText(active ? R.string.history_section_active
                        : R.string.history_section_previous);

                tvServiceType.setText(request.getService_type() != null
                        ? request.getService_type() : getString(R.string.history_service_default));

                // Técnico · fecha (la agendada si existe)
                String worker = request.getWorker_name() != null
                        ? request.getWorker_name() : getString(R.string.history_worker_pending);
                tvWorkerName.setText(worker + " · " + formatDate(request, active));

                bindStatus(request);
                bindDetail(request, active);

                // La tarjeta activa lleva borde naranja (como Widget.ADS.Card.Highlight)
                cardView.setStrokeColor(ContextCompat.getColor(MyRequestsActivity.this,
                        active ? R.color.ads_brand_text : R.color.ads_line));
                cardView.setStrokeWidth(getResources().getDimensionPixelSize(
                        active ? R.dimen.stroke_focus : R.dimen.stroke_control));

                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(MyRequestsActivity.this,
                            ServiceRequestDetailActivity.class);
                    intent.putExtra("request_id", request.getRequest_id());
                    startActivity(intent);
                });
            }

            /** Chip de estado: naranja en curso, navy completada, gris cancelada. */
            private void bindStatus(ServiceRequest request) {
                String status = request.getStatus() != null ? request.getStatus() : "";
                int label;
                int background = R.color.ads_chip_bg;
                int text = R.color.ads_chip_text;
                switch (status) {
                    case "pending":
                        label = "quoted".equals(request.getQuote_status())
                                ? R.string.request_status_quoted : R.string.request_status_pending;
                        break;
                    case "accepted":
                        label = R.string.request_status_accepted;
                        break;
                    case "on_the_way":
                        label = R.string.request_status_on_the_way;
                        break;
                    case "in_progress":
                        label = R.string.request_status_in_progress;
                        break;
                    case "completed":
                        label = R.string.request_status_completed;
                        background = R.color.ads_text;
                        text = R.color.ads_surface;
                        break;
                    case "rejected":
                        label = R.string.request_status_rejected;
                        background = R.color.ads_surface_alt;
                        text = R.color.ads_charcoal;
                        break;
                    default:
                        label = R.string.request_status_cancelled;
                        background = R.color.ads_surface_alt;
                        text = R.color.ads_charcoal;
                }
                tvStatus.setText(label);
                ViewCompat.setBackgroundTintList(tvStatus, ColorStateList.valueOf(
                        ContextCompat.getColor(MyRequestsActivity.this, background)));
                tvStatus.setTextColor(ContextCompat.getColor(MyRequestsActivity.this, text));
            }

            /** Precio (activas), calificación (completadas) o motivo (canceladas). */
            private void bindDetail(ServiceRequest request, boolean active) {
                tvDetail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                String status = request.getStatus();
                if (active && request.getQuoted_price() > 0) {
                    TextViewCompat.setTextAppearance(tvDetail, R.style.TextAppearance_ADS_Price);
                    tvDetail.setText(ServiceRequest.formatCop(request.getQuoted_price()));
                } else if ("completed".equals(status) && request.getRating() > 0) {
                    TextViewCompat.setTextAppearance(tvDetail, R.style.TextAppearance_ADS_BodySmall_Strong);
                    tvDetail.setText(String.format(new Locale("es", "CO"), "%.1f", request.getRating()));
                    tvDetail.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_filled, 0, 0, 0);
                } else if ("cancelled".equals(status) && request.getCancellationReasonCode() != null) {
                    TextViewCompat.setTextAppearance(tvDetail, R.style.TextAppearance_ADS_Caption);
                    tvDetail.setTextColor(ContextCompat.getColor(MyRequestsActivity.this, R.color.ads_charcoal));
                    tvDetail.setText(getString(R.string.history_cancel_reason, ReasonHelper.cancellationLabel(
                            request.getCancellationReasonCode(), request.getCancellationReasonText())));
                } else {
                    tvDetail.setVisibility(View.GONE);
                    return;
                }
                tvDetail.setVisibility(View.VISIBLE);
            }

            /** "Lun 19 oct, 3:00 p. m." para activas agendadas; "3 oct" para el resto. */
            private String formatDate(ServiceRequest request, boolean active) {
                Locale locale = new Locale("es", "CO");
                if (active && request.getScheduledAt() > 0) {
                    String text = new SimpleDateFormat("EEE d MMM, h:mm a", locale)
                            .format(new Date(request.getScheduledAt()));
                    return text.substring(0, 1).toUpperCase(locale) + text.substring(1);
                }
                long time = request.getTimestamp();
                return time > 0 ? new SimpleDateFormat("d MMM", locale).format(new Date(time))
                        : request.getTimeAgo();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRequestsListener != null && mRequestsQuery != null) {
            mRequestsQuery.removeEventListener(mRequestsListener);
        }
    }
}