package com.project.ads.providers;

import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.CatalogData;
import com.project.ads.models.api.QuoteReference;
import com.project.ads.retrofit.AdsApiClient;
import com.project.ads.retrofit.ApiCallAdapter;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catálogo de oficios y tareas (GET /api/catalog/) y estimado de precio
 * (POST /api/requests/estimate/). El catálogo se guarda en memoria mientras la
 * app vive; si el backend no responde se usa la lista local de oficios, sin
 * tareas (sirve para mostrar nombres y filtrar, no para crear solicitudes).
 */
public class CatalogProvider {

    // Respaldo local slug -> nombre, en el orden del catálogo (semilla del backend)
    private static final Map<String, String> LOCAL_CATEGORIES = new LinkedHashMap<>();

    static {
        LOCAL_CATEGORIES.put("electricidad", "Electricidad");
        LOCAL_CATEGORIES.put("plomeria", "Plomería");
        LOCAL_CATEGORIES.put("pintura", "Pintura");
        LOCAL_CATEGORIES.put("albanileria", "Albañilería");
        LOCAL_CATEGORIES.put("cerrajeria", "Cerrajería");
        LOCAL_CATEGORIES.put("jardineria", "Jardinería");
        LOCAL_CATEGORIES.put("carpinteria", "Carpintería");
    }

    // Valores del campo viejo `work` que no coinciden con un slug al quitar
    // tildes (igual que pricing/services/worker_categories.py del backend)
    private static final Map<String, String> LEGACY_WORK = new LinkedHashMap<>();

    static {
        LEGACY_WORK.put("electricista", "electricidad");
        LEGACY_WORK.put("plomero", "plomeria");
        LEGACY_WORK.put("pintor", "pintura");
        LEGACY_WORK.put("albanil", "albanileria");
        LEGACY_WORK.put("cerrajero", "cerrajeria");
        LEGACY_WORK.put("jardinero", "jardineria");
        LEGACY_WORK.put("carpintero", "carpinteria");
    }

    private static CatalogData sCatalog;

    /** Catálogo del backend (o el que ya está en memoria). */
    public void getCatalog(ApiCallback<CatalogData> callback) {
        if (sCatalog != null) {
            callback.onSuccess(sCatalog);
            return;
        }
        ApiCallAdapter.enqueue(AdsApiClient.getApi().getCatalog(), new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData data) {
                sCatalog = data;
                callback.onSuccess(data);
            }

            @Override
            public void onError(String message, int httpStatus) {
                callback.onError(message, httpStatus);
            }
        });
    }

    /** Como getCatalog, pero si falla entrega los oficios locales sin tareas. */
    public void getCategoriesWithFallback(ApiCallback<CatalogData> callback) {
        getCatalog(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData data) {
                callback.onSuccess(data);
            }

            @Override
            public void onError(String message, int httpStatus) {
                callback.onSuccess(localCatalog());
            }
        });
    }

    public void estimate(String categorySlug, List<ApiBodies.TaskItemBody> items,
                         ApiCallback<QuoteReference> callback) {
        ApiCallAdapter.enqueue(
                AdsApiClient.getApi().estimate(new ApiBodies.EstimateBody(categorySlug, items)), callback);
    }

    // ---------- utilidades estáticas ----------

    /** Catálogo ya cargado en esta sesión, o null. */
    public static CatalogData cachedCatalog() {
        return sCatalog;
    }

    public static CatalogData localCatalog() {
        List<CatalogData.Category> categories = new ArrayList<>();
        for (Map.Entry<String, String> entry : LOCAL_CATEGORIES.entrySet()) {
            categories.add(new CatalogData.Category(entry.getKey(), entry.getValue()));
        }
        return new CatalogData(0, categories);
    }

    /** Nombre visible de un oficio; usa el catálogo en memoria y si no, el respaldo local. */
    public static String categoryName(String slug) {
        if (slug == null) return null;
        if (sCatalog != null) {
            CatalogData.Category category = sCatalog.findCategory(slug);
            if (category != null) return category.getName();
        }
        String local = LOCAL_CATEGORIES.get(slug);
        return local != null ? local : slug;
    }

    /** Slug del oficio que corresponde al texto viejo de `work`, o null. */
    public static String legacyWorkToSlug(String work) {
        if (work == null) return null;
        String value = Normalizer.normalize(work, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        String slug = LEGACY_WORK.containsKey(value) ? LEGACY_WORK.get(value) : value;
        return LOCAL_CATEGORIES.containsKey(slug) ? slug : null;
    }

    /** Lista de nombres para un conjunto de slugs: "Plomería, Pintura". */
    public static String joinNames(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return null;
        // Sin TextUtils para que se pueda probar en la JVM (WorkerTest)
        StringBuilder names = new StringBuilder();
        for (String slug : slugs) {
            if (names.length() > 0) names.append(", ");
            names.append(categoryName(slug));
        }
        return names.toString();
    }

    public static List<String> localSlugs() {
        return Collections.unmodifiableList(new ArrayList<>(LOCAL_CATEGORIES.keySet()));
    }
}
