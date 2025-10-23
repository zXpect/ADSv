package com.ads.includes;

import android.graphics.PorterDuff;
import android.widget.TextView;
import android.content.res.ColorStateList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.ads.activities.TermsConditionsActivity;
import com.project.ads.R;

public class MyToolbar {

    public static void showTransparent(AppCompatActivity activity, String title, boolean upButton) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setTitle(title);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(upButton);

        // Usar color adaptable desde resources (se ajusta con tema)
        int titleColor = ContextCompat.getColor(activity, R.color.black);
        toolbar.setTitleTextColor(titleColor);

        if (upButton && toolbar.getNavigationIcon() != null) {
            int iconColor = ContextCompat.getColor(activity, R.color.colorAccent);
            toolbar.getNavigationIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public static void showColorPrimary(AppCompatActivity activity, String title, boolean upButton) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar_color);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setTitle(title);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(upButton);

        // Usar color adaptable desde resources
        int titleColor = ContextCompat.getColor(activity, R.color.textOnlywhite);
        toolbar.setTitleTextColor(titleColor);

        if (upButton && toolbar.getNavigationIcon() != null) {
            int iconColor = ContextCompat.getColor(activity, R.color.colorAccent);
            toolbar.getNavigationIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public static void show(TermsConditionsActivity activity, String title, boolean upButton) {
        showColorPrimary(activity, title, upButton);
    }
}
