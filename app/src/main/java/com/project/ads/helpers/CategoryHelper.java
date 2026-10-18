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
