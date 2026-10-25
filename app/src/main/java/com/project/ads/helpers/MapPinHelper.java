package com.project.ads.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

/**
 * Pines del mapa (docs/branding/pines). Google Maps no lee vector drawables,
 * así que se dibujan en un Bitmap a su tamaño intrínseco (48 o 60 dp).
 */
public final class MapPinHelper {

    /** Ancla vertical de la punta: pin normal (24, 44) en un lienzo de 48 x 48. */
    public static final float ANCHOR_V = 44f / 48f;
    /** Ancla vertical de la punta: pin seleccionado (30, 56) en un lienzo de 60 x 60. */
    public static final float ANCHOR_V_SELECTED = 56f / 60f;

    private MapPinHelper() {
    }

    /** Llamar solo con el mapa listo: BitmapDescriptorFactory exige Maps inicializado. */
    public static BitmapDescriptor fromVector(Context context, int drawableRes) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        if (drawable == null) return BitmapDescriptorFactory.defaultMarker();
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(bitmap));
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
