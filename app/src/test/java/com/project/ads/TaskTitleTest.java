package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.CatalogData;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Título corto de las tarjetas de Mis trabajos e Historial (mockups 41 y 52). */
public class TaskTitleTest {

    private static final CatalogData CATALOG = new Gson().fromJson(
            "{\"categories\":[{\"slug\":\"electricidad\",\"name\":\"Electricidad\",\"tasks\":["
                    + "{\"slug\":\"reparar-cortocircuito\",\"name\":\"Reparar cortocircuito\",\"unit\":\"visita\"},"
                    + "{\"slug\":\"cambiar-interruptor\",\"name\":\"Cambiar interruptor\",\"unit\":\"unidad\"}]}]}",
            CatalogData.class);

    private static Map<String, Object> item(String task, double quantity) {
        Map<String, Object> item = new HashMap<>();
        item.put("task", task);
        item.put("quantity", quantity);
        return item;
    }

    private static ServiceRequest request(List<Map<String, Object>> items) {
        Map<String, Object> map = new HashMap<>();
        map.put("service_category", "electricidad");
        map.put("task_items", items);
        return ServiceRequest.fromMap(map);
    }

    @Test
    public void oneTaskOneUnitShowsOnlyTheName() {
        assertEquals("Reparar cortocircuito",
                request(Arrays.asList(item("reparar-cortocircuito", 1))).getTaskTitle(CATALOG));
    }

    @Test
    public void severalUnitsShowTheQuantity() {
        assertEquals("Cambiar interruptor · 2 unidades",
                request(Arrays.asList(item("cambiar-interruptor", 2))).getTaskTitle(CATALOG));
    }

    @Test
    public void moreTasksAddACount() {
        assertEquals("Reparar cortocircuito +1", request(Arrays.asList(
                item("reparar-cortocircuito", 1), item("cambiar-interruptor", 3))).getTaskTitle(CATALOG));
    }

    @Test
    public void customTaskUsesItsDescription() {
        Map<String, Object> custom = new HashMap<>();
        custom.put("custom_description", "Revisar humedad");
        custom.put("quantity", 1);
        assertEquals("Revisar humedad", request(Arrays.asList(custom)).getTaskTitle(null));
    }

    @Test
    public void withoutCatalogOrTasksFallsBack() {
        assertNull(request(Arrays.asList(item("reparar-cortocircuito", 1))).getTaskTitle(null));
        assertNull(request(Arrays.asList(item("tarea-borrada", 1))).getTaskTitle(CATALOG));
        assertNull(request(null).getTaskTitle(CATALOG));
    }
}
