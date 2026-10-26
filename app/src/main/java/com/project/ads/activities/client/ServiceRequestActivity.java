package com.project.ads.activities.client;

import android.Manifest;
import com.project.ads.helpers.LoadingDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.project.ads.R;
import com.project.ads.models.RequestTaskItem;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.Worker;
import com.project.ads.models.api.CatalogData;
import com.project.ads.models.api.QuoteReference;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.providers.WorkerProvider;
import com.project.ads.helpers.AdsSnackbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Solicitud, paso 1 de 2 · mockup 10 (docs/mockups/Solicitud.dc.html).
 * El cliente elige el oficio (si el trabajador tiene varios) y marca las tareas
 * del catálogo con su cantidad, o «Otro trabajo» con su descripción. La dirección,
 * el estimado del backend y el envío están en VerifyRequestActivity (paso 2).
 */
public class ServiceRequestActivity extends AppCompatActivity {

    private static final String TAG = "ServiceRequestActivity";

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";

    // Límites del backend (EstimateSerializer / TaskItemSerializer)
    private static final int MAX_ITEMS = 10;
    private static final double MAX_QUANTITY = 1000;

    private final CatalogProvider mCatalogProvider = new CatalogProvider();
    private final WorkerProvider mWorkerProvider = new WorkerProvider();

    private TextView serviceTypeText;
    private TextView noTasksText;
    private TextView selectedCountText;
    private LinearLayout taskItemsLayout;
    private MaterialButton continueButton;
    private LoadingDialog progressDialog;

    private String mWorkerId;
    private String mWorkerName;
    private Double mLat;
    private Double mLng;

