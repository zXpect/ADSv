package com.project.ads.helpers;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.project.ads.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Aviso sin conexión (HU-35). Escucha la red desde MyApplication y, mientras no
 * haya internet, muestra una franja arriba de la pantalla abierta, sea cual sea.
 * Firebase guarda las escrituras y las envía al volver; las llamadas al backend
 * sí fallan, por eso el aviso dice que algunas acciones no están disponibles.
 */
public final class ConnectivityBanner implements Application.ActivityLifecycleCallbacks {

    private static final String TAG_BANNER = "ads_offline_banner";

    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final Set<Network> mNetworks = new HashSet<>();
    private volatile boolean mOnline = true;
    private Activity mResumed;

    private ConnectivityBanner() {
    }

    public static void install(Application app) {
        ConnectivityBanner banner = new ConnectivityBanner();
        app.registerActivityLifecycleCallbacks(banner);
        ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        banner.mOnline = hasInternet(cm, cm.getActiveNetwork());
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                synchronized (banner.mNetworks) {
                    banner.mNetworks.add(network);
                }
                banner.setOnline(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                boolean any;
                synchronized (banner.mNetworks) {
                    banner.mNetworks.remove(network);
                    any = !banner.mNetworks.isEmpty();
                }
                banner.setOnline(any || hasInternet(cm, cm.getActiveNetwork()));
            }
        });
    }

    private static boolean hasInternet(ConnectivityManager cm, @Nullable Network network) {
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void setOnline(boolean online) {
        if (mOnline == online) return;
        mOnline = online;
        mMain.post(() -> {
            if (mResumed != null) update(mResumed);
        });
    }

    private void update(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;
        View banner = content.findViewWithTag(TAG_BANNER);
        if (mOnline) {
            if (banner != null) banner.setVisibility(View.GONE);
            return;
        }
        if (banner == null) {
            banner = createBanner(activity);
            content.addView(banner, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));
        }
        banner.setVisibility(View.VISIBLE);
        banner.bringToFront();
    }

    private static View createBanner(Activity activity) {
        TextView banner = new TextView(activity);
        banner.setTag(TAG_BANNER);
        banner.setText(R.string.offline_banner);
        banner.setTextAppearance(R.style.TextAppearance_ADS_Caption);
        banner.setTextColor(ContextCompat.getColor(activity, R.color.ads_on_navy));
        banner.setBackgroundColor(ContextCompat.getColor(activity, R.color.ads_header));
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setMinHeight(activity.getResources().getDimensionPixelSize(R.dimen.touch_min));
        int h = activity.getResources().getDimensionPixelSize(R.dimen.screen_margin);
        int v = activity.getResources().getDimensionPixelSize(R.dimen.space_2);
        banner.setPadding(h, v, h, v);
        banner.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_offline, 0, 0, 0);
        banner.setCompoundDrawablePadding(activity.getResources().getDimensionPixelSize(R.dimen.space_3));
        // El lector de pantalla lo anuncia al aparecer
        ViewCompat.setAccessibilityLiveRegion(banner, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        // No deja pasar los toques a lo que tapa
        banner.setClickable(true);
        return banner;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mResumed = activity;
        update(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (mResumed == activity) mResumed = null;
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) { }
    @Override public void onActivityStarted(@NonNull Activity activity) { }
    @Override public void onActivityStopped(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle state) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
