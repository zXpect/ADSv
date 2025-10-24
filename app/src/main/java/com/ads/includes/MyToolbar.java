package com.ads.includes;

import android.graphics.PorterDuff;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.ads.R;

public class MyToolbar {

    /**
     * Muestra el toolbar con fondo transparente y texto oscuro
     * Ideal para pantallas con fondo claro
     */
    public static void showTransparent(AppCompatActivity activity, String title, boolean upButton) {
        setupToolbar(activity, R.id.toolbar, title, upButton, R.color.text_primary, R.color.colorPrimary);
    }

    /**
     * Muestra el toolbar con los colores primarios de la app
     * Texto claro sobre fondo de color
     */
    public static void showColorPrimary(AppCompatActivity activity, String title, boolean upButton) {
        setupToolbar(activity, R.id.toolbar, title, upButton, R.color.textOnlywhite, R.color.textOnlywhite);
    }

    /**
     * Método por defecto - usa colores adaptables al tema
     * Compatible con modo oscuro/claro
     */
    public static void show(AppCompatActivity activity, String title, boolean upButton) {
        showAdaptive(activity, title, upButton);
    }

    /**
     * Toolbar adaptable que se ajusta automáticamente al tema actual
     * Recomendado para mejor compatibilidad con modo oscuro
     */
    public static void showAdaptive(AppCompatActivity activity, String title, boolean upButton) {
        setupToolbar(activity, R.id.toolbar, title, upButton, R.color.text_primary, R.color.colorPrimary);
    }

    /**
     * Configura el toolbar con los parámetros especificados
     */
    private static void setupToolbar(AppCompatActivity activity, int toolbarId, String title,
                                     boolean upButton, int titleColorRes, int iconColorRes) {
        Toolbar toolbar = activity.findViewById(toolbarId);
        if (toolbar == null) {
            throw new IllegalStateException("Toolbar not found. Make sure your layout includes one with id '@+id/toolbar'");
        }

        activity.setSupportActionBar(toolbar);

        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(title);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(upButton);

            // Mejora la elevación del título
            activity.getSupportActionBar().setElevation(0f);
        }

        // Aplicar color del título
        int titleColor = ContextCompat.getColor(activity, titleColorRes);
        toolbar.setTitleTextColor(titleColor);

        // Aplicar color al ícono de navegación (flecha atrás)
        if (upButton && toolbar.getNavigationIcon() != null) {
            int iconColor = ContextCompat.getColor(activity, iconColorRes);
            toolbar.getNavigationIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);
        }

        // Padding optimizado para mejor alineación
        toolbar.setContentInsetsAbsolute(
                toolbar.getContentInsetStart(),
                toolbar.getContentInsetEnd()
        );
    }

    /**
     * Oculta el toolbar si es necesario
     */
    public static void hide(AppCompatActivity activity) {
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().hide();
        }
    }

    /**
     * Muestra el toolbar si estaba oculto
     */
    public static void showToolbar(AppCompatActivity activity) {
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().show();
        }
    }

    /**
     * Actualiza solo el título del toolbar
     */
    public static void updateTitle(AppCompatActivity activity, String title) {
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(title);
        }
    }

    /**
     * Configura un subtítulo para el toolbar
     */
    public static void setSubtitle(AppCompatActivity activity, String subtitle) {
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setSubtitle(subtitle);
        }
    }
}