package com.project.ads.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.project.ads.helpers.Motion;
import com.project.ads.R;
import com.project.ads.adapters.PqrAdapter;
import com.project.ads.helpers.ClientNavHelper;
import com.project.ads.helpers.NotificationRouter;
import com.project.ads.models.Pqr;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.PqrProvider;
import com.project.ads.helpers.RequestsNavBadge;

import java.util.List;

/**
 * PQR · mockups 42 (cliente: pestaña de la barra inferior) y 54 (trabajador:
 * se abre desde Mi cuenta y tiene volver). Lista las PQR propias desde
 * GET /api/pqr/ y abre Crear PQR (mockup 43).
 */
public class PqrListActivity extends AppCompatActivity {

    // Solo el cliente: el trabajador abre PQR sin barra inferior
    private RequestsNavBadge mRequestsBadge;

    private static final String ROLE_WORKER = "trabajador";

    private final PqrProvider mProvider = new PqrProvider();
    private PqrAdapter mAdapter;

    private SwipeRefreshLayout mSwipe;
    private View mProgress, mEmpty, mRetry, mSection;
    private RecyclerView mRecycler;
    private TextView mEmptyTitle, mEmptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pqr_list);

        boolean isWorker = ROLE_WORKER.equals(NotificationRouter.role(this));
        View back = findViewById(R.id.btn_back);
        if (isWorker) {
            findViewById(R.id.include_bottom_nav).setVisibility(View.GONE);
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(v -> finish());
        } else {
            ClientNavHelper.setup(this, R.id.nav_client_pqr);
            mRequestsBadge = RequestsNavBadge.forClient(this);
        }

        mSwipe = findViewById(R.id.swipe_pqr);
        mProgress = findViewById(R.id.progress_pqr);
        mEmpty = findViewById(R.id.layout_pqr_empty);
        mEmptyTitle = findViewById(R.id.tv_pqr_empty_title);
        mEmptyMessage = findViewById(R.id.tv_pqr_empty_message);
        mRetry = findViewById(R.id.btn_pqr_retry);
        mSection = findViewById(R.id.tv_pqr_section);
        mRecycler = findViewById(R.id.recycler_pqr);

        mAdapter = new PqrAdapter();
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

        mSwipe.setColorSchemeResources(R.color.ads_brand);
        mSwipe.setOnRefreshListener(this::load);
        mRetry.setOnClickListener(v -> {
            mProgress.setVisibility(View.VISIBLE);
            mEmpty.setVisibility(View.GONE);
            load();
        });
        findViewById(R.id.btn_pqr_create).setOnClickListener(v ->
                startActivity(new Intent(this, PqrCreateActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mRequestsBadge != null) mRequestsBadge.start();
    }

    @Override
    protected void onStop() {
        if (mRequestsBadge != null) mRequestsBadge.stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver de Crear PQR la lista ya trae la nueva
        load();
    }

    private void load() {
        mProvider.getMyPqrs(new ApiCallback<List<Pqr>>() {
            @Override
            public void onSuccess(List<Pqr> data) {
                if (isFinishing()) return;
                done();
                mAdapter.setItems(data);
                Motion.animateListOnce(mRecycler);
                boolean empty = data == null || data.isEmpty();
                mSection.setVisibility(empty ? View.GONE : View.VISIBLE);
                mRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
                showEmpty(empty, R.string.pqr_empty_title, R.string.pqr_empty_message, false);
            }

            @Override
            public void onError(String message, int httpStatus) {
                if (isFinishing()) return;
                done();
                // Si ya había lista se conserva; si no, el estado de error con reintentar
                if (mAdapter.getItemCount() == 0) {
                    showEmpty(true, R.string.pqr_error_title, R.string.notifications_error_message, true);
                } else {
                    android.widget.Toast.makeText(PqrListActivity.this, message, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void done() {
        mProgress.setVisibility(View.GONE);
        mSwipe.setRefreshing(false);
    }

    private void showEmpty(boolean show, int title, int message, boolean retry) {
        mEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        mEmptyTitle.setText(title);
        mEmptyMessage.setText(message);
        mRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
    }
}
