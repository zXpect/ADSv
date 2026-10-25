package com.project.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.project.ads.helpers.Motion;
import com.project.ads.adapters.PendingRequestsAdapter;
import com.project.ads.helpers.WorkerNavHelper;
import com.project.ads.models.ServiceRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.helpers.AdsSnackbar;
import com.project.ads.helpers.RequestsNavBadge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bandeja de solicitudes del trabajador · mockup 14 (docs/mockups/Bandeja.dc.html).
 * Pestañas: Nuevas (por responder o cotizar), Cotizadas (esperando al cliente) y
 * Activas (aceptadas, en camino o en progreso). Lee requests filtrado por su uid.
 */
public class PendingRequestsActivity extends AppCompatActivity implements PendingRequestsAdapter.OnRequestClickListener {

    private RequestsNavBadge mRequestsBadge;

    private static final String TAG = "PendingRequestsActivity";

    // UI Components
    private RecyclerView recyclerView;
    private View progressBar;   // tarjetas de relleno (AdsSkeletonView)
    private LinearLayout tvEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private ImageButton btnBack;
    private TextView tvTitle;

    // Data
    private String workerId;
    private String workerType;
    private List<ServiceRequest> pendingRequests;
    private final List<ServiceRequest> allRequests = new ArrayList<>();
    private int currentTab = R.id.chip_inbox_new;
    private PendingRequestsAdapter adapter;

    // Firebase
    private DatabaseReference databaseReference;
    private ValueEventListener requestsListener;
    private Query requestsQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        // Get data from intent
        workerId = getIntent().getStringExtra("worker_id");
        workerType = getIntent().getStringExtra("worker_type");

        Log.d(TAG, "Worker Type: " + workerType);

