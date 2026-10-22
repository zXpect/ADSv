package com.project.ads.activities.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.helpers.SearchTimer;
import com.project.ads.helpers.CategoryHelper;
import com.project.ads.helpers.ClientNavHelper;
import com.project.ads.helpers.NotificationPermissionHelper;
import com.project.ads.helpers.NotificationRouter;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.CatalogData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.providers.ClientProvider;
import com.project.ads.providers.TokenProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Inicio del cliente · mockup 06 (docs/mockups/InicioCliente.dc.html).
 * Saludo, buscador, acceso al mapa, oficios del catálogo y servicio activo.
 * Tocar un oficio abre TasksActivity (mockup 07) y de ahí WorkersActivity
 * (mockup 08), que hace la búsqueda de técnicos cercanos que antes vivía aquí.
 */
public class HomeUserActivity extends AppCompatActivity {
    private static final String TAG = "HomeUserActivity";

    private TextView mUserGreetingText;
    private EditText mSearchBar;
    private View mActiveServiceCard;
    private TextView mActiveServiceLabel;
    private TextView mActiveServiceStatus;

    private AuthProvider mAuthProvider;
    private ClientProvider mClientProvider;
    private FirebaseCrashlytics crashlytics;
    private FirebaseAnalytics mFirebaseAnalytics;

