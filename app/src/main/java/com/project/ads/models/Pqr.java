package com.project.ads.models;

import com.project.ads.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Petición, queja o reclamo (B11, D21). Vive en el backend (SQL), no en Firebase:
 * GET/POST /api/pqr/. Las fechas llegan en ISO 8601 con la zona de Colombia.
 */
public class Pqr {

    public static final String TYPE_PETITION = "peticion";
    public static final String TYPE_COMPLAINT = "queja";
    public static final String TYPE_CLAIM = "reclamo";

    public static final String STATUS_OPEN = "abierta";
    public static final String STATUS_IN_PROGRESS = "en_tramite";
    public static final String STATUS_CLOSED = "cerrada";

    private static final Locale LOCALE = new Locale("es", "CO");

    private long id;
    private String service_request_id;
    private String author_role;
    private String type;
    private String description;
    private String status;
    private String response;
    private String created_at;
    private String due_at;
    private String closed_at;

    public long getId() { return id; }
    public String getServiceRequestId() { return service_request_id; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getResponse() { return response; }
    public String getCreatedAt() { return created_at; }
    public String getDueAt() { return due_at; }

    public boolean isClosed() {
        return STATUS_CLOSED.equals(status);
    }

    public boolean hasResponse() {
        return response != null && !response.trim().isEmpty();
    }

    public static int typeLabel(String type) {
        if (TYPE_PETITION.equals(type)) return R.string.pqr_type_petition;
        if (TYPE_CLAIM.equals(type)) return R.string.pqr_type_claim;
        return R.string.pqr_type_complaint;
    }

    /** Cerrar exige respuesta en el panel, así que «cerrada» se muestra como «Respondida». */
    public static int statusLabel(String status) {
        if (STATUS_IN_PROGRESS.equals(status)) return R.string.pqr_status_in_progress;
        if (STATUS_CLOSED.equals(status)) return R.string.pqr_status_closed;
        return R.string.pqr_status_open;
    }

    /** «14 oct» a partir de la fecha ISO (solo la parte de la fecha, que ya viene en hora de Colombia). */
    public static String shortDate(String iso) {
        if (iso == null || iso.length() < 10) return "";
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.substring(0, 10));
            return new SimpleDateFormat("d MMM", LOCALE).format(date).replace(".", "");
        } catch (ParseException e) {
            return "";
        }
    }
}
