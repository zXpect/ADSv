package com.project.ads.models.api;

/**
 * Cuerpos de las peticiones al backend. Los nombres de campo coinciden con el
 * JSON que espera la API (snake_case).
 */
public final class ApiBodies {

    private ApiBodies() {
    }

    public static class QuoteBody {
        // Pesos COP enteros: el dinero nunca viaja como decimal
        final long quoted_price;

        public QuoteBody(long quotedPrice) {
            this.quoted_price = quotedPrice;
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

    public static class CreatePaymentBody {
        // Solo la solicitud: el monto lo calcula el servidor desde la cotización aceptada
        final String service_request_id;
        final String redirect_url;

        public CreatePaymentBody(String serviceRequestId, String redirectUrl) {
            this.service_request_id = serviceRequestId;
            this.redirect_url = redirectUrl;
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
