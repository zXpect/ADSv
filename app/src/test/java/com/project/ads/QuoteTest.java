package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.project.ads.models.RequestTaskItem;
import com.project.ads.models.api.CatalogData;
import com.project.ads.models.api.QuoteReference;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Precio de referencia y motivo del ajuste (D5): la app pide motivo con el mismo
 * criterio que ServiceRequestService.is_adjusted_price del backend.
 */
public class QuoteTest {

    private static Map<String, Object> referenceMap(Object min, Object max, Object unpriced) {
        Map<String, Object> map = new HashMap<>();
        map.put("min_cop", min);
        map.put("max_cop", max);
        map.put("has_unpriced_items", unpriced);
        map.put("regional_factor_pct", -15L);
        map.put("min_visit_fee_cop", 25500L);
        return map;
    }

    private static QuoteReference reference(Object min, Object max, Object unpriced) {
        return QuoteReference.fromMap(referenceMap(min, max, unpriced));
    }

    @Test
    public void priceInsideTheRangeNeedsNoReason() {
        QuoteReference ref = reference(60000L, 90000L, false);
        assertFalse(QuoteReference.isAdjusted(ref, 60000));
        assertFalse(QuoteReference.isAdjusted(ref, 75000));
        assertFalse(QuoteReference.isAdjusted(ref, 90000));
    }

    @Test
    public void priceOutsideTheRangeNeedsReason() {
        QuoteReference ref = reference(60000L, 90000L, false);
        assertTrue(QuoteReference.isAdjusted(ref, 59900));
        assertTrue(QuoteReference.isAdjusted(ref, 90100));
    }

    @Test
    public void withoutRangeEveryPriceNeedsReason() {
        assertTrue(QuoteReference.isAdjusted(reference(60000L, 90000L, true), 75000));
        assertTrue(QuoteReference.isAdjusted(reference(null, null, true), 75000));
        // Solicitud anterior al catálogo, sin quote_reference
        assertTrue(QuoteReference.isAdjusted(null, 75000));
        // Sin has_unpriced_items el backend asume que hay ítems sin rango (DEF-11)
        Map<String, Object> withoutFlag = referenceMap(60000L, 90000L, false);
        withoutFlag.remove("has_unpriced_items");
        assertTrue(QuoteReference.isAdjusted(QuoteReference.fromMap(withoutFlag), 75000));
        assertNull(QuoteReference.fromMap("no es un mapa"));
    }

    @Test
    public void fromMapReadsWhatTheBackendFroze() {
        Map<String, Object> item = new HashMap<>();
        item.put("task", "destapar_desague");
        item.put("quantity", 2L);
        item.put("unit", "visita");
        item.put("min_cop", 40000L);
        item.put("max_cop", 60000L);
        Map<String, Object> map = referenceMap(40000, 60000.0, false);
        map.put("items", Arrays.asList(item, "basura"));

        QuoteReference ref = QuoteReference.fromMap(map);
        assertEquals(Long.valueOf(40000), ref.getMinCop());
        assertEquals(Long.valueOf(60000), ref.getMaxCop());
        assertEquals(25500L, ref.getMinVisitFeeCop());
        assertTrue(ref.hasRange());
        assertFalse(ref.hasUnpricedItems());
        assertEquals(1, ref.getItems().size());
        assertEquals("destapar_desague", ref.getItems().get(0).getTask());
        assertEquals(2.0, ref.getItems().get(0).getQuantity(), 0);
    }

    @Test
    public void unitLabels() {
        assertEquals("visita", CatalogData.unitLabel("visita", 1));
        assertEquals("visitas", CatalogData.unitLabel("visita", 2));
        assertEquals("unidades", CatalogData.unitLabel("unidad", 3));
        assertEquals("jornadas", CatalogData.unitLabel("jornada", 0.5));
        assertEquals("m²", CatalogData.unitLabel("m2", 12.5));
        assertEquals("", CatalogData.unitLabel(null, 2));
    }

    @Test
    public void requestTaskItemLabelsAndBody() {
        CatalogData.Task task = new com.google.gson.Gson().fromJson(
                "{\"slug\":\"pintar_muro\",\"name\":\"Pintar muro\",\"unit\":\"m2\",\"min_cop\":10000,\"max_cop\":15000}",
                CatalogData.Task.class);
        assertTrue(task.hasRange());
        assertTrue(task.allowsDecimals());

        RequestTaskItem item = RequestTaskItem.fromTask(task, 12.5);
        assertEquals("12,5 m²", item.getQuantityLabel());
        assertEquals("Pintar muro · 12,5 m²", item.getDisplayText());
        assertEquals("{\"task\":\"pintar_muro\",\"quantity\":12.5}", new com.google.gson.Gson().toJson(item.toBody()));

        RequestTaskItem custom = RequestTaskItem.custom("Revisar humedad", 1);
        assertTrue(custom.isCustom());
        assertEquals("1", custom.getQuantityLabel());
        assertEquals("Otro: Revisar humedad · 1", custom.getDisplayText());
        assertEquals("{\"custom_description\":\"Revisar humedad\",\"quantity\":1.0}",
                new com.google.gson.Gson().toJson(custom.toBody()));
    }

    @Test
    public void catalogLookups() {
        CatalogData catalog = new com.google.gson.Gson().fromJson(
                "{\"categories\":[{\"slug\":\"plomeria\",\"name\":\"Plomería\",\"tasks\":[{\"slug\":\"fuga\",\"name\":\"Fuga\"}]}]}",
                CatalogData.class);
        assertEquals("Plomería", catalog.findCategory("plomeria").getName());
        assertEquals("Fuga", catalog.findCategory("plomeria").findTask("fuga").getName());
        assertNull(catalog.findCategory("pintura"));
        assertFalse(catalog.findCategory("plomeria").findTask("fuga").hasRange());
    }
}