    private final List<CatalogData.Category> mWorkerCategories = new ArrayList<>();
    private CatalogData.Category mCategory;
    /** Tareas marcadas (slug → cantidad), en el orden del catálogo. */
    private final Map<String, Double> mSelected = new LinkedHashMap<>();
    private boolean mCustomSelected = false;
    private double mCustomQuantity = 1;
    private EditText mCustomDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request);
        initViews();

        mWorkerId = getIntent().getStringExtra("workerId");
        if (TextUtils.isEmpty(mWorkerId)) {
            Log.e(TAG, "No se recibió ID del trabajador");
            Toast.makeText(this, "Error: No se seleccionó un trabajador", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (getIntent().hasExtra(EXTRA_LAT) && getIntent().hasExtra(EXTRA_LNG)) {
            mLat = getIntent().getDoubleExtra(EXTRA_LAT, 0);
            mLng = getIntent().getDoubleExtra(EXTRA_LNG, 0);
        } else {
            loadLastLocation();
        }

        loadWorkerAndCatalog();
    }

    private void initViews() {
        serviceTypeText = findViewById(R.id.tv_service_type);
        noTasksText = findViewById(R.id.tv_no_tasks);
        selectedCountText = findViewById(R.id.tv_selected_count);
        taskItemsLayout = findViewById(R.id.layout_task_items);
        continueButton = findViewById(R.id.btn_send_request);

        findViewById(R.id.btn_request_back).setOnClickListener(v -> finish());
        serviceTypeText.setOnClickListener(v -> showCategoryChooser());
        continueButton.setOnClickListener(v -> {
            if (validateInputs()) navigateToVerifyRequest();
        });
        updateSelectedCount();
    }

    /** Respaldo si quien abrió la pantalla no pasó la coordenada del cliente. */
    private void loadLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        mLat = location.getLatitude();
                        mLng = location.getLongitude();
                    }
                });
    }

    // ---------- trabajador y catálogo ----------

    private void loadWorkerAndCatalog() {
        showProgressDialog(getString(R.string.loading));

        mWorkerProvider.getWorker(mWorkerId)
                .addOnSuccessListener(snapshot -> {
                    Worker worker = snapshot.exists() ? snapshot.getValue(Worker.class) : null;
                    if (worker == null) {
                        hideProgressDialog();
                        Toast.makeText(this, "No se pudo cargar la información del trabajador", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    String fullName = ((worker.getName() != null ? worker.getName() : "") + " "
                            + (worker.getLastName() != null ? worker.getLastName() : "")).trim();
                    mWorkerName = fullName.isEmpty() ? "Trabajador" : fullName;
                    ((TextView) findViewById(R.id.worker_name)).setText(mWorkerName);
                    ((TextView) findViewById(R.id.tv_worker_initials)).setText(initialsOf(mWorkerName));
                    loadCatalog(worker);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e(TAG, "Error loading worker info", e);
                    Toast.makeText(this, "Error al cargar datos del trabajador", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadCatalog(Worker worker) {
        mCatalogProvider.getCatalog(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                hideProgressDialog();
                for (String slug : worker.getCategorySlugs()) {
                    CatalogData.Category category = catalog.findCategory(slug);
                    if (category != null) mWorkerCategories.add(category);
                }
                if (mWorkerCategories.isEmpty()) {
                    noTasksText.setText(R.string.request_worker_without_category);
                    noTasksText.setVisibility(View.VISIBLE);
                    return;
                }
                selectCategory(mWorkerCategories.get(0));
            }

            @Override
            public void onError(String message, int httpStatus) {
                hideProgressDialog();
                // Sin catálogo no se pueden elegir tareas ni calcular el estimado
                new MaterialAlertDialogBuilder(ServiceRequestActivity.this)
                        .setTitle(R.string.request_catalog_error_title)
                        .setMessage(message)
                        .setPositiveButton(R.string.retry, (d, w) -> {
                            showProgressDialog(getString(R.string.loading));
                            loadCatalog(worker);
                        })
                        .setNegativeButton(R.string.cancel, (d, w) -> finish())
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private void selectCategory(CatalogData.Category category) {
        mCategory = category;
        boolean several = mWorkerCategories.size() > 1;
        serviceTypeText.setText(several
                ? getString(R.string.request_category_change, category.getName())
                : category.getName());
        serviceTypeText.setClickable(several);
        mSelected.clear();
        mCustomSelected = false;
        mCustomQuantity = 1;
        renderTasks();
    }

    private void showCategoryChooser() {
        if (mWorkerCategories.size() < 2) return;
        String[] names = new String[mWorkerCategories.size()];
        for (int i = 0; i < names.length; i++) names[i] = mWorkerCategories.get(i).getName();
        int checked = mWorkerCategories.indexOf(mCategory);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.request_choose_category)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    dialog.dismiss();
                    CatalogData.Category chosen = mWorkerCategories.get(which);
                    if (chosen == mCategory) return;
                    if (selectedCount() == 0) {
                        selectCategory(chosen);
                    } else {
                        // Las tareas son del oficio: al cambiarlo se quitan
                        new MaterialAlertDialogBuilder(this)
                                .setMessage(R.string.request_category_change_confirm)
                                .setPositiveButton(R.string.accept, (d, w) -> selectCategory(chosen))
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                })
                .show();
    }

    // ---------- tareas (mockup 10) ----------

    private int selectedCount() {
        return mSelected.size() + (mCustomSelected ? 1 : 0);
    }

    /** Una tarjeta por tarea del oficio y «Otro trabajo» al final. */
    private void renderTasks() {
        taskItemsLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CatalogData.Task task : mCategory.getTasks()) {
            View card = inflater.inflate(R.layout.item_request_task_option, taskItemsLayout, false);
            bindTaskCard(card, task);
            taskItemsLayout.addView(card);
        }
        View custom = inflater.inflate(R.layout.item_request_task_option, taskItemsLayout, false);
        bindCustomCard(custom);
        taskItemsLayout.addView(custom);
        updateSelectedCount();
    }

    private void bindTaskCard(View card, CatalogData.Task task) {
        ((TextView) card.findViewById(R.id.tv_task_option_name)).setText(task.getName());
        String unit = CatalogData.unitLabel(task.getUnit());
        ((TextView) card.findViewById(R.id.tv_task_option_unit)).setText(capitalize(unit));
        ((TextView) card.findViewById(R.id.tv_task_quantity_label))
                .setText(capitalize(CatalogData.unitLabel(task.getUnit(), 2)));

        Runnable refresh = () -> {
            Double quantity = mSelected.get(task.getSlug());
            setCardSelected(card, quantity != null);
            card.findViewById(R.id.tv_task_option_unit).setVisibility(quantity != null ? View.GONE : View.VISIBLE);
            if (quantity != null) {
                ((TextView) card.findViewById(R.id.tv_task_quantity)).setText(RequestTaskItem.formatQuantity(quantity));
            }
            updateSelectedCount();
        };

        card.setOnClickListener(v -> {
            if (mSelected.containsKey(task.getSlug())) {
                mSelected.remove(task.getSlug());
            } else if (selectedCount() >= MAX_ITEMS) {
                showToast(getString(R.string.request_max_items, MAX_ITEMS));
                return;
            } else {
                mSelected.put(task.getSlug(), 1.0);
            }
            refresh.run();
        });
        card.findViewById(R.id.btn_task_minus).setOnClickListener(v -> {
            Double quantity = mSelected.get(task.getSlug());
            if (quantity == null) return;
            if (quantity <= 1) {
                mSelected.remove(task.getSlug());   // bajar de 1 la desmarca
            } else {
                mSelected.put(task.getSlug(), Math.max(1, quantity - 1));
            }
            refresh.run();
        });
        card.findViewById(R.id.btn_task_plus).setOnClickListener(v -> {
            Double quantity = mSelected.get(task.getSlug());
            if (quantity == null || quantity + 1 > MAX_QUANTITY) return;
            mSelected.put(task.getSlug(), quantity + 1);
            refresh.run();
        });
        // Tocar el número permite escribir la cantidad (p. ej. 12,5 m²)
        card.findViewById(R.id.tv_task_quantity).setOnClickListener(v -> {
            Double quantity = mSelected.get(task.getSlug());
            if (quantity == null) return;
            askQuantity(quantity, task.allowsDecimals(), value -> {
                mSelected.put(task.getSlug(), value);
                refresh.run();
            });
        });
        refresh.run();
    }

    private void bindCustomCard(View card) {
        ((TextView) card.findViewById(R.id.tv_task_option_name)).setText(R.string.request_custom_title);
        ((TextView) card.findViewById(R.id.tv_task_option_unit)).setText(R.string.request_custom_subtitle);
        ((TextView) card.findViewById(R.id.tv_task_quantity_label)).setText(R.string.request_quantity_label);
        mCustomDescription = card.findViewById(R.id.et_task_custom);

        Runnable refresh = () -> {
            setCardSelected(card, mCustomSelected);
            card.findViewById(R.id.tv_task_option_unit).setVisibility(mCustomSelected ? View.GONE : View.VISIBLE);
            card.findViewById(R.id.til_task_custom).setVisibility(mCustomSelected ? View.VISIBLE : View.GONE);
            ((TextView) card.findViewById(R.id.tv_task_quantity)).setText(RequestTaskItem.formatQuantity(mCustomQuantity));
            updateSelectedCount();
        };

        card.setOnClickListener(v -> {
            if (!mCustomSelected && selectedCount() >= MAX_ITEMS) {
                showToast(getString(R.string.request_max_items, MAX_ITEMS));
                return;
            }
            mCustomSelected = !mCustomSelected;
            refresh.run();
            if (mCustomSelected) mCustomDescription.requestFocus();
        });
        card.findViewById(R.id.btn_task_minus).setOnClickListener(v -> {
            if (mCustomQuantity <= 1) {
                mCustomSelected = false;
            } else {
                mCustomQuantity = Math.max(1, mCustomQuantity - 1);
            }
            refresh.run();
        });
        card.findViewById(R.id.btn_task_plus).setOnClickListener(v -> {
            if (mCustomQuantity + 1 > MAX_QUANTITY) return;
            mCustomQuantity += 1;
            refresh.run();
        });
        card.findViewById(R.id.tv_task_quantity).setOnClickListener(v ->
                askQuantity(mCustomQuantity, false, value -> {
                    mCustomQuantity = value;
                    refresh.run();
                }));
        refresh.run();
    }

    /** Borde naranja, casilla marcada y contador visibles cuando la tarea está elegida. */
    private void setCardSelected(View card, boolean selected) {
        MaterialCardView materialCard = (MaterialCardView) card;
        materialCard.setStrokeColor(ContextCompat.getColor(this, selected ? R.color.ads_brand_text : R.color.ads_line));
        materialCard.setStrokeWidth(getResources().getDimensionPixelSize(
                selected ? R.dimen.stroke_focus : R.dimen.stroke_control));
        ImageView check = card.findViewById(R.id.iv_task_check);
        check.setBackgroundResource(selected ? R.drawable.bg_ads_check_on : R.drawable.bg_ads_check_off);
        check.setImageResource(selected ? R.drawable.ic_ads_check : 0);
        card.findViewById(R.id.layout_task_quantity).setVisibility(selected ? View.VISIBLE : View.GONE);
        materialCard.setContentDescription(null);
    }

    private void updateSelectedCount() {
        int count = selectedCount();
        selectedCountText.setText(getResources().getQuantityString(R.plurals.request_selected_count, count, count));
    }

    private interface QuantityListener {
        void onQuantity(double value);
    }

    /** Diálogo para escribir la cantidad; valida los mismos límites que el backend. */
    private void askQuantity(double current, boolean decimals, QuantityListener listener) {
        EditText input = new EditText(this);
        input.setInputType(decimals
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                : InputType.TYPE_CLASS_NUMBER);
        input.setText(RequestTaskItem.formatQuantity(current));
        input.setSelectAllOnFocus(true);
        int padding = getResources().getDimensionPixelSize(R.dimen.screen_margin);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.request_quantity_label)
                .setView(container)
                .setPositiveButton(R.string.accept, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Double quantity = parseQuantity(input.getText().toString());
            if (quantity == null || quantity <= 0 || quantity > MAX_QUANTITY
                    || (!decimals && quantity != Math.floor(quantity))) {
                input.setError(getString(decimals ? R.string.request_quantity_error_decimal
                        : R.string.request_quantity_error_integer, (int) MAX_QUANTITY));
                return;
            }
            listener.onQuantity(quantity);
            dialog.dismiss();
        }));
        dialog.show();
    }

    /**
     * Acepta coma o punto decimal y lo deja con dos decimales como máximo.
     * Con coma, el punto se toma como separador de miles ("1.000,5").
     */
    private static Double parseQuantity(String text) {
        String value = text.trim();
        value = value.contains(",") ? value.replace(".", "").replace(',', '.') : value;
        if (value.isEmpty()) return null;
        try {
            return Math.round(Double.parseDouble(value) * 100) / 100.0;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------- siguiente paso ----------

    /** Tareas elegidas en el orden del catálogo, y «Otro» al final. */
    private List<RequestTaskItem> buildItems() {
        List<RequestTaskItem> items = new ArrayList<>();
        for (CatalogData.Task task : mCategory.getTasks()) {
            Double quantity = mSelected.get(task.getSlug());
            if (quantity != null) items.add(RequestTaskItem.fromTask(task, quantity));
        }
        if (mCustomSelected) {
            String description = mCustomDescription.getText() != null
                    ? mCustomDescription.getText().toString().trim() : "";
            items.add(RequestTaskItem.custom(description, mCustomQuantity));
        }
        return items;
    }

    private boolean validateInputs() {
        if (mCategory == null) {
            showToast(getString(R.string.request_worker_without_category));
            return false;
        }
        if (selectedCount() == 0) {
            showToast(getString(R.string.request_tasks_required));
            return false;
        }
        if (mCustomSelected && (mCustomDescription.getText() == null
                || mCustomDescription.getText().toString().trim().isEmpty())) {
            mCustomDescription.setError(getString(R.string.request_custom_task_required));
            mCustomDescription.requestFocus();
            return false;
        }
        if (mLat == null || mLng == null) {
            showToast(getString(R.string.request_location_required));
            loadLastLocation();
            return false;
        }
        return true;
    }

    private void navigateToVerifyRequest() {
        Intent intent = new Intent(this, VerifyRequestActivity.class);
        intent.putExtra("worker_name", mWorkerName);
        intent.putExtra("worker_id", mWorkerId);
        intent.putExtra("service_category", mCategory.getSlug());
        intent.putExtra("service_type", mCategory.getName());
        intent.putExtra("task_items", new Gson().toJson(buildItems().toArray(new RequestTaskItem[0])));
        intent.putExtra(EXTRA_LAT, mLat);
        intent.putExtra(EXTRA_LNG, mLng);
        startActivity(intent);
        // No se finaliza para poder volver a editar
    }

    /** Texto del estimado total ("$55.500 – $110.000"); null si no hay rango. */
    static String estimateRange(QuoteReference estimate) {
        if (estimate == null || !estimate.hasRange()) return null;
        return ServiceRequest.formatCop(estimate.getMinCop()) + " – " + ServiceRequest.formatCop(estimate.getMaxCop());
    }

    private static String initialsOf(String name) {
        StringBuilder initials = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) initials.append(Character.toUpperCase(part.charAt(0)));
        }
        return initials.toString();
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase(new Locale("es", "CO")) + text.substring(1);
    }

    private void showToast(String message) {
        AdsSnackbar.show(this, message);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new LoadingDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