        if (workerId == null || workerId.isEmpty()) {
            Toast.makeText(this, "Error: ID de trabajador no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        WorkerNavHelper.setup(this, R.id.nav_worker_requests);
        mRequestsBadge = RequestsNavBadge.forWorker(this);
        loadPendingRequests();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewRequests);
        // Encabezado que se eleva al desplazar la lista
        com.project.ads.helpers.Motion.liftOnScroll(findViewById(R.id.layout_list_header), recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);

        btnBack.setOnClickListener(v -> finish());

        swipeRefresh.setOnRefreshListener(() -> {
            loadPendingRequests();
        });

        tvTitle.setText(R.string.inbox_title);

        com.google.android.material.chip.ChipGroup tabs = findViewById(R.id.chip_group_inbox);
        tabs.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            currentTab = checkedIds.get(0);
            Motion.animateList(recyclerView);
            applyTab();
        });

        // Verificar que todas las vistas se inicializaron correctamente
        if (tvEmpty == null) {
            Log.w(TAG, "tvEmpty is null - check layout");
        }
    }

    private void setupRecyclerView() {
        pendingRequests = new ArrayList<>();
        adapter = new PendingRequestsAdapter(pendingRequests, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // Add subtle animation for better UX
        recyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
    }

    private void loadPendingRequests() {
        showLoading(true);
        Log.d(TAG, "Loading pending requests...");

        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Remove previous listener if exists
        if (requestsListener != null && requestsQuery != null) {
            requestsQuery.removeEventListener(requestsListener);
        }

        // Las reglas de Firebase solo permiten leer requests filtrado por el propio uid;
        // el filtro por estado (pending) se sigue haciendo localmente
        requestsQuery = databaseReference.child("requests")
                .orderByChild("worker_id")
                .equalTo(workerId);

        requestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ServiceRequest> requests = new ArrayList<>();

                Log.d(TAG, "DataSnapshot exists: " + dataSnapshot.exists());
                Log.d(TAG, "Children count: " + dataSnapshot.getChildrenCount());
                Log.d(TAG, "Current worker type: " + workerType);

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Map<String, Object> requestData = (Map<String, Object>) snapshot.getValue();
                            if (requestData != null) {
                                Log.d(TAG, "Processing request: " + snapshot.getKey());
                                Log.d(TAG, "Request data: " + requestData.toString());
                                Log.d(TAG, "Request keys: " + requestData.keySet().toString());

                                ServiceRequest request = ServiceRequest.fromMap(requestData);
                                request.setRequest_id(snapshot.getKey()); // Asegurar que el ID esté configurado

                                if (request != null) {
                                    Log.d(TAG, "Request status: " + request.getStatus() +
                                            ", Service type: " + request.getService_type() +
                                            ", Worker ID: " + request.getWorker_id());

                                    if (isRequestRelevant(request)) {
                                        requests.add(request);
                                        Log.d(TAG, "Request added to list");
                                    } else {
                                        Log.d(TAG, "Request not relevant");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing request data for " + snapshot.getKey(), e);
                        }
                    }
                } else {
                    Log.w(TAG, "No data found in requests");
                }

                Log.d(TAG, "Total relevant requests found: " + requests.size());
                
                // Log details of each relevant request
                for (int i = 0; i < requests.size(); i++) {
                    ServiceRequest req = requests.get(i);
                    Log.d(TAG, "Request " + i + ": ID=" + req.getRequest_id() + 
                            ", Status=" + req.getStatus() + 
                            ", WorkerID=" + req.getWorker_id() + 
                            ", ServiceType=" + req.getService_type());
                }
                
                updateUI(requests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                showError("Error al cargar solicitudes: " + databaseError.getMessage());
            }
        };

        requestsQuery.addValueEventListener(requestsListener);
    }

    /** Solicitudes del trabajador que todavía no terminaron ni se cancelaron. */
    private boolean isRequestRelevant(ServiceRequest request) {
        if (request == null || request.getStatus() == null) return false;
        if (workerId == null || !workerId.equals(request.getWorker_id())) return false;
        String status = request.getStatus().toLowerCase();
        return status.equals("pending") || status.equals("accepted")
                || status.equals("on_the_way") || status.equals("in_progress");
    }

    /** Pestaña de cada solicitud: nueva, cotizada o activa. */
    private int tabOf(ServiceRequest request) {
        String status = request.getStatus().toLowerCase();
        if (status.equals("on_the_way") || status.equals("in_progress")) return R.id.chip_inbox_active;
        if (status.equals("accepted")) {
            if (request.isQuoteAccepted()) return R.id.chip_inbox_active;
            if (request.hasQuote()) return R.id.chip_inbox_quoted;
        }
        return R.id.chip_inbox_new;   // pendiente o aceptada sin cotizar: hay que responder
    }

    private void applyTab() {
        pendingRequests.clear();
        int newCount = 0;
        for (ServiceRequest request : allRequests) {
            int tab = tabOf(request);
            if (tab == R.id.chip_inbox_new) newCount++;
            if (tab == currentTab) pendingRequests.add(request);
        }
        adapter.notifyDataSetChanged();
        Motion.animateListOnce(recyclerView);

        ((com.google.android.material.chip.Chip) findViewById(R.id.chip_inbox_new)).setText(newCount > 0
                ? getString(R.string.inbox_tab_new_count, newCount) : getString(R.string.inbox_tab_new));
        findViewById(R.id.card_inbox_notice).setVisibility(
                currentTab == R.id.chip_inbox_new && !pendingRequests.isEmpty() ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.tv_inbox_empty)).setText(currentTab == R.id.chip_inbox_new
                ? R.string.inbox_empty_new : currentTab == R.id.chip_inbox_quoted
                ? R.string.inbox_empty_quoted : R.string.inbox_empty_active);
        showEmpty(pendingRequests.isEmpty());
    }

    private void updateUI(List<ServiceRequest> requests) {
        runOnUiThread(() -> {
            // Más recientes primero
            java.util.Collections.sort(requests, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            allRequests.clear();
            allRequests.addAll(requests);
            showLoading(false);
            applyTab();
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void showEmpty(boolean show) {
        runOnUiThread(() -> {
            if (tvEmpty != null) {
                tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
                // No need to set text since it's already defined in the layout
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void showError(String message) {
        showLoading(false);
        AdsSnackbar.showLong(this, message);
    }

    @Override
    public void onRequestClick(ServiceRequest request) {
        if (request != null && request.getRequest_id() != null) {
            Log.d(TAG, "Opening request detail for: " + request.getRequest_id());
            boolean isNew = "pending".equalsIgnoreCase(request.getStatus());
            Intent intent = new Intent(this, isNew ? RequestDetailActivity.class : ServiceDetailWorkerActivity.class);
            intent.putExtra("request_id", request.getRequest_id());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Error: Datos de solicitud no válidos", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mRequestsBadge.start();
    }

    @Override
    protected void onStop() {
        mRequestsBadge.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up listener
        if (requestsListener != null && requestsQuery != null) {
            requestsQuery.removeEventListener(requestsListener);
        }
    }
}