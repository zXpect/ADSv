package com.project.ads.models.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Precio de referencia de una solicitud: lo devuelve POST /api/requests/estimate/
 * y el backend lo congela en requests/<id>/quote_reference al crearla.
 * min_cop/max_cop son null si ningún ítem tiene rango.
 */
public class QuoteReference {
    private Long min_cop;
    private Long max_cop;
    private int regional_factor_pct;
    private long min_visit_fee_cop;
    private boolean has_unpriced_items;
    private List<Item> items = new ArrayList<>();

    public Long getMinCop() {
        return min_cop;
    }

    public Long getMaxCop() {
        return max_cop;
    }

    public long getMinVisitFeeCop() {
        return min_visit_fee_cop;
    }

    public boolean hasUnpricedItems() {
        return has_unpriced_items;
    }

    public boolean hasRange() {
        return min_cop != null && max_cop != null;
    }

    public List<Item> getItems() {
        return items != null ? items : new ArrayList<>();
    }

    /**
     * Mismo criterio que el backend: el precio necesita motivo si hay ítems sin
     * rango o queda fuera de [min_cop, max_cop]. Sin referencia (solicitud
     * anterior al catálogo) todo cuenta como sin rango.
     */
    public static boolean isAdjusted(QuoteReference reference, long price) {
        if (reference == null || reference.has_unpriced_items || !reference.hasRange()) return true;
        return price < reference.min_cop || price > reference.max_cop;
    }

    public static class Item {
        private String task;
        private String custom_description;
        private double quantity;
        private String unit;
        private Long min_cop;
        private Long max_cop;

        public String getTask() {
            return task;
        }

        public String getCustomDescription() {
            return custom_description;
        }

        public double getQuantity() {
            return quantity;
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
    }

    /** Lee el objeto que el backend guardó en Firebase (requests/<id>/quote_reference). */
    @SuppressWarnings("unchecked")
    public static QuoteReference fromMap(Object value) {
        if (!(value instanceof Map)) return null;
        Map<String, Object> map = (Map<String, Object>) value;
        QuoteReference reference = new QuoteReference();
        reference.min_cop = toLong(map.get("min_cop"));
        reference.max_cop = toLong(map.get("max_cop"));
        Long factor = toLong(map.get("regional_factor_pct"));
        reference.regional_factor_pct = factor != null ? factor.intValue() : 0;
        Long fee = toLong(map.get("min_visit_fee_cop"));
        reference.min_visit_fee_cop = fee != null ? fee : 0;
        reference.has_unpriced_items = Boolean.TRUE.equals(map.get("has_unpriced_items"));

        Object items = map.get("items");
        if (items instanceof List) {
            for (Object raw : (List<Object>) items) {
                if (!(raw instanceof Map)) continue;
                Map<String, Object> itemMap = (Map<String, Object>) raw;
                Item item = new Item();
                item.task = itemMap.get("task") != null ? itemMap.get("task").toString() : null;
                item.custom_description = itemMap.get("custom_description") != null
                        ? itemMap.get("custom_description").toString() : null;
                item.quantity = itemMap.get("quantity") instanceof Number
                        ? ((Number) itemMap.get("quantity")).doubleValue() : 0;
                item.unit = itemMap.get("unit") != null ? itemMap.get("unit").toString() : null;
                item.min_cop = toLong(itemMap.get("min_cop"));
                item.max_cop = toLong(itemMap.get("max_cop"));
                reference.items.add(item);
            }
        }
        return reference;
    }

    private static Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}
