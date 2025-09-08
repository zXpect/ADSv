package com.ads.activities.worker;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ads.adapters.PendingRequestsAdapter;
import com.ads.models.ServiceRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PendingRequestsActivity extends AppCompatActivity implements PendingRequestsAdapter.OnRequestClickListener {

    private static final String TAG = "PendingRequestsActivity";

    // UI Components
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout tvEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private ImageButton btnBack;
    private TextView tvTitle;

    // Data
    private String workerId;
    private String workerType;
    private List<ServiceRequest> pendingRequests;
    private PendingRequestsAdapter adapter;

    // Firebase
    private DatabaseReference databaseReference;
    private ValueEventListener requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        // Get data from intent
        workerId = getIntent().getStringExtra("worker_id");
        workerType = getIntent().getStringExtra("worker_type");

        Log.d(TAG, "Worker ID: " + workerId + ", Worker Type: " + workerType);

        if (workerId == null || workerId.isEmpty()) {
            Toast.makeText(this, "Error: ID de trabajador no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        loadPendingRequests();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewRequests);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);

        btnBack.setOnClickListener(v -> finish());

        swipeRefresh.setOnRefreshListener(() -> {
            loadPendingRequests();
        });

        // Update title
        tvTitle.setText("Solicitudes Pendientes");

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
        if (requestsListener != null) {
            databaseReference.child("requests").removeEventListener(requestsListener);
        }

        // MÉTODO ALTERNATIVO: Cargar todas las solicitudes y filtrar localmente
        requestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ServiceRequest> requests = new ArrayList<>();

                Log.d(TAG, "DataSnapshot exists: " + dataSnapshot.exists());
                Log.d(TAG, "Children count: " + dataSnapshot.getChildrenCount());
                Log.d(TAG, "Current worker ID: " + workerId);
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

        // CAMBIO: Cargar todas las solicitudes en lugar de filtrar en la consulta
        databaseReference.child("requests")
                .addValueEventListener(requestsListener);
    }

    private boolean isRequestRelevant(ServiceRequest request) {
        if (request == null) {
            Log.d(TAG, "Request is null");
            return false;
        }

        String status = request.getStatus();
        Log.d(TAG, "Checking request relevance - Status: " + status);

        // Verificar que el status sea "pending"
        if (status == null || !"pending".equals(status.toLowerCase())) {
            Log.d(TAG, "Request status is not pending: " + status);
            return false;
        }

        String requestWorkerId = request.getWorker_id();
        String requestServiceType = request.getService_type();

        Log.d(TAG, "Request worker ID: " + requestWorkerId +
                ", Request service type: " + requestServiceType +
                ", My worker ID: " + workerId +
                ", My worker type: " + workerType);

        // SOLO mostrar solicitudes específicamente asignadas a este trabajador
        if (requestWorkerId != null && workerId != null && workerId.equals(requestWorkerId)) {
            Log.d(TAG, "Request assigned to this worker");
            return true;
        }

        Log.d(TAG, "Request not assigned to this worker");
        return false;
    }

    private void updateUI(List<ServiceRequest> requests) {
        runOnUiThread(() -> {
            pendingRequests.clear();
            pendingRequests.addAll(requests);
            adapter.notifyDataSetChanged();

            showLoading(false);

            // Update title with count
            updateTitle(requests.size());

            if (requests.isEmpty()) {
                showEmpty(true);
                Log.d(TAG, "No requests to show - showing empty state");
            } else {
                showEmpty(false);
                Log.d(TAG, "Showing " + requests.size() + " requests");
            }

            Log.d(TAG, "UI updated with " + requests.size() + " pending requests");
        });
    }

    private void updateTitle(int requestCount) {
        if (tvTitle != null) {
            if (requestCount > 0) {
                tvTitle.setText("Solicitudes Pendientes (" + requestCount + ")");
            } else {
                tvTitle.setText("Solicitudes Pendientes");
            }
        }
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestClick(ServiceRequest request) {
        if (request != null && request.getRequest_id() != null) {
            Log.d(TAG, "Opening request detail for: " + request.getRequest_id());
            Intent intent = new Intent(this, RequestDetailActivity.class);
            intent.putExtra("request_id", request.getRequest_id());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Error: Datos de solicitud no válidos", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up listener
        if (requestsListener != null && databaseReference != null) {
            databaseReference.child("requests").removeEventListener(requestsListener);
        }
    }
}