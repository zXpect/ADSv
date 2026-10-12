package com.project.ads.models.api;

/**
 * Respuesta de las acciones sobre una solicitud que no son de cotización:
 * crear (POST /api/requests/), reprogramar (schedule/) y cancelar (cancel/).
 * Cada ruta llena solo sus campos.
 */
public class RequestActionData {
    private String request_id;
    private String status;
    private QuoteReference quote_reference;
    private Long scheduled_at;
    private Integer estimated_duration_min;
    private Integer reschedule_count;
    private String cancelled_by;

    public String getRequestId() {
        return request_id;
    }

    public String getStatus() {
        return status;
    }

    public QuoteReference getQuoteReference() {
        return quote_reference;
    }

    public Long getScheduledAt() {
        return scheduled_at;
    }

    public Integer getEstimatedDurationMin() {
        return estimated_duration_min;
    }

    public Integer getRescheduleCount() {
        return reschedule_count;
    }

    public String getCancelledBy() {
        return cancelled_by;
    }
}
