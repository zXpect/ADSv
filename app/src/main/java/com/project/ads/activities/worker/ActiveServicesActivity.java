package com.project.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.helpers.WorkerNavHelper;
import com.project.ads.models.ServiceRequest;
import com.project.ads.helpers.DisplayNameHelper;
import com.project.ads.models.api.CatalogData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.helpers.RequestsNavBadge;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mis trabajos del trabajador · mockup 52 (docs/mockups/MisTrabajos.dc.html).
 * Pendientes (aceptados, en camino o en progreso) y Realizados (completados),
 * con la misma tarjeta del historial del cliente. Lee requests filtrado por su uid.
 */
public class ActiveServicesActivity extends AppCompatActivity {

    private RequestsNavBadge mRequestsBadge;

    private static final String TAG = "ActiveServicesActivity";
    private static final Locale LOCALE = new Locale("es", "CO");

    private RecyclerView mRecyclerView;
    private View mProgressBar;   // tarjetas de relleno (AdsSkeletonView)
    private LinearLayout mEmptyState;
    private SwipeRefreshLayout mSwipeRefresh;

    private String mWorkerId;
    private final List<ServiceRequest> mServices = new ArrayList<>();
    private JobsAdapter mAdapter;
    private CatalogData mCatalog;

    private Query mServicesQuery;
    private ValueEventListener mServicesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_services);

        mWorkerId = getIntent().getStringExtra("worker_id");
        if (mWorkerId == null || mWorkerId.isEmpty()) {
            Toast.makeText(this, "Error: ID de trabajador no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mRecyclerView = findViewById(R.id.recycler_active_services);
        mProgressBar = findViewById(R.id.progress_bar);
        mEmptyState = findViewById(R.id.empty_state);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        mSwipeRefresh.setColorSchemeResources(R.color.ads_brand);
        mSwipeRefresh.setOnRefreshListener(this::loadServices);

        mAdapter = new JobsAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        WorkerNavHelper.setup(this, R.id.nav_worker_jobs);
        mRequestsBadge = RequestsNavBadge.forWorker(this);
        loadCatalogForTitles();
        loadServices();
    }

    /** Nombres de las tareas para los títulos de las tarjetas; sin catálogo se usa el oficio. */
    private void loadCatalogForTitles() {
        mCatalog = CatalogProvider.cachedCatalog();
        if (mCatalog != null) return;
        new CatalogProvider().getCatalog(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                mCatalog = catalog;
                if (!isFinishing() && mAdapter != null) mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
            }

            @Override
            public void onError(String message, int httpStatus) {
                // Sin catálogo los títulos quedan con el nombre del oficio
            }
        });
    }

    private void loadServices() {
        mProgressBar.setVisibility(View.VISIBLE);
        if (mServicesListener != null && mServicesQuery != null) {
            mServicesQuery.removeEventListener(mServicesListener);
        }
        // Las reglas de Firebase solo permiten leer requests filtrado por el propio uid
        mServicesQuery = FirebaseDatabase.getInstance().getReference().child("requests")
                .orderByChild("worker_id").equalTo(mWorkerId);
        mServicesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ServiceRequest> services = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                        if (data == null) continue;
                        ServiceRequest request = ServiceRequest.fromMap(data);
                        request.setRequest_id(snapshot.getKey());
                        if (isPending(request) || "completed".equals(request.getStatus())) services.add(request);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando solicitud: " + snapshot.getKey(), e);
                    }
                }
                // Pendientes primero; dentro de cada grupo, la más reciente primero
                Collections.sort(services, (a, b) -> {
                    int byGroup = Boolean.compare(isPending(b), isPending(a));
                    return byGroup != 0 ? byGroup : Long.compare(b.getTimestamp(), a.getTimestamp());
                });
                mServices.clear();
                mServices.addAll(services);
                mAdapter.notifyDataSetChanged();
                mProgressBar.setVisibility(View.GONE);
                mSwipeRefresh.setRefreshing(false);
                mEmptyState.setVisibility(services.isEmpty() ? View.VISIBLE : View.GONE);
                mRecyclerView.setVisibility(services.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error de base de datos: " + error.getMessage());
                mProgressBar.setVisibility(View.GONE);
                mSwipeRefresh.setRefreshing(false);
                Toast.makeText(ActiveServicesActivity.this, "Error al cargar servicios", Toast.LENGTH_SHORT).show();
            }
        };
        mServicesQuery.addValueEventListener(mServicesListener);
    }

    /** Servicio en curso: aceptado (con o sin cotización aceptada), en camino o en progreso. */
    private static boolean isPending(ServiceRequest request) {
        String status = request.getStatus();
        return "accepted".equals(status) || "on_the_way".equals(status) || "in_progress".equals(status);
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
        if (mServicesListener != null && mServicesQuery != null) {
            mServicesQuery.removeEventListener(mServicesListener);
        }
    }

    // ---------- Adapter (tarjeta del historial, item_my_request) ----------

    private class JobsAdapter extends RecyclerView.Adapter<JobsAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_request, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(mServices.get(position), position);
        }

        @Override
        public int getItemCount() {
            return mServices.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final TextView section, title, status, meta, detail;

            Holder(View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.card_request);
                section = itemView.findViewById(R.id.tv_section);
                title = itemView.findViewById(R.id.tv_service_type);
                status = itemView.findViewById(R.id.tv_status);
                meta = itemView.findViewById(R.id.tv_worker_name);
                detail = itemView.findViewById(R.id.tv_timestamp);
            }

            void bind(ServiceRequest request, int position) {
                boolean pending = isPending(request);
                boolean firstOfGroup = position == 0 || isPending(mServices.get(position - 1)) != pending;
                section.setVisibility(firstOfGroup ? View.VISIBLE : View.GONE);
                section.setText(pending ? R.string.jobs_section_pending : R.string.jobs_section_done);

                // La tarea, como el mockup 52; sin tareas o catálogo, el oficio
                String task = request.getTaskTitle(mCatalog);
                title.setText(task != null ? task : request.getService_type() != null
                        ? request.getService_type() : getString(R.string.history_service_default));

                String client = request.getClient_name() != null ? DisplayNameHelper.format(request.getClient_name()) : getString(R.string.inbox_client_default);
                meta.setText(client + " · " + formatDate(request, pending));

                // Chip: naranja en curso, navy completado
                int label;
                switch (request.getStatus()) {
                    case "on_the_way": label = R.string.request_status_on_the_way; break;
                    case "in_progress": label = R.string.request_status_in_progress; break;
                    case "completed": label = R.string.jobs_status_completed; break;
                    default: label = request.isQuoteAccepted()
                            ? R.string.request_status_accepted : R.string.request_status_quoted;
                }
                status.setText(label);
                boolean done = !pending;
                ViewCompat.setBackgroundTintList(status, ColorStateList.valueOf(ContextCompat.getColor(
                        ActiveServicesActivity.this, done ? R.color.ads_chip_strong_bg : R.color.ads_chip_bg)));
                status.setTextColor(ContextCompat.getColor(ActiveServicesActivity.this,
                        done ? R.color.ads_chip_strong_text : R.color.ads_chip_text));

                // Precio (pendientes) o calificación (realizados)
                detail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                if (pending) {
                    detail.setVisibility(request.hasQuote() ? View.VISIBLE : View.GONE);
                    androidx.core.widget.TextViewCompat.setTextAppearance(detail, R.style.TextAppearance_ADS_Price);
                    detail.setText(request.getFormattedQuotedPrice());
                } else if (request.getRating() > 0) {
                    detail.setVisibility(View.VISIBLE);
                    androidx.core.widget.TextViewCompat.setTextAppearance(detail, R.style.TextAppearance_ADS_BodySmall_Strong);
                    detail.setText(String.format(LOCALE, "%.1f", request.getRating()));
                    detail.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_filled, 0, 0, 0);
                } else {
                    detail.setVisibility(View.VISIBLE);
                    androidx.core.widget.TextViewCompat.setTextAppearance(detail, R.style.TextAppearance_ADS_Caption);
                    detail.setText(R.string.jobs_not_rated);
                }

                card.setStrokeColor(ContextCompat.getColor(ActiveServicesActivity.this,
                        pending ? R.color.ads_brand_text : R.color.ads_line));
                card.setStrokeWidth(getResources().getDimensionPixelSize(
                        pending ? R.dimen.stroke_focus : R.dimen.stroke_control));

                card.setOnClickListener(v -> {
                    Intent intent = new Intent(ActiveServicesActivity.this, ServiceDetailWorkerActivity.class);
                    intent.putExtra("request_id", request.getRequest_id());
                    startActivity(intent);
                });
            }

            /** "Hoy, 3:00 p. m." o "Lun 19 oct, 3:00 p. m." si hay agenda; "14 oct" en realizados. */
            private String formatDate(ServiceRequest request, boolean pending) {
                if (pending && request.getScheduledAt() > 0) {
                    String text = new SimpleDateFormat("EEE d MMM, h:mm a", LOCALE).format(new Date(request.getScheduledAt()));
                    return text.substring(0, 1).toUpperCase(LOCALE) + text.substring(1);
                }
                long time = request.getCompleted_at() > 0 ? request.getCompleted_at() : request.getTimestamp();
                return time > 0 ? new SimpleDateFormat("d MMM", LOCALE).format(new Date(time)) : request.getTimeAgo();
            }
        }
    }
}
