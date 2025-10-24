package com.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ads.models.ServiceRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ads.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ActiveServicesActivity extends AppCompatActivity {
    private static final String TAG = "ActiveServicesActivity";

    // UI Components
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private LinearLayout mEmptyState;
    private SwipeRefreshLayout mSwipeRefresh;
    private ImageButton mBtnBack;
    private TextView mTvTitle;

    // Data
    private String mWorkerId;
    private List<ServiceRequest> mActiveServices;
    private ActiveServicesAdapter mAdapter;

    // Firebase
    private DatabaseReference mDatabaseReference;
    private ValueEventListener mServicesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_services);

        // Obtener worker ID
        mWorkerId = getIntent().getStringExtra("worker_id");

        if (mWorkerId == null || mWorkerId.isEmpty()) {
            Toast.makeText(this, "Error: ID de trabajador no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupStatusBar();
        setupRecyclerView();
        loadActiveServices();
    }

    private void initViews() {
        mRecyclerView = findViewById(R.id.recycler_active_services);
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
        mSwipeRefresh.setOnRefreshListener(this::loadActiveServices);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private void setupRecyclerView() {
        mActiveServices = new ArrayList<>();
        mAdapter = new ActiveServicesAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(false);
    }

    private void loadActiveServices() {
        showLoading(true);
        Log.d(TAG, "Cargando servicios activos para trabajador: " + mWorkerId);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // Remover listener anterior si existe
        if (mServicesListener != null) {
            mDatabaseReference.child("requests").removeEventListener(mServicesListener);
        }

        mServicesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ServiceRequest> services = new ArrayList<>();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Map<String, Object> requestData = (Map<String, Object>) snapshot.getValue();
                            if (requestData != null) {
                                ServiceRequest request = ServiceRequest.fromMap(requestData);
                                request.setRequest_id(snapshot.getKey());

                                if (request != null && isActiveService(request)) {
                                    services.add(request);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parseando solicitud: " + snapshot.getKey(), e);
                        }
                    }
                }

                // Ordenar por timestamp (más reciente primero)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Collections.sort(services, (s1, s2) ->
                            Long.compare(s2.getTimestamp(), s1.getTimestamp()));
                }

                updateUI(services);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error de base de datos: " + databaseError.getMessage());
                showLoading(false);
                Toast.makeText(ActiveServicesActivity.this,
                        "Error al cargar servicios", Toast.LENGTH_SHORT).show();
            }
        };

        mDatabaseReference.child("requests").addValueEventListener(mServicesListener);
    }

    private boolean isActiveService(ServiceRequest request) {
        if (request == null) return false;

        // Verificar que sea del trabajador actual
        if (!mWorkerId.equals(request.getWorker_id())) {
            return false;
        }

        // Verificar que esté en estado activo
        String status = request.getStatus();
        if (status == null) return false;

        switch (status.toLowerCase()) {
            case "accepted":
            case "on_the_way":
            case "in_progress":
                return true;
            default:
                return false;
        }
    }

    private void updateUI(List<ServiceRequest> services) {
        runOnUiThread(() -> {
            mActiveServices.clear();
            mActiveServices.addAll(services);
            mAdapter.notifyDataSetChanged();

            showLoading(false);
            mSwipeRefresh.setRefreshing(false);

            // Actualizar título con cantidad
            mTvTitle.setText("Servicios Activos (" + services.size() + ")");

            if (services.isEmpty()) {
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

    private class ActiveServicesAdapter extends RecyclerView.Adapter<ActiveServicesAdapter.ServiceViewHolder> {

        @NonNull
        @Override
        public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_active_service_worker, parent, false);
            return new ServiceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
            ServiceRequest service = mActiveServices.get(position);
            holder.bind(service);
        }

        @Override
        public int getItemCount() {
            return mActiveServices.size();
        }

        class ServiceViewHolder extends RecyclerView.ViewHolder {
            TextView tvServiceType, tvClientName, tvAddress, tvStatus, tvTimestamp;
            ImageView ivStatusIcon;
            View cardView;

            ServiceViewHolder(View itemView) {
                super(itemView);
                tvServiceType = itemView.findViewById(R.id.tv_service_type);
                tvClientName = itemView.findViewById(R.id.tv_client_name);
                tvAddress = itemView.findViewById(R.id.tv_address);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
                ivStatusIcon = itemView.findViewById(R.id.iv_status_icon);
                cardView = itemView.findViewById(R.id.service_card);
            }

            void bind(ServiceRequest service) {
                // Tipo de servicio
                tvServiceType.setText(service.getService_type() != null ?
                        service.getService_type() : "Servicio");

                // Cliente
                tvClientName.setText(service.getClient_name() != null ?
                        service.getClient_name() : "Cliente");

                // Dirección
                tvAddress.setText(service.getAddress() != null ?
                        service.getAddress() : "Sin dirección");

                // Estado
                tvStatus.setText(service.getStatusDisplayText());
                tvStatus.setTextColor(
                        android.graphics.Color.parseColor(service.getStatusColor())
                );

                // Ícono según estado
                setStatusIcon(service.getStatus());

                // Tiempo
                tvTimestamp.setText(service.getTimeAgo());

                // Click listener
                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(ActiveServicesActivity.this,
                            ServiceDetailWorkerActivity.class);
                    intent.putExtra("request_id", service.getRequest_id());
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

            private void setStatusIcon(String status) {
                if (status == null) {
                    ivStatusIcon.setImageResource(R.drawable.ic_pending);
                    return;
                }

                switch (status.toLowerCase()) {
                    case "accepted":
                        ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                        break;
                    case "on_the_way":
                        //ivStatusIcon.setImageResource(R.drawable.ic_directions);
                        break;
                    case "in_progress":
                        ivStatusIcon.setImageResource(R.drawable.ic_work_active);
                        break;
                    default:
                        ivStatusIcon.setImageResource(R.drawable.ic_pending);
                        break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServicesListener != null && mDatabaseReference != null) {
            mDatabaseReference.child("requests").removeEventListener(mServicesListener);
        }
    }
}