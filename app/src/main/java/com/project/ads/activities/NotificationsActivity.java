package com.project.ads.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.adapters.NotificationsAdapter;
import com.project.ads.helpers.NotificationRouter;
import com.project.ads.models.AppNotification;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.NotificationHistoryProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notificaciones · mockups 40 (cliente) y 53 (trabajador). Lee el historial de
 * notifications/<uid> que guarda el backend al enviar cada push. Tocar una
 * notificación la marca como leída y abre la solicitud, igual que el push.
 */
public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private final NotificationHistoryProvider mProvider = new NotificationHistoryProvider();
    private final List<AppNotification> mNotifications = new ArrayList<>();
    private NotificationsAdapter mAdapter;
    private Query mQuery;
    private ValueEventListener mListener;
    private String mUid;

    private RecyclerView mRecycler;
    private View mProgress, mEmpty, mRetry, mMarkRead;
    private TextView mEmptyTitle, mEmptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mUid = new AuthProvider().getId();
        if (mUid == null) {
            finish();
            return;
        }

        // Push sin solicitud: se abrió esta pantalla; se marca esa notificación
        NotificationRouter.markOpened(this, null);

        mRecycler = findViewById(R.id.recycler_notifications);
        mProgress = findViewById(R.id.progress_notifications);
        mEmpty = findViewById(R.id.layout_notifications_empty);
        mEmptyTitle = findViewById(R.id.tv_notifications_empty_title);
        mEmptyMessage = findViewById(R.id.tv_notifications_empty_message);
        mRetry = findViewById(R.id.btn_notifications_retry);
        mMarkRead = findViewById(R.id.btn_notifications_mark_read);

        mAdapter = new NotificationsAdapter(this::openNotification);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        mMarkRead.setOnClickListener(v -> markAllAsRead());
        mRetry.setOnClickListener(v -> load());

        load();
    }

    private void load() {
        detach();
        mProgress.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.GONE);
        mRecycler.setVisibility(View.GONE);

        mQuery = mProvider.getNotifications(mUid);
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mNotifications.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    mNotifications.add(AppNotification.fromSnapshot(child));
                }
                Collections.reverse(mNotifications);
                showList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "No se pudo leer el historial: " + error.getMessage());
                // Mientras el backend no publique la regla de notifications/<uid>
                // la lectura se rechaza: se muestra como lista vacía, no como error.
                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                    mNotifications.clear();
                    showList();
                } else {
                    showError();
                }
            }
        };
        mQuery.addValueEventListener(mListener);
    }

    private void showList() {
        mProgress.setVisibility(View.GONE);
        mAdapter.setNotifications(this, mNotifications);
        boolean empty = mNotifications.isEmpty();
        mRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        mEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        mEmptyTitle.setText(R.string.notifications_empty_title);
        mEmptyMessage.setText(R.string.notifications_empty_message);
        mRetry.setVisibility(View.GONE);
        mMarkRead.setVisibility(unreadIds().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showError() {
        mProgress.setVisibility(View.GONE);
        mRecycler.setVisibility(View.GONE);
        mEmpty.setVisibility(View.VISIBLE);
        mEmptyTitle.setText(R.string.notifications_error_title);
        mEmptyMessage.setText(R.string.notifications_error_message);
        mRetry.setVisibility(View.VISIBLE);
        mMarkRead.setVisibility(View.GONE);
    }

    private List<String> unreadIds() {
        List<String> ids = new ArrayList<>();
        for (AppNotification n : mNotifications) {
            if (!n.isRead()) ids.add(n.getId());
        }
        return ids;
    }

    private void markAllAsRead() {
        List<String> ids = unreadIds();
        if (ids.isEmpty()) return;
        mMarkRead.setEnabled(false);
        mProvider.markAsRead(mUid, ids)
                .addOnFailureListener(e -> Toast.makeText(this, R.string.notifications_mark_read_error, Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(t -> mMarkRead.setEnabled(true));
    }

    /** Abre el mismo destino que el push (NotificationRouter). */
    private void openNotification(AppNotification notification) {
        if (!notification.isRead()) {
            mProvider.markAsRead(mUid, Collections.singletonList(notification.getId()));
        }
        Intent intent = NotificationRouter.destinationIntent(
                this, notification.getType(), notification.getRequestId(), null);
        // Sin solicitud o tipo desconocido el destino es esta misma pantalla
        if (NotificationsActivity.class.getName().equals(intent.getComponent().getClassName())) return;
        startActivity(intent);
    }

    private void detach() {
        if (mQuery != null && mListener != null) {
            mQuery.removeEventListener(mListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detach();
    }
}
