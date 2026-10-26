package com.project.ads.activities.worker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.project.ads.R;
import com.project.ads.helpers.RegisterStepsHelper;
import com.project.ads.models.api.CatalogData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro de trabajador, paso 2 de 3 · mockup 32 (docs/mockups/OficiosTrabajador.dc.html).
 * Hasta 3 oficios del catálogo (D16), las tareas que resuelve de esos oficios (tasks,
 * opcionales) y una descripción opcional; reenvía los datos del paso 1 a
 * SecondRegisterWorkerActivity.
 */
public class WorkerSkillsActivity extends AppCompatActivity {

    // Oficios máximos por trabajador (D16; worker_max_categories en el backend)
    private static final int MAX_CATEGORIES = 3;

    private final ArrayList<String> mSelectedCategories = new ArrayList<>();
    // Tareas elegidas, en el orden del catálogo al guardar
    private final java.util.Set<String> mSelectedTasks = new java.util.HashSet<>();
    private List<CatalogData.Category> mCategories = new ArrayList<>();
    private android.widget.LinearLayout mTaskGroups;
    private ChipGroup mChipGroup;
    private TextView mCounter;
    private TextInputEditText mDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_skills);

        RegisterStepsHelper.bind(this, 2, R.string.register_step2);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        mChipGroup = findViewById(R.id.chip_group_categories);
        mCounter = findViewById(R.id.tv_skills_counter);
        mDescription = findViewById(R.id.et_skills_description);
        mTaskGroups = findViewById(R.id.layout_skills_tasks_groups);
        findViewById(R.id.btn_skills_continue).setOnClickListener(v -> next());

        // Mientras llega el catálogo se muestran los oficios locales
        showCategories(CatalogProvider.localCatalog().getCategories());
        new CatalogProvider().getCategoriesWithFallback(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                if (!isFinishing()) showCategories(catalog.getCategories());
            }

            @Override
            public void onError(String message, int httpStatus) {
                // getCategoriesWithFallback no falla
            }
        });
        updateCounter();
    }

    private void showCategories(List<CatalogData.Category> categories) {
        mCategories = categories;
        mChipGroup.removeAllViews();
        for (CatalogData.Category category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category.getName());
            chip.setCheckable(true);
            chip.setChecked(mSelectedCategories.contains(category.getSlug()));
            chip.setOnClickListener(v -> {
                String slug = category.getSlug();
                if (chip.isChecked()) {
                    if (mSelectedCategories.size() >= MAX_CATEGORIES) {
                        chip.setChecked(false);
                        Toast.makeText(this, getString(R.string.worker_categories_max, MAX_CATEGORIES),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!mSelectedCategories.contains(slug)) mSelectedCategories.add(slug);
                } else {
                    mSelectedCategories.remove(slug);
                    // Sus tareas dejan de aplicar
                    for (CatalogData.Task task : category.getTasks()) mSelectedTasks.remove(task.getSlug());
                }
                updateCounter();
                showTasks();
            });
            mChipGroup.addView(chip);
        }
        showTasks();
    }

    /** Un grupo por oficio elegido, con sus tareas del catálogo (mockup 32). */
    private void showTasks() {
        mTaskGroups.removeAllViews();
        android.view.LayoutInflater inflater = getLayoutInflater();
        boolean any = false;
        for (String slug : mSelectedCategories) {
            CatalogData.Category category = findCategory(slug);
            if (category == null || category.getTasks().isEmpty()) continue;
            any = true;

            TextView title = new TextView(this);
            title.setText(category.getName());
            title.setTextAppearance(R.style.TextAppearance_ADS_Label);
            title.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ads_brand_text));
            title.setPadding(0, getResources().getDimensionPixelSize(R.dimen.space_3), 0, 0);
            mTaskGroups.addView(title);

            ChipGroup group = new ChipGroup(this);
            group.setChipSpacingHorizontal(getResources().getDimensionPixelSize(R.dimen.space_2));
            group.setChipSpacingVertical(0);
            for (CatalogData.Task task : category.getTasks()) {
                Chip chip = (Chip) inflater.inflate(R.layout.view_skill_task_chip, group, false);
                chip.setText(task.getName());
                chip.setChecked(mSelectedTasks.contains(task.getSlug()));
                chip.setOnCheckedChangeListener((button, checked) -> {
                    if (checked) mSelectedTasks.add(task.getSlug());
                    else mSelectedTasks.remove(task.getSlug());
                });
                group.addView(chip);
            }
            mTaskGroups.addView(group);
        }
        findViewById(R.id.layout_skills_tasks).setVisibility(any ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private CatalogData.Category findCategory(String slug) {
        for (CatalogData.Category category : mCategories) {
            if (category.getSlug().equals(slug)) return category;
        }
        return null;
    }

    /** Tareas elegidas de los oficios elegidos, en el orden del catálogo. */
    private ArrayList<String> selectedTasks() {
        ArrayList<String> tasks = new ArrayList<>();
        for (String slug : mSelectedCategories) {
            CatalogData.Category category = findCategory(slug);
            if (category == null) continue;
            for (CatalogData.Task task : category.getTasks()) {
                if (mSelectedTasks.contains(task.getSlug())) tasks.add(task.getSlug());
            }
        }
        return tasks;
    }

    private void updateCounter() {
        mCounter.setText(getString(R.string.skills_counter, mSelectedCategories.size(), MAX_CATEGORIES));
    }

    private void next() {
        if (mSelectedCategories.isEmpty()) {
            Toast.makeText(this, R.string.worker_categories_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SecondRegisterWorkerActivity.class);
        if (getIntent().getExtras() != null) intent.putExtras(getIntent().getExtras());
        // `work` se sigue escribiendo con el primer oficio como respaldo para versiones viejas de la app
        intent.putExtra("work", CatalogProvider.categoryName(mSelectedCategories.get(0)));
        intent.putStringArrayListExtra("categories", mSelectedCategories);
        intent.putStringArrayListExtra("tasks", selectedTasks());
        String description = mDescription.getText() != null ? mDescription.getText().toString().trim() : "";
        intent.putExtra("description", description);
        startActivity(intent);
    }
}