    private Query mActiveQuery;
    private ValueEventListener mActiveListener;
    private final List<CatalogData.Category> mCategories = new ArrayList<>();
    private CategoriesAdapter mCategoriesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_user);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        crashlytics = FirebaseCrashlytics.getInstance();
        mAuthProvider = new AuthProvider();
        mClientProvider = new ClientProvider();

        setupViews();
        loadUserName();
        loadCategories();
        listenActiveService();
        ClientNavHelper.setup(this, R.id.nav_client_home);
        NotificationPermissionHelper.askOnce(this);
        NotificationRouter.openPendingIfAny(this);

        if (mAuthProvider.getId() != null) {
            String userId = mAuthProvider.getId();
            crashlytics.setUserId(userId);
            crashlytics.setCustomKey("user_type", "client");

            // Sincroniza el token FCM para que el backend pueda notificar al cliente
            new TokenProvider().create(userId);

            Bundle params = new Bundle();
            params.putString("user_id", userId);
            params.putString("login_method", "email");
            mFirebaseAnalytics.logEvent("user_login", params);
        }
    }

    private void setupViews() {
        mUserGreetingText = findViewById(R.id.user_greeting_text);
        mSearchBar = findViewById(R.id.search_bar);
        mActiveServiceCard = findViewById(R.id.card_active_service);
        mActiveServiceLabel = findViewById(R.id.tv_active_service_label);
        mActiveServiceStatus = findViewById(R.id.tv_active_service_status);

        findViewById(R.id.vermapa).setOnClickListener(v -> {
            SearchTimer.start(this);
            startActivity(new Intent(this, MapClientActivity.class));
        });

        // Buscar: abre la lista de técnicos filtrada por nombre u oficio
        mSearchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            String query = mSearchBar.getText() != null ? mSearchBar.getText().toString().trim() : "";
            if (query.isEmpty()) return true;
            SearchTimer.start(this);
            Intent intent = new Intent(this, WorkersActivity.class);
            intent.putExtra(WorkersActivity.EXTRA_QUERY, query);
            startActivity(intent);
            return true;
        });

        RecyclerView categories = findViewById(R.id.recycler_home_categories);
        categories.setLayoutManager(new GridLayoutManager(this, 2));
        mCategoriesAdapter = new CategoriesAdapter();
        categories.setAdapter(mCategoriesAdapter);
    }

    private void loadUserName() {
        String userId = mAuthProvider.getId();
        if (userId == null) return;
        mClientProvider.mDataBase.child(userId).get()
                .addOnSuccessListener(snapshot -> {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null && !name.trim().isEmpty()) {
                        mUserGreetingText.setText(getString(R.string.home_greeting, name.trim().split(" ")[0]));
                    }
                })
                .addOnFailureListener(e -> crashlytics.recordException(e));
    }

    /** Oficios del catálogo del backend; si no responde, la lista local. */
    private void loadCategories() {
        new CatalogProvider().getCategoriesWithFallback(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                if (isFinishing()) return;
                mCategories.clear();
                mCategories.addAll(catalog.getCategories());
                mCategoriesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message, int httpStatus) {
                // getCategoriesWithFallback no falla
            }
        });
    }

    /** Oficio → Problemas del oficio (mockup 07) → Técnicos (mockup 08). */
    private void openTasks(CatalogData.Category category) {
        SearchTimer.start(this);
        Intent intent = new Intent(this, TasksActivity.class);
        intent.putExtra(WorkersActivity.EXTRA_CATEGORY, category.getSlug());
        intent.putExtra(WorkersActivity.EXTRA_CATEGORY_NAME, category.getName());
        startActivity(intent);
    }

    /** Tarjeta navy con la solicitud en curso más reciente del cliente (mockup 06). */
    private void listenActiveService() {
        String clientId = mAuthProvider.getId();
        if (clientId == null) return;
        // Las reglas de Firebase solo permiten leer requests filtrado por el propio uid
        mActiveQuery = FirebaseDatabase.getInstance().getReference().child("requests")
                .orderByChild("client_id").equalTo(clientId);
        mActiveListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ServiceRequest latest = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (data == null) continue;
                        ServiceRequest request = ServiceRequest.fromMap(data);
                        request.setRequest_id(child.getKey());
                        if (isActive(request) && (latest == null || request.getTimestamp() > latest.getTimestamp())) {
                            latest = request;
                        }
                    } catch (Exception e) {
                        crashlytics.recordException(e);
                    }
                }
                showActiveService(latest);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                crashlytics.log(TAG + " solicitudes activas: " + error.getMessage());
                showActiveService(null);
            }
        };
        mActiveQuery.addValueEventListener(mActiveListener);
    }

    private static boolean isActive(ServiceRequest request) {
        String status = request.getStatus();
        return status != null && !status.equals("completed")
                && !status.equals("cancelled") && !status.equals("rejected");
    }

    private void showActiveService(ServiceRequest request) {
        if (request == null) {
            mActiveServiceCard.setVisibility(View.GONE);
            return;
        }
        String service = request.getService_type() != null
                ? request.getService_type() : getString(R.string.history_service_default);
        mActiveServiceLabel.setText(getString(R.string.home_active_service, service));
        mActiveServiceStatus.setText(activeStatusText(request));
        mActiveServiceCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ServiceRequestDetailActivity.class);
            intent.putExtra("request_id", request.getRequest_id());
            startActivity(intent);
        });
        mActiveServiceCard.setVisibility(View.VISIBLE);
    }

    private int activeStatusText(ServiceRequest request) {
        switch (request.getStatus()) {
            case "pending":
                return "quoted".equals(request.getQuote_status())
                        ? R.string.home_active_quoted : R.string.home_active_pending;
            case "on_the_way":
                return R.string.home_active_on_the_way;
            case "in_progress":
                return R.string.home_active_in_progress;
            default:
                return R.string.home_active_accepted;
        }
    }

    /** Coordenada del cliente para la solicitud (B14); el backend la guarda en requests/<id>. */
    static void putClientLocation(Intent intent, LatLng location) {
        if (location != null) {
            intent.putExtra(ServiceRequestActivity.EXTRA_LAT, location.latitude);
            intent.putExtra(ServiceRequestActivity.EXTRA_LNG, location.longitude);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Salir de la aplicación")
                .setMessage("¿Deseas salir?")
                .setPositiveButton("Salir", (dialog, which) -> finish())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mActiveQuery != null && mActiveListener != null) {
            mActiveQuery.removeEventListener(mActiveListener);
        }
    }

    // ---------- Oficios ----------

    private class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_category, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CatalogData.Category category = mCategories.get(position);
            holder.name.setText(category.getName());
            holder.icon.setImageResource(CategoryHelper.iconFor(category.getSlug()));
            holder.itemView.setOnClickListener(v -> openTasks(category));
        }

        @Override
        public int getItemCount() {
            return mCategories.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final ImageView icon;

            Holder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_home_category);
                icon = itemView.findViewById(R.id.iv_home_category);
            }
        }
    }
}
