package com.project.ads.helpers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Códigos de motivo que acepta el backend y su texto visible.
 * Ajuste de la cotización (D5) y cancelación por rol (D8); con «otro» el
 * texto es obligatorio.
 */
public final class ReasonHelper {

    public static final String OTHER = "otro";
    public static final int MAX_TEXT = 300;

    public static final String ROLE_CLIENT = "client";
    public static final String ROLE_WORKER = "worker";

    public static final Map<String, String> ADJUSTMENT = new LinkedHashMap<>();
    public static final Map<String, String> CANCELLATION_CLIENT = new LinkedHashMap<>();
    public static final Map<String, String> CANCELLATION_WORKER = new LinkedHashMap<>();

    static {
        ADJUSTMENT.put("materiales", "Materiales");
        ADJUSTMENT.put("dificultad", "Dificultad del trabajo");
        ADJUSTMENT.put("tiempo", "Tiempo requerido");
        ADJUSTMENT.put("condiciones_lugar", "Condiciones del lugar");
        ADJUSTMENT.put(OTHER, "Otro");

        CANCELLATION_CLIENT.put("ya_no_lo_necesito", "Ya no lo necesito");
        CANCELLATION_CLIENT.put("encontre_otra_opcion", "Encontré otra opción");
        CANCELLATION_CLIENT.put("precio", "Por el precio");
        CANCELLATION_CLIENT.put("horario", "Por el horario");
        CANCELLATION_CLIENT.put(OTHER, "Otro");

        CANCELLATION_WORKER.put("no_disponible", "No estoy disponible");
        CANCELLATION_WORKER.put("fuera_de_zona", "Está fuera de mi zona");
        CANCELLATION_WORKER.put("trabajo_no_corresponde", "El trabajo no corresponde a mi oficio");
        CANCELLATION_WORKER.put(OTHER, "Otro");
    }

    private ReasonHelper() {
    }

    public static Map<String, String> cancellationReasons(String role) {
        return ROLE_WORKER.equals(role) ? CANCELLATION_WORKER : CANCELLATION_CLIENT;
    }

    /** Texto visible de un motivo; con «otro» se muestra el texto libre. */
    public static String label(Map<String, String> reasons, String code, String text) {
        if (code == null) return null;
        if (OTHER.equals(code) && text != null && !text.isEmpty()) return text;
        String label = reasons.get(code);
        return label != null ? label : code;
    }

    public static String cancellationLabel(String code, String text) {
        String label = label(CANCELLATION_CLIENT, code, text);
        return label != null && label.equals(code) ? label(CANCELLATION_WORKER, code, text) : label;
    }
}
