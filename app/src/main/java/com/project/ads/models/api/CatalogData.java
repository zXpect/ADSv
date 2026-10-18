package com.project.ads.models.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de GET /api/catalog/: oficios y tareas activos con el rango de
 * referencia ya ajustado para Ubaté (lo calcula el backend).
 */
public class CatalogData {
    private int regional_factor_pct;
    private long min_visit_fee_cop;
    private List<Category> categories = new ArrayList<>();

    public CatalogData() {
    }

    public CatalogData(long minVisitFeeCop, List<Category> categories) {
        this.min_visit_fee_cop = minVisitFeeCop;
        this.categories = categories;
    }

    public int getRegionalFactorPct() {
        return regional_factor_pct;
    }

    public long getMinVisitFeeCop() {
        return min_visit_fee_cop;
    }

    public List<Category> getCategories() {
        return categories != null ? categories : new ArrayList<>();
    }

    public Category findCategory(String slug) {
        for (Category category : getCategories()) {
            if (category.getSlug().equals(slug)) return category;
        }
        return null;
    }

    public static class Category {
        private String slug;
        private String name;
        private List<Task> tasks = new ArrayList<>();

        public Category() {
        }

        public Category(String slug, String name) {
            this.slug = slug;
            this.name = name;
        }

        public String getSlug() {
            return slug;
        }

        public String getName() {
            return name;
        }

        public List<Task> getTasks() {
            return tasks != null ? tasks : new ArrayList<>();
        }

        public Task findTask(String taskSlug) {
            for (Task task : getTasks()) {
                if (task.getSlug().equals(taskSlug)) return task;
            }
            return null;
        }

        @Override
        public String toString() {
            // Lo usan los ArrayAdapter de los spinners
            return name;
        }
    }

    public static class Task {
        public static final String UNIT_M2 = "m2";

        private String slug;
        private String name;
        private String unit;      // visita | unidad | m2 | jornada
        private Long min_cop;     // null: la tarea no tiene rango
        private Long max_cop;

        public String getSlug() {
            return slug;
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public Long getMinCop() {
            return min_cop;
        }

        public Long getMaxCop() {
            return max_cop;
        }

        public boolean hasRange() {
            return min_cop != null && max_cop != null;
        }

        public boolean allowsDecimals() {
            return UNIT_M2.equals(unit);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Nombre visible de la unidad de cobro. */
    /** Unidad en singular o plural según la cantidad ("1 visita", "2 visitas"). */
    public static String unitLabel(String unit, double quantity) {
        String label = unitLabel(unit);
        if (quantity == 1 || "m2".equals(unit) || label.isEmpty()) return label;
        return label.endsWith("d") ? label + "es" : label + "s";
    }

    public static String unitLabel(String unit) {
        if (unit == null) return "";
        switch (unit) {
            case "visita":
                return "visita";
            case "unidad":
                return "unidad";
            case "m2":
                return "m²";
            case "jornada":
                return "jornada";
            default:
                return unit;
        }
    }
}
