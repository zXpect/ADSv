package com.ads.activities.client;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ads.activities.worker.RequestDetailActivity;
import com.ads.models.ServiceRequest;
import com.ads.providers.AuthProvider;
import com.ads.providers.RequestProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.ads.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyRequestsActivity extends AppCompatActivity {
    private static final String TAG = "MyRequestsActivity";

    // UI Components
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private SwipeRefreshLayout mSwipeRefresh;
    private RecyclerView mRecyclerView;
    private LinearLayout mEmptyState;
    private FrameLayout mLoadingState;

    // Data
    private RequestsAdapter mAdapter;
    private List<ServiceRequest> mAllRequests = new ArrayList<>();
    private List<ServiceRequest> mFilteredRequests = new ArrayList<>();

    // Providers
    private AuthProvider mAuthProvider;
    private RequestProvider mRequestProvider;
    private FirebaseCrashlytics mCrashlytics;
    private FirebaseAnalytics mAnalytics;

    // Listeners
    private ValueEventListener mRequestsListener;

    // Filter
    private String mCurrentFilter = "all"; // all, active, completed, cancelled

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        try {
            initFirebase();
            initProviders();
            initViews();
            setupToolbar();
            setupTabs();
            setupRecyclerView();
            setupSwipeRefresh();
            setupStatusBar();

            loadRequests();

            logEvent("my_requests_opened");

        } catch (Exception e) {
            logError("onCreate", e);
            showError("Error al inicializar la pantalla");
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
        mTabLayout = findViewById(R.id.tab_layout);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);
        mRecyclerView = findViewById(R.id.recycler_requests);
        mEmptyState = findViewById(R.id.empty_state);
        mLoadingState = findViewById(R.id.loading_state);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mis Solicitudes");
        }

        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupTabs() {
        mTabLayout.addTab(mTabLayout.newTab().setText("Todas"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Activas"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Completadas"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Canceladas"));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterRequests(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        mAdapter = new RequestsAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(false);
    }

    private void setupSwipeRefresh() {
        mSwipeRefresh.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );
        mSwipeRefresh.setOnRefreshListener(this::loadRequests);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private void loadRequests() {
        String clientId = mAuthProvider.getId();
        if (clientId == null) {
            showError("Error: Usuario no identificado");
            return;
        }

        showLoading(true);
        logInfo("Cargando solicitudes del cliente: " + clientId);

        // Remover listener anterior si existe
        if (mRequestsListener != null) {
            mRequestProvider.removeClientRequestsListener(mRequestsListener);
        }

        // Crear nuevo listener
        mRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mAllRequests.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        ServiceRequest request = snapshot.getValue(ServiceRequest.class);
                        if (request != null) {
                            // Asegurarse de que el ID esté configurado
                            if (request.getRequest_id() == null) {
                                request.setRequest_id(snapshot.getKey());
                            }
                            mAllRequests.add(request);
                        }
                    } catch (Exception e) {
                        logError("onDataChange_parse", e);
                    }
                }

                // Ordenar por timestamp (más reciente primero)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Collections.sort(mAllRequests, (r1, r2) ->
                            Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                }

                logInfo("Solicitudes cargadas: " + mAllRequests.size());
                showLoading(false);
                mSwipeRefresh.setRefreshing(false);

                // Aplicar filtro actual
                applyCurrentFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                logError("onCancelled", new Exception(databaseError.getMessage()));
                showLoading(false);
                mSwipeRefresh.setRefreshing(false);
                showError("Error al cargar solicitudes");
            }
        };

        mRequestProvider.listenToClientRequests(clientId, mRequestsListener);
    }

    private void filterRequests(int tabPosition) {
        switch (tabPosition) {
            case 0: // Todas
                mCurrentFilter = "all";
                break;
            case 1: // Activas
                mCurrentFilter = "active";
                break;
            case 2: // Completadas
                mCurrentFilter = "completed";
                break;
            case 3: // Canceladas
                mCurrentFilter = "cancelled";
                break;
        }

        applyCurrentFilter();
        logEvent("filter_changed", "filter", mCurrentFilter);
    }

    private void applyCurrentFilter() {
        mFilteredRequests.clear();

        for (ServiceRequest request : mAllRequests) {
            boolean shouldAdd = false;

            switch (mCurrentFilter) {
                case "all":
                    shouldAdd = true;
                    break;
                case "active":
                    shouldAdd = request.isActive();
                    break;
                case "completed":
                    shouldAdd = "completed".equalsIgnoreCase(request.getStatus());
                    break;
                case "cancelled":
                    String status = request.getStatus();
                    shouldAdd = "cancelled".equalsIgnoreCase(status) ||
                            "rejected".equalsIgnoreCase(status);
                    break;
            }

            if (shouldAdd) {
                mFilteredRequests.add(request);
            }
        }

        updateUI();
    }

    private void updateUI() {
        if (mFilteredRequests.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyState.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyState.setVisibility(View.GONE);
        }

        mAdapter.updateRequests(mFilteredRequests);
    }

    private void showLoading(boolean show) {
        mLoadingState.setVisibility(show ? View.VISIBLE : View.GONE);
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

    private void logEvent(String eventName) {
        Bundle params = new Bundle();
        mAnalytics.logEvent(eventName, params);
    }

    private void logEvent(String eventName, String paramKey, String paramValue) {
        Bundle params = new Bundle();
        params.putString(paramKey, paramValue);
        mAnalytics.logEvent(eventName, params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRequestsListener != null) {
            mRequestProvider.removeClientRequestsListener(mRequestsListener);
        }
    }

    // ========== ADAPTER ==========

    private class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {
        private List<ServiceRequest> requests = new ArrayList<>();

        public void updateRequests(List<ServiceRequest> newRequests) {
            this.requests.clear();
            this.requests.addAll(newRequests);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_request_card, parent, false);
            return new RequestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
            ServiceRequest request = requests.get(position);
            holder.bind(request);
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        class RequestViewHolder extends RecyclerView.ViewHolder {
            TextView tvServiceType, tvStatusBadge, tvRequestDate, tvAddress;
            TextView tvWorkerName, tvProgressText;
            ImageView ivWorkerImage, btnCallWorker;
            LinearLayout workerInfoLayout;
            ProgressBar progressBar;
            View cardView;

            RequestViewHolder(View itemView) {
                super(itemView);
                tvServiceType = itemView.findViewById(R.id.tv_service_type);
                tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
                tvRequestDate = itemView.findViewById(R.id.tv_request_date);
                tvAddress = itemView.findViewById(R.id.tv_address);
                tvWorkerName = itemView.findViewById(R.id.tv_worker_name);
                tvProgressText = itemView.findViewById(R.id.tv_progress_text);
                ivWorkerImage = itemView.findViewById(R.id.iv_worker_image);
                btnCallWorker = itemView.findViewById(R.id.btn_call_worker);
                workerInfoLayout = itemView.findViewById(R.id.worker_info_layout);
                progressBar = itemView.findViewById(R.id.progress_bar);
                cardView = itemView.findViewById(R.id.request_card);
            }

            void bind(ServiceRequest request) {
                // Servicio
                tvServiceType.setText(request.getService_type() != null ?
                        request.getService_type() : "Servicio");

                // Estado
                tvStatusBadge.setText(request.getStatusDisplayText());
                tvStatusBadge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor(request.getStatusColor())
                        )
                );

                // Fecha
                tvRequestDate.setText(request.getTimeAgo());

                // Dirección
                tvAddress.setText(request.getAddress() != null ?
                        request.getAddress() : "Sin dirección");

                // Trabajador
                if (request.hasWorkerAssigned()) {
                    workerInfoLayout.setVisibility(View.VISIBLE);
                    tvWorkerName.setText(request.getWorker_name() != null ?
                            request.getWorker_name() : "Trabajador");

                    // Cargar imagen del trabajador
                    if (request.getWorker_image() != null && !request.getWorker_image().isEmpty()) {
                        Glide.with(MyRequestsActivity.this)
                                .load(request.getWorker_image())
                                .placeholder(R.drawable.logo)
                                .circleCrop()
                                .into(ivWorkerImage);
                    } else {
                        ivWorkerImage.setImageResource(R.drawable.logo);
                    }

                    // Botón llamar
                    btnCallWorker.setOnClickListener(v -> {
                        // TODO: Implementar llamada
                        Toast.makeText(MyRequestsActivity.this,
                                "Llamar: " + request.getWorker_phone(),
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    workerInfoLayout.setVisibility(View.GONE);
                }

                // Progreso
                progressBar.setProgress(request.getProgress());
                tvProgressText.setText(getProgressText(request.getStatus()));

                // Click en la card
                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(MyRequestsActivity.this,
                            RequestDetailActivity.class);
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

            private String getProgressText(String status) {
                if (status == null) return "Procesando...";

                switch (status.toLowerCase()) {
                    case "pending":
                        return "Esperando respuesta...";
                    case "accepted":
                        return "Trabajador asignado";
                    case "on_the_way":
                        return "Trabajador en camino";
                    case "in_progress":
                        return "Servicio en progreso";
                    case "completed":
                        return "Servicio completado";
                    case "rejected":
                        return "Solicitud rechazada";
                    case "cancelled":
                        return "Solicitud cancelada";
                    default:
                        return "Estado desconocido";
                }
            }
        }
    }
}