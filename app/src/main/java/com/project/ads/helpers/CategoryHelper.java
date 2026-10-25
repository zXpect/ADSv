package com.project.ads.helpers;

import com.project.ads.R;
import com.project.ads.models.Worker;
import com.project.ads.models.api.CatalogData;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades de oficios para la UI: icono por slug y opciones de los filtros
 * (reemplaza el arreglo fijo R.array.service_types).
 */
public final class CategoryHelper {

    /** Valor del filtro "Todos los servicios". */
    public static final String ALL = "todos";

    private CategoryHelper() {
    }

    public static int iconFor(String slug) {
        if (slug == null) return R.drawable.icon_worker;
        switch (slug) {
            case "carpinteria":
                return R.drawable.icon_carpenter;
            case "pintura":
                return R.drawable.icon_painter;
            case "electricidad":
                return R.drawable.icon_electrician;
            case "plomeria":
                return R.drawable.icon_plumber;
            case "jardineria":
                return R.drawable.icon_gardener;
            case "albanileria":
                return R.drawable.icon_mason;
            case "cerrajeria":
                return R.drawable.icon_locksmith;
            default:
                return R.drawable.icon_worker;
        }
    }

    /**
     * Pin del mapa para el oficio (docs/branding/pines): normal o seleccionado.
     * Devuelve 0 si el oficio no tiene pin propio.
     */
    public static int pinFor(String slug, boolean selected) {
        if (slug == null) return 0;
        switch (slug) {
            case "carpinteria":
                return selected ? R.drawable.pin_sel_carpinteria : R.drawable.pin_carpinteria;
            case "pintura":
                return selected ? R.drawable.pin_sel_pintura : R.drawable.pin_pintura;
            case "electricidad":
                return selected ? R.drawable.pin_sel_electricidad : R.drawable.pin_electricidad;
            case "plomeria":
                return selected ? R.drawable.pin_sel_plomeria : R.drawable.pin_plomeria;
            case "jardineria":
                return selected ? R.drawable.pin_sel_jardineria : R.drawable.pin_jardineria;
            case "albanileria":
                return selected ? R.drawable.pin_sel_albanileria : R.drawable.pin_albanileria;
            case "cerrajeria":
                return selected ? R.drawable.pin_sel_cerrajeria : R.drawable.pin_cerrajeria;
            default:
                return 0;
        }
    }

    /** Pin del primer oficio del trabajador. */
    public static int pinFor(Worker worker, boolean selected) {
        List<String> slugs = worker.getCategorySlugs();
        return pinFor(slugs.isEmpty() ? null : slugs.get(0), selected);
    }

    /** Icono del primer oficio del trabajador. */
    public static int iconFor(Worker worker) {
        List<String> slugs = worker.getCategorySlugs();
        return iconFor(slugs.isEmpty() ? null : slugs.get(0));
    }

    /** Opciones del filtro: "Todos los servicios" y los oficios del catálogo. */
    public static List<CatalogData.Category> filterOptions(CatalogData catalog, String allLabel) {
        List<CatalogData.Category> options = new ArrayList<>();
        options.add(new CatalogData.Category(ALL, allLabel));
        options.addAll(catalog.getCategories());
        return options;
    }

    public static boolean matches(Worker worker, String filterSlug) {
        return ALL.equals(filterSlug) || worker.offersCategory(filterSlug);
    }
}
