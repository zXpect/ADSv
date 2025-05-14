package com.ads.includes;

import android.graphics.Color;
import android.graphics.PorterDuff;

import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.Toolbar;

import com.ads.activities.TermsConditionsActivity;
import com.project.ads.R;

public class MyToolbar {
    public static void showTransparent(AppCompatActivity activity, String tittle, boolean upButton) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setTitle(tittle);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(upButton);
        if (upButton) {
            toolbar.getNavigationIcon().setColorFilter(Color.parseColor("#FF8C00"), PorterDuff.Mode.SRC_ATOP);
        }

    }
    public static void showColorPrimary(AppCompatActivity activity, String tittle, boolean upButton) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar_color);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setTitle(tittle);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(upButton);
        if (upButton) {
            toolbar.getNavigationIcon().setColorFilter(Color.parseColor("#FF8C00"), PorterDuff.Mode.SRC_ATOP); // Naranja
        }
    }

    public static void show(TermsConditionsActivity termsConditionsActivity, String términosYCondiciones, boolean b) {
    }
}
