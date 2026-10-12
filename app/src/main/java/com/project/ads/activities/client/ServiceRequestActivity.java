package com.project.ads.activities.client;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.project.ads.R;
import com.project.ads.models.RequestTaskItem;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.Worker;
import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.CatalogData;
import com.project.ads.models.api.QuoteReference;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.providers.WorkerProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Primer paso de la solicitud: el cliente elige el oficio (si el trabajador
 * tiene varios), agrega tareas del catálogo con su cantidad o «Otro», y ve el
 * estimado que calcula el backend (POST /api/requests/estimate/). La solicitud
 * se envía en VerifyRequestActivity (POST /api/requests/).
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

    private TextInputEditText addressInput;
    private TextInputEditText descriptionInput;
    private TextView serviceTypeText;
    private TextView noTasksText;
    private TextView estimateText;
    private TextView estimateNoticeText;
    private LinearLayout taskItemsLayout;
    private MaterialButton addTaskButton;
    private MaterialButton sendRequestButton;
    private ProgressDialog progressDialog;

    private String mWorkerId;
    private String mWorkerName;
    private Double mLat;
    private Double mLng;

    private CatalogData mCatalog;
    private final List<CatalogData.Category> mWorkerCategories = new ArrayList<>();
    private CatalogData.Category mCategory;
    private final List<RequestTaskItem> mItems = new ArrayList<>();
    private QuoteReference mEstimate;
    private int mEstimateRequestSeq = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request);
        setupStatusBar();
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

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void initViews() {
        addressInput = findViewById(R.id.et_address);
        descriptionInput = findViewById(R.id.et_description);
        serviceTypeText = findViewById(R.id.tv_service_type);
        noTasksText = findViewById(R.id.tv_no_tasks);
        estimateText = findViewById(R.id.tv_estimate);
        estimateNoticeText = findViewById(R.id.tv_estimate_notice);
        taskItemsLayout = findViewById(R.id.layout_task_items);
        addTaskButton = findViewById(R.id.btn_add_task);
        sendRequestButton = findViewById(R.id.btn_send_request);

        addTaskButton.setEnabled(false);
        addTaskButton.setOnClickListener(v -> showAddTaskDialog());
        serviceTypeText.setOnClickListener(v -> showCategoryChooser());
        sendRequestButton.setOnClickListener(v -> {
            if (validateInputs()) navigateToVerifyRequest();
        });
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
                mCatalog = catalog;
                for (String slug : worker.getCategorySlugs()) {
                    CatalogData.Category category = catalog.findCategory(slug);
                    if (category != null) mWorkerCategories.add(category);
                }
                if (mWorkerCategories.isEmpty()) {
                    serviceTypeText.setText(R.string.request_worker_without_category);
                    return;
                }
                selectCategory(mWorkerCategories.get(0));
            }

            @Override
            public void onError(String message, int httpStatus) {
                hideProgressDialog();
                // Sin catálogo no se pueden elegir tareas ni calcular el estimado
                new AlertDialog.Builder(ServiceRequestActivity.this)
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
        addTaskButton.setEnabled(true);
        mItems.clear();
        renderItems();
    }

    private void showCategoryChooser() {
        if (mWorkerCategories.size() < 2) return;
        String[] names = new String[mWorkerCategories.size()];
        for (int i = 0; i < names.length; i++) names[i] = mWorkerCategories.get(i).getName();
        int checked = mWorkerCategories.indexOf(mCategory);

        new AlertDialog.Builder(this)
                .setTitle(R.string.request_choose_category)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    dialog.dismiss();
                    CatalogData.Category chosen = mWorkerCategories.get(which);
                    if (chosen == mCategory) return;
                    if (mItems.isEmpty()) {
                        selectCategory(chosen);
                    } else {
                        // Las tareas son del oficio: al cambiarlo se quitan
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.request_category_change_confirm)
                                .setPositiveButton(R.string.accept, (d, w) -> selectCategory(chosen))
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                })
                .show();
    }

    // ---------- tareas ----------

    private void showAddTaskDialog() {
        if (mCategory == null) return;
        if (mItems.size() >= MAX_ITEMS) {
            Toast.makeText(this, getString(R.string.request_max_items, MAX_ITEMS), Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        Spinner taskSpinner = view.findViewById(R.id.sp_task);
        TextView rangeText = view.findViewById(R.id.tv_task_range);
        TextInputLayout descriptionLayout = view.findViewById(R.id.til_custom_description);
        TextInputEditText descriptionEdit = view.findViewById(R.id.et_custom_description);
        TextInputLayout quantityLayout = view.findViewById(R.id.til_quantity);
        TextInputEditText quantityEdit = view.findViewById(R.id.et_quantity);

        // Opciones: tareas del oficio y al final «Otro»
        List<String> options = new ArrayList<>();
        List<CatalogData.Task> tasks = mCategory.getTasks();
        for (CatalogData.Task task : tasks) options.add(task.getName());
        options.add(getString(R.string.request_other_task));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskSpinner.setAdapter(adapter);

        taskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                CatalogData.Task task = position < tasks.size() ? tasks.get(position) : null;
                descriptionLayout.setVisibility(task == null ? View.VISIBLE : View.GONE);
                boolean decimals = task != null && task.allowsDecimals();
                quantityEdit.setInputType(decimals
                        ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        : InputType.TYPE_CLASS_NUMBER);
                quantityLayout.setHint(task == null ? getString(R.string.request_quantity_hint)
                        : getString(R.string.request_quantity_unit_hint, CatalogData.unitLabel(task.getUnit())));
                if (task == null) {
                    rangeText.setText(R.string.request_other_task_note);
                } else if (task.hasRange()) {
                    rangeText.setText(getString(R.string.request_task_range,
                            ServiceRequest.formatCop(task.getMinCop()), ServiceRequest.formatCop(task.getMaxCop()),
                            CatalogData.unitLabel(task.getUnit())));
                } else {
                    rangeText.setText(R.string.request_task_without_range);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.request_add_task)
                .setView(view)
                .setPositiveButton(R.string.request_add, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int position = taskSpinner.getSelectedItemPosition();
            CatalogData.Task task = position < tasks.size() ? tasks.get(position) : null;
            descriptionLayout.setError(null);
            quantityLayout.setError(null);

            Double quantity = parseQuantity(quantityEdit.getText() != null ? quantityEdit.getText().toString() : "");
            boolean decimals = task != null && task.allowsDecimals();
            if (quantity == null || quantity <= 0 || quantity > MAX_QUANTITY
                    || (!decimals && quantity != Math.floor(quantity))) {
                quantityLayout.setError(getString(decimals ? R.string.request_quantity_error_decimal
                        : R.string.request_quantity_error_integer, (int) MAX_QUANTITY));
                return;
            }

            if (task == null) {
                String description = descriptionEdit.getText() != null ? descriptionEdit.getText().toString().trim() : "";
                if (description.isEmpty()) {
                    descriptionLayout.setError(getString(R.string.request_custom_task_required));
                    return;
                }
                mItems.add(RequestTaskItem.custom(description, quantity));
            } else {
                mItems.add(RequestTaskItem.fromTask(task, quantity));
            }
            dialog.dismiss();
            renderItems();
        }));
        dialog.show();
    }

    /** Acepta coma o punto decimal y lo deja con dos decimales como máximo. */
    private static Double parseQuantity(String text) {
        String value = text.trim().replace(',', '.');
        if (value.isEmpty()) return null;
        try {
            return Math.round(Double.parseDouble(value) * 100) / 100.0;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void renderItems() {
        taskItemsLayout.removeAllViews();
        noTasksText.setVisibility(mItems.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (RequestTaskItem item : new ArrayList<>(mItems)) {
            View row = inflater.inflate(R.layout.item_request_task, taskItemsLayout, false);
            ((TextView) row.findViewById(R.id.tv_task_item)).setText(item.getDisplayText());
            ImageButton remove = row.findViewById(R.id.btn_remove_task);
            remove.setOnClickListener(v -> {
                mItems.remove(item);
                renderItems();
            });
            taskItemsLayout.addView(row);
        }
        refreshEstimate();
    }

    // ---------- estimado ----------

    private void refreshEstimate() {
        mEstimate = null;
        if (mItems.isEmpty()) {
            estimateText.setVisibility(View.GONE);
            estimateNoticeText.setVisibility(View.GONE);
            return;
        }
        estimateText.setVisibility(View.VISIBLE);
        estimateText.setText(R.string.request_estimate_loading);
        estimateNoticeText.setVisibility(View.VISIBLE);

        // Solo cuenta la respuesta de la última petición
        int seq = ++mEstimateRequestSeq;
        List<ApiBodies.TaskItemBody> bodies = new ArrayList<>();
        for (RequestTaskItem item : mItems) bodies.add(item.toBody());
        mCatalogProvider.estimate(mCategory.getSlug(), bodies, new ApiCallback<QuoteReference>() {
            @Override
            public void onSuccess(QuoteReference estimate) {
                if (seq != mEstimateRequestSeq) return;
                mEstimate = estimate;
                estimateText.setText(estimateSummary(ServiceRequestActivity.this, estimate));
            }

            @Override
            public void onError(String message, int httpStatus) {
                if (seq != mEstimateRequestSeq) return;
                estimateText.setText(getString(R.string.request_estimate_error, message));
            }
        });
    }

    /** Texto del estimado; lo reutiliza VerifyRequestActivity. */
    static String estimateSummary(android.content.Context context, QuoteReference estimate) {
        if (estimate == null) return context.getString(R.string.request_estimate_unavailable);
        if (!estimate.hasRange()) return context.getString(R.string.request_estimate_unpriced);
        String range = context.getString(R.string.request_estimate_range,
                ServiceRequest.formatCop(estimate.getMinCop()), ServiceRequest.formatCop(estimate.getMaxCop()));
        return estimate.hasUnpricedItems()
                ? range + "\n" + context.getString(R.string.request_estimate_partial)
                : range;
    }

    // ---------- siguiente paso ----------

    private boolean validateInputs() {
        if (addressInput.getText() == null || addressInput.getText().toString().trim().isEmpty()) {
            showToast("Por favor, ingresa la dirección.");
            return false;
        }
        if (mCategory == null) {
            showToast(getString(R.string.request_worker_without_category));
            return false;
        }
        if (mItems.isEmpty()) {
            showToast(getString(R.string.request_tasks_required));
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
        intent.putExtra("worker_photo", R.drawable.workerlogo);
        intent.putExtra("worker_name", mWorkerName);
        intent.putExtra("worker_id", mWorkerId);
        intent.putExtra("address", addressInput.getText().toString().trim());
        intent.putExtra("description", descriptionInput.getText() != null
                ? descriptionInput.getText().toString().trim() : "");
        intent.putExtra("service_category", mCategory.getSlug());
        intent.putExtra("service_type", mCategory.getName());
        intent.putExtra("task_items", new Gson().toJson(mItems.toArray(new RequestTaskItem[0])));
        intent.putExtra("estimate", estimateSummary(this, mEstimate));
        intent.putExtra(EXTRA_LAT, mLat);
        intent.putExtra(EXTRA_LNG, mLng);
        startActivity(intent);
        // No se finaliza para poder volver a editar
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
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
