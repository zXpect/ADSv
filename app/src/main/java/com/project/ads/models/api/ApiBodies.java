package com.project.ads.models.api;

/**
 * Cuerpos de las peticiones al backend. Los nombres de campo coinciden con el
 * JSON que espera la API (snake_case).
 */
public final class ApiBodies {

    private ApiBodies() {
    }

    /** Ítem de la solicitud: una tarea del catálogo o «Otro» (Gson omite el campo nulo). */
    public static class TaskItemBody {
        final String task;
        final String custom_description;
        final double quantity;

        private TaskItemBody(String task, String customDescription, double quantity) {
            this.task = task;
            this.custom_description = customDescription;
            this.quantity = quantity;
        }

        public static TaskItemBody ofTask(String taskSlug, double quantity) {
            return new TaskItemBody(taskSlug, null, quantity);
        }

        public static TaskItemBody custom(String description, double quantity) {
            return new TaskItemBody(null, description, quantity);
        }
    }

    public static class EstimateBody {
        final String service_category;
        final java.util.List<TaskItemBody> task_items;

        public EstimateBody(String serviceCategory, java.util.List<TaskItemBody> taskItems) {
            this.service_category = serviceCategory;
            this.task_items = taskItems;
        }
    }

    public static class CreateRequestBody extends EstimateBody {
        final String worker_id;
        final String address;
        final String description;
        final double lat;
        final double lng;
        final Long search_started_at;
        final Long worker_selected_at;

        public CreateRequestBody(String workerId, String serviceCategory, String address, String description,
                                 double lat, double lng, java.util.List<TaskItemBody> taskItems,
                                 Long searchStartedAt, Long workerSelectedAt) {
            super(serviceCategory, taskItems);
            this.worker_id = workerId;
            this.address = address;
            this.description = description;
            this.lat = lat;
            this.lng = lng;
            this.search_started_at = searchStartedAt;
            this.worker_selected_at = workerSelectedAt;
        }
    }

    public static class ScheduleBody {
        final long scheduled_at;            // ms desde 1970 (UTC)
        final int estimated_duration_min;

        public ScheduleBody(long scheduledAt, int estimatedDurationMin) {
            this.scheduled_at = scheduledAt;
            this.estimated_duration_min = estimatedDurationMin;
        }
    }

    public static class QuoteBody extends ScheduleBody {
        // Pesos COP enteros: el dinero nunca viaja como decimal
        final long quoted_price;
        final String adjustment_reason_code;   // null si el precio no se ajusta
        final String adjustment_reason_text;

        public QuoteBody(long quotedPrice, String reasonCode, String reasonText,
                         long scheduledAt, int estimatedDurationMin) {
            super(scheduledAt, estimatedDurationMin);
            this.quoted_price = quotedPrice;
            this.adjustment_reason_code = reasonCode;
            this.adjustment_reason_text = reasonText;
        }
    }

    public static class CancelBody {
        final String reason_code;
        final String reason_text;

        public CancelBody(String reasonCode, String reasonText) {
            this.reason_code = reasonCode;
            this.reason_text = reasonText;
        }
    }

    public static class RejectQuoteBody {
        final String reason;

        public RejectQuoteBody(String reason) {
            this.reason = reason;
        }
    }

    public static class RatingBody {
        final int rating;
        final String comment;

        public RatingBody(int rating, String comment) {
            this.rating = rating;
            this.comment = comment;
        }
    }

    /** Método de pago al aceptar la cotización o al cambiarlo (D10): "digital" o "cash". */
    public static class PaymentMethodBody {
        final String payment_method;

        public PaymentMethodBody(String paymentMethod) {
            this.payment_method = paymentMethod;
        }
    }

    public static class CreatePaymentBody {
        // Solo la solicitud: el monto lo calcula el servidor desde la cotización aceptada
        final String service_request_id;
        final String redirect_url;

        public CreatePaymentBody(String serviceRequestId, String redirectUrl) {
            this.service_request_id = serviceRequestId;
            this.redirect_url = redirectUrl;
        }
    }

    public static class PqrBody {
        final String service_request_id;
        final String type;
        final String description;

        public PqrBody(String serviceRequestId, String type, String description) {
            this.service_request_id = serviceRequestId;
            this.type = type;
            this.description = description;
        }
    }

    public static class NotificationBody {
        final String request_id;
        final String type;

        public NotificationBody(String requestId, String type) {
            this.request_id = requestId;
            this.type = type;
        }
    }
}
