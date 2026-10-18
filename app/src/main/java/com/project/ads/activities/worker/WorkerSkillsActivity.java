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
 * Hasta 3 oficios del catálogo (D16) y una descripción opcional; reenvía los datos del
 * paso 1 a SecondRegisterWorkerActivity. Las tareas por trabajador del mockup no se
 * piden: no existe ese dato en el modelo ni en el backend.
 */
public class WorkerSkillsActivity extends AppCompatActivity {

    // Oficios máximos por trabajador (D16; worker_max_categories en el backend)
    private static final int MAX_CATEGORIES = 3;

    private final ArrayList<String> mSelectedCategories = new ArrayList<>();
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
                }
                updateCounter();
            });
            mChipGroup.addView(chip);
        }
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
        String description = mDescription.getText() != null ? mDescription.getText().toString().trim() : "";
        intent.putExtra("description", description);
        startActivity(intent);
    }
}
