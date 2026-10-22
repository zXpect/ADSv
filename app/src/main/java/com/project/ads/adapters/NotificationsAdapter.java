package com.project.ads.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.project.ads.R;
import com.project.ads.models.AppNotification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Lista de notificaciones agrupada en "Hoy" y "Antes" (mockups 40 y 53).
 * Las no leídas llevan borde naranja, ficha naranja y la etiqueta "Nueva".
 */
public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;

    private final List<Object> mRows = new ArrayList<>();
    private final OnNotificationClickListener mListener;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("d MMM", new Locale("es", "CO"));

    public NotificationsAdapter(OnNotificationClickListener listener) {
        mListener = listener;
    }

    /** Recibe la lista ya ordenada de la más reciente a la más antigua. */
    public void setNotifications(Context context, List<AppNotification> notifications) {
        mRows.clear();
        long startOfToday = startOfDay(0);
        boolean todayAdded = false, earlierAdded = false;
        for (AppNotification n : notifications) {
            if (n.getCreatedAt() >= startOfToday) {
                if (!todayAdded) {
                    mRows.add(context.getString(R.string.notifications_today));
                    todayAdded = true;
                }
            } else if (!earlierAdded) {
                mRows.add(context.getString(R.string.notifications_earlier));
                earlierAdded = true;
            }
            mRows.add(n);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mRows.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_notification_header, parent, false));
        }
        return new ItemHolder(inflater.inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = mRows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText((String) row);
        } else {
            ((ItemHolder) holder).bind((AppNotification) row);
        }
    }

    @DrawableRes
    private static int iconFor(String type) {
        if (type == null) return R.drawable.ic_ads_bell;
        switch (type) {
            case "new_request":
                return R.drawable.ic_nav_inbox;
            case "quote_received":
            case "quote_pending":
            case "requote_received":
                return R.drawable.ic_notif_quote;
            case "request_accepted":
            case "quote_accepted":
            case "requote_accepted":
                return R.drawable.ic_notif_accepted;
            case "request_rejected":
            case "quote_rejected":
            case "request_cancelled":
            case "requote_rejected":
                return R.drawable.ic_notif_cancelled;
            case "request_rescheduled":
            case "service_reminder":
                return R.drawable.ic_notif_schedule;
            case "payment_approved":
            case "cash_received":
            case "payment_pending":
                return R.drawable.ic_notif_payment;
            case "pqr_answered":
                return R.drawable.ic_nav_pqr;
            default:
                return R.drawable.ic_ads_bell;
        }
    }

    private static long startOfDay(int daysAgo) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -daysAgo);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String relativeTime(Context context, long createdAt) {
        long diff = System.currentTimeMillis() - createdAt;
        if (createdAt >= startOfDay(0)) {
            if (diff < MINUTE) return context.getString(R.string.notifications_time_now);
            if (diff < HOUR) return context.getString(R.string.notifications_time_minutes, (int) (diff / MINUTE));
            return context.getString(R.string.notifications_time_hours, (int) (diff / HOUR));
        }
        if (createdAt >= startOfDay(1)) return context.getString(R.string.notifications_time_yesterday);
        return mDateFormat.format(new Date(createdAt));
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tv_notification_header);
        }
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View iconTile;
        final ImageView icon;
        final TextView title, body, time, unread;

        ItemHolder(View view) {
            super(view);
            card = view.findViewById(R.id.card_notification);
            iconTile = view.findViewById(R.id.layout_notification_icon);
            icon = view.findViewById(R.id.iv_notification_icon);
            title = view.findViewById(R.id.tv_notification_title);
            body = view.findViewById(R.id.tv_notification_body);
            time = view.findViewById(R.id.tv_notification_time);
            unread = view.findViewById(R.id.tv_notification_unread);
        }

        void bind(AppNotification n) {
            Context context = itemView.getContext();
            boolean isUnread = !n.isRead();

            title.setText(n.getTitle());
            body.setText(n.getBody());
            body.setVisibility(n.getBody() == null || n.getBody().isEmpty() ? View.GONE : View.VISIBLE);
            time.setText(relativeTime(context, n.getCreatedAt()));
            unread.setVisibility(isUnread ? View.VISIBLE : View.GONE);

            icon.setImageResource(iconFor(n.getType()));
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(context,
                    isUnread ? R.color.ads_brand_text : R.color.ads_charcoal)));
            iconTile.setBackgroundResource(isUnread ? R.drawable.bg_ads_icon_tile : R.drawable.bg_ads_icon_tile_neutral);
            card.setStrokeColor(ContextCompat.getColor(context, isUnread ? R.color.ads_brand_text : R.color.ads_line));
            card.setStrokeWidth(context.getResources().getDimensionPixelSize(
                    isUnread ? R.dimen.stroke_focus : R.dimen.stroke_control));

            card.setOnClickListener(v -> mListener.onNotificationClick(n));
        }
    }
}
