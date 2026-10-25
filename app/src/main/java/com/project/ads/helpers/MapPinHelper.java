package com.project.ads.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.project.ads.R;

/**
 * Pines del mapa (docs/branding/pines). Google Maps no lee vector drawables,
 * así que se dibujan en un Bitmap a su tamaño intrínseco (48 o 60 dp).
 */
public final class MapPinHelper {

    /** Ancla vertical de la punta: pin normal (24, 44) en un lienzo de 48 x 48. */
    public static final float ANCHOR_V = 44f / 48f;
    /** Ancla vertical de la punta: pin seleccionado (30, 56) en un lienzo de 60 x 60. */
    public static final float ANCHOR_V_SELECTED = 56f / 60f;

    /**
     * Cuadros del pin seleccionado para animar su tamaño (Google Maps no escala
     * marcadores): POP_FRAMES escalas desde POP_FROM, de POP_STEP en POP_STEP.
     * A 0,8 el pin seleccionado mide lo mismo que el normal (60 x 0,8 = 48 dp).
     */
    public static final float POP_FROM = 0.8f;
    public static final float POP_STEP = 0.04f;
    public static final int POP_FRAMES = 9;   // 0,80 … 1,12

    private MapPinHelper() {
    }

    /** El pin dibujado a cada escala de la animación (ver POP_FROM). */
    public static BitmapDescriptor[] popFrames(Context context, int drawableRes) {
        BitmapDescriptor[] frames = new BitmapDescriptor[POP_FRAMES];
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        for (int i = 0; i < POP_FRAMES; i++) {
            if (drawable == null) {
                frames[i] = BitmapDescriptorFactory.defaultMarker();
                continue;
            }
            float scale = POP_FROM + i * POP_STEP;
            int width = Math.max(1, Math.round(drawable.getIntrinsicWidth() * scale));
            int height = Math.max(1, Math.round(drawable.getIntrinsicHeight() * scale));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(new Canvas(bitmap));
            frames[i] = BitmapDescriptorFactory.fromBitmap(bitmap);
        }
        return frames;
    }

    /** Cuadros del halo del cliente: radio del halo de CLIENT_HALO_MIN a CLIENT_HALO_MAX. */
    public static final int CLIENT_FRAMES = 10;
    private static final float CLIENT_HALO_MIN = 15f;
    private static final float CLIENT_HALO_MAX = 22f;

    /**
     * Ubicación del cliente con la geometría de pin_cliente (lienzo de 48, centro
     * en 24, 24): halo naranja, punto navy con borde blanco y centro naranja. En
     * cada cuadro el halo crece y se aclara un poco, para que «respire».
     */
    public static BitmapDescriptor[] clientFrames(Context context) {
        return locationFrames(context, R.color.ads_pin_navy, R.color.ads_pin_brand);
    }

    /**
     * Ubicación del propio trabajador: la misma forma con los colores invertidos
     * (punto naranja, centro navy), para no confundirla con las solicitudes, que en
     * su mapa usan pin_cliente.
     */
    public static BitmapDescriptor[] workerFrames(Context context) {
        return locationFrames(context, R.color.ads_pin_brand, R.color.ads_pin_navy);
    }

    private static BitmapDescriptor[] locationFrames(Context context, int dotColorRes, int centerColorRes) {
        float density = context.getResources().getDisplayMetrics().density;
        int size = Math.round(48 * density);
        float center = size / 2f;
        int brand = ContextCompat.getColor(context, R.color.ads_pin_brand);
        int dot = ContextCompat.getColor(context, dotColorRes);
        int dotCenter = ContextCompat.getColor(context, centerColorRes);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapDescriptor[] frames = new BitmapDescriptor[CLIENT_FRAMES];
        for (int i = 0; i < CLIENT_FRAMES; i++) {
            float t = i / (float) (CLIENT_FRAMES - 1);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // Halo: de radio 15 al 30 % a radio 22 al 14 %
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(brand);
            paint.setAlpha(Math.round(255 * (0.30f - 0.16f * t)));
            canvas.drawCircle(center, center, (CLIENT_HALO_MIN + (CLIENT_HALO_MAX - CLIENT_HALO_MIN) * t) * density, paint);

            // Punto: borde blanco 3 y relleno de radio 9
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(center, center, 10.5f * density, paint);
            paint.setColor(dot);
            canvas.drawCircle(center, center, 7.5f * density, paint);

            // Centro
            paint.setColor(dotCenter);
            canvas.drawCircle(center, center, 3.5f * density, paint);
            frames[i] = BitmapDescriptorFactory.fromBitmap(bitmap);
        }
        return frames;
    }

    /** Cuadro más cercano a la escala pedida. */
    public static BitmapDescriptor frameFor(BitmapDescriptor[] frames, float scale) {
        int index = Math.round((scale - POP_FROM) / POP_STEP);
        return frames[Math.max(0, Math.min(frames.length - 1, index))];
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
