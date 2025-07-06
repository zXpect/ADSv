package com.ads.helpers;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import com.project.ads.R;

public class DatabaseHelper {

    private DatabaseReference mDatabase;


    public DatabaseHelper() {
        mDatabase = FirebaseDatabase.getInstance().getReference();

    }

    public void uploadWorkerIcons(Resources resources) {
        String[] workerTypes = {"carpintería", "ferretería", "pintor", "electricista", "plomería", "jardinería", "albañilería"};
        int[] iconResources = {
                R.drawable.icon_carpenter,
                R.drawable.icon_ferreteria,
                R.drawable.icon_painter,
                R.drawable.icon_electrician,
                R.drawable.icon_plumber,
                R.drawable.icon_gardener,
                R.drawable.icon_mason
        };

        for (int i = 0; i < workerTypes.length; i++) {
            final String workerType = workerTypes[i];
            final int iconResource = iconResources[i];

            Bitmap bitmap = BitmapFactory.decodeResource(resources, iconResource);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

        }
    }
}