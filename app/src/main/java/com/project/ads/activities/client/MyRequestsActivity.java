package com.project.ads.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.project.ads.R;
import com.project.ads.models.ServiceRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        setupStatusBar();
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

        mTvTitle.setText("Mis Solicitudes");
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
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

                // Ordenar por timestamp (más reciente primero)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Collections.sort(requests, (r1, r2) ->
                            Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                }

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

            // Actualizar título con cantidad
            mTvTitle.setText("Mis Solicitudes (" + requests.size() + ")");

            if (requests.isEmpty()) {
                mRecyclerView.setVisibility(View.GONE);
                mEmptyState.setVisibility(View.VISIBLE);
            } else {
                mRecyclerView.setVisibility(View.VISIBLE);
                mEmptyState.setVisibility(View.GONE);
            }
        });
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
            CardView cardView;
            TextView tvServiceType, tvWorkerName, tvAddress, tvStatus, tvTimestamp;
            View statusIndicator;

            RequestViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_request);
                tvServiceType = itemView.findViewById(R.id.tv_service_type);
                tvWorkerName = itemView.findViewById(R.id.tv_worker_name);
                tvAddress = itemView.findViewById(R.id.tv_address);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
                statusIndicator = itemView.findViewById(R.id.status_indicator);
            }

            void bind(ServiceRequest request) {
                // Tipo de servicio
                tvServiceType.setText(request.getService_type() != null ?
                        request.getService_type() : "Servicio");

                // Trabajador
                tvWorkerName.setText(request.getWorker_name() != null ?
                        request.getWorker_name() : "Trabajador asignado");

                // Dirección
                tvAddress.setText(request.getAddress() != null ?
                        request.getAddress() : "Sin dirección");

                // Estado
                tvStatus.setText(request.getStatusDisplayText());
                int statusColor = android.graphics.Color.parseColor(request.getStatusColor());
                tvStatus.setTextColor(statusColor);
                statusIndicator.setBackgroundColor(statusColor);

                // Tiempo
                tvTimestamp.setText(request.getTimeAgo());

                // Click listener para abrir detalles
                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(MyRequestsActivity.this,
                            ServiceRequestDetailActivity.class);
                    intent.putExtra("request_id", request.getRequest_id());
                    startActivity(intent);
                });

                // Animación de entrada
                cardView.setAlpha(0f);
                cardView.setTranslationY(50f);
                cardView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setStartDelay(getAdapterPosition() * 50L)
                        .start();
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