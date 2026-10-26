package com.project.ads.activities.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.ads.R;
import com.project.ads.helpers.CategoryHelper;
import com.project.ads.models.api.CatalogData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Problemas del oficio · mockup 07 (docs/mockups/Tareas.dc.html).
 * Lista las tareas del oficio según el catálogo del backend y la unidad con la
 * que se cobra. Elegir una (o «Otro problema») abre la lista de técnicos (mockup 08).
 * La tarea elegida solo se muestra como contexto; la solicitud sigue eligiendo
 * sus tareas en su propia pantalla.
 */
public class TasksActivity extends AppCompatActivity {

    private String mCategorySlug;
    private String mCategoryName;
    private final List<CatalogData.Task> mTasks = new ArrayList<>();
    private TasksAdapter mAdapter;
    private ProgressBar mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        mCategorySlug = getIntent().getStringExtra(WorkersActivity.EXTRA_CATEGORY);
        mCategoryName = getIntent().getStringExtra(WorkersActivity.EXTRA_CATEGORY_NAME);

        findViewById(R.id.btn_tasks_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_tasks_category)).setText(mCategoryName);
        ((ImageView) findViewById(R.id.iv_tasks_category)).setImageResource(CategoryHelper.iconFor(mCategorySlug));
        mProgress = findViewById(R.id.progress_tasks);

        RecyclerView recycler = findViewById(R.id.recycler_tasks);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new TasksAdapter();
        recycler.setAdapter(mAdapter);

        loadTasks();
    }

    /** Tareas del oficio; si el catálogo no responde, queda solo «Otro problema». */
    private void loadTasks() {
        mProgress.setVisibility(View.VISIBLE);
        new CatalogProvider().getCatalog(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                if (isFinishing()) return;
                mProgress.setVisibility(View.GONE);
                CatalogData.Category category = mCategorySlug != null ? catalog.findCategory(mCategorySlug) : null;
                mTasks.clear();
                if (category != null && category.getTasks() != null) mTasks.addAll(category.getTasks());
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message, int httpStatus) {
                if (isFinishing()) return;
                mProgress.setVisibility(View.GONE);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    /** Siguiente paso: técnicos del oficio, con la tarea como contexto (null = «Otro problema»). */
    private void openWorkers(CatalogData.Task task) {
        Intent intent = new Intent(this, WorkersActivity.class);
        intent.putExtra(WorkersActivity.EXTRA_CATEGORY, mCategorySlug);
        intent.putExtra(WorkersActivity.EXTRA_CATEGORY_NAME, mCategoryName);
        intent.putExtra(WorkersActivity.EXTRA_TASK_NAME,
                task != null ? task.getName() : getString(R.string.tasks_other_title));
        startActivity(intent);
    }

    private String unitText(CatalogData.Task task) {
        String unit = CatalogData.unitLabel(task.getUnit());
        return unit.isEmpty() ? "" : getString(R.string.tasks_unit, unit);
    }

    // ---------- Adapter: tareas + «Otro problema» al final ----------

    private class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.Holder> {
        private static final int TYPE_TASK = 0;
        private static final int TYPE_OTHER = 1;

        @Override
        public int getItemViewType(int position) {
            return position < mTasks.size() ? TYPE_TASK : TYPE_OTHER;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == TYPE_TASK ? R.layout.item_task : R.layout.item_task_other;
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            if (getItemViewType(position) == TYPE_OTHER) {
                holder.itemView.setOnClickListener(v -> openWorkers(null));
                return;
            }
            CatalogData.Task task = mTasks.get(position);
            holder.name.setText(task.getName());
            holder.unit.setText(unitText(task));
            holder.itemView.setOnClickListener(v -> openWorkers(task));
        }

        @Override
        public int getItemCount() {
            // Mientras carga no se muestra nada; luego las tareas y «Otro problema»
            return mProgress.getVisibility() == View.VISIBLE ? 0 : mTasks.size() + 1;
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView unit;

            Holder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_task_name);
                unit = itemView.findViewById(R.id.tv_task_unit);
            }
        }
    }
}
