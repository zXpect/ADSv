package com.project.ads.models;

import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.CatalogData;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Ítem que el cliente agrega a la solicitud antes de enviarla: una tarea del
 * catálogo con su cantidad, o «Otro» con descripción libre. Se pasa entre
 * pantallas como JSON (Gson).
 */
public class RequestTaskItem {
    private String taskSlug;            // null en «Otro»
    private String name;
    private String unit;
    private String customDescription;   // solo en «Otro»
    private double quantity;

    public static RequestTaskItem fromTask(CatalogData.Task task, double quantity) {
        RequestTaskItem item = new RequestTaskItem();
        item.taskSlug = task.getSlug();
        item.name = task.getName();
        item.unit = task.getUnit();
        item.quantity = quantity;
        return item;
    }

    public static RequestTaskItem custom(String description, double quantity) {
        RequestTaskItem item = new RequestTaskItem();
        item.customDescription = description;
        item.name = description;
        item.quantity = quantity;
        return item;
    }

    public ApiBodies.TaskItemBody toBody() {
        return taskSlug != null
                ? ApiBodies.TaskItemBody.ofTask(taskSlug, quantity)
                : ApiBodies.TaskItemBody.custom(customDescription, quantity);
    }

    public boolean isCustom() {
        return taskSlug == null;
    }

    public String getTaskSlug() {
        return taskSlug;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    /** Nombre de la tarea, o la descripción en «Otro». */
    public String getName() {
        return name;
    }

    /** "1 visita", "2 unidades", "12,5 m²"; en «Otro» solo la cantidad. */
    public String getQuantityLabel() {
        String quantityText = formatQuantity(quantity);
        if (isCustom() || unit == null) return quantityText;
        return quantityText + " " + CatalogData.unitLabel(unit, quantity);
    }

    /** "Pintura sobre estuco, 2 manos · 12,5 m²" o "Otro: Revisar humedad · 1". */
    public String getDisplayText() {
        String quantityText = formatQuantity(quantity);
        if (isCustom()) {
            return "Otro: " + customDescription + " · " + quantityText;
        }
        return name + " · " + quantityText + " " + CatalogData.unitLabel(unit);
    }

    public static String formatQuantity(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.##", new DecimalFormatSymbols(new Locale("es", "CO")));
        return format.format(value);
    }
}
