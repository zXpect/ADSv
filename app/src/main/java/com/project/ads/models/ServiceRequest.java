package com.project.ads.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ServiceRequest {
    private String request_id;
    private String client_id;
    private String client_name;
    private String client_phone;
    private String client_email;
    private String worker_id;
    private String worker_name;
    private String worker_phone;
    private String service_type;
    private String address;
    private String description;
    private String status;
    private String urgency_level;
    private double estimated_cost;
    private long timestamp;
    private long last_updated;
    private long started_trip_at;
    private long started_work_at;
    private long completed_at;
    private long cancelled_at;
    private double rating;
    private String rating_comment;
    private boolean is_rated;

    // Cotización y pago: solo los escribe el backend (ver reglas de Firebase)
    private long quoted_price;          // pesos COP enteros; 0 = sin cotización
    private String quote_status;        // quoted | accepted | rejected
    private String quote_rejection_reason;
    private String payment_status;      // pending | approved | declined | error | voided
    private String payment_reference;

    // Constructor vacío requerido por Firebase
    public ServiceRequest() {
    }

    // Constructor completo
    public ServiceRequest(String client_id, String worker_id, String service_type,
                          String address, String description, String status) {
        this.client_id = client_id;
        this.worker_id = worker_id;
        this.service_type = service_type;
        this.address = address;
        this.description = description;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
        this.last_updated = System.currentTimeMillis();
    }

    // Método estático para crear desde Map (Firebase)
    public static ServiceRequest fromMap(Map<String, Object> map) {
        if (map == null) return null;

        ServiceRequest request = new ServiceRequest();

        request.client_id = getStringValue(map, "client_id");
        request.client_name = getStringValue(map, "client_name");
        request.client_phone = getStringValue(map, "client_phone");
        request.client_email = getStringValue(map, "client_email");
        request.worker_id = getStringValue(map, "worker_id");
        request.worker_name = getStringValue(map, "worker_name");
        request.worker_phone = getStringValue(map, "worker_phone");
        request.service_type = getStringValue(map, "service_type");
        request.address = getStringValue(map, "address");
        request.description = getStringValue(map, "description");
        request.status = getStringValue(map, "status");
        request.urgency_level = getStringValue(map, "urgency_level");
        request.estimated_cost = getDoubleValue(map, "estimated_cost");
        request.timestamp = getLongValue(map, "timestamp");
        request.last_updated = getLongValue(map, "last_updated");
        request.started_trip_at = getLongValue(map, "started_trip_at");
        request.started_work_at = getLongValue(map, "started_work_at");
        request.completed_at = getLongValue(map, "completed_at");
        request.cancelled_at = getLongValue(map, "cancelled_at");
        request.rating = getDoubleValue(map, "rating");
        request.rating_comment = getStringValue(map, "rating_comment");
        request.is_rated = getBooleanValue(map, "is_rated");
        request.quoted_price = getLongValue(map, "quoted_price");
        request.quote_status = getStringValue(map, "quote_status");
        request.quote_rejection_reason = getStringValue(map, "quote_rejection_reason");
        request.payment_status = getStringValue(map, "payment_status");
        request.payment_reference = getStringValue(map, "payment_reference");

        return request;
    }

    private static boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return false;
    }

    // Métodos auxiliares para extraer valores de Map
    private static String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Double) return ((Double) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Métodos de utilidad para UI
    public String getStatusDisplayText() {
        if (status == null) return "Desconocido";

        switch (status.toLowerCase()) {
            case "pending":
                return "Pendiente";
            case "accepted":
                return "Aceptado";
            case "on_the_way":
                return "En Camino";
            case "in_progress":
                return "En Progreso";
            case "completed":
                return "Completado";
            case "cancelled":
                return "Cancelado";
            case "rejected":
                return "Rechazado";
            default:
                return status;
        }
    }

    public String getStatusColor() {
        if (status == null) return "#9E9E9E";

        switch (status.toLowerCase()) {
            case "pending":
                return "#FFC107"; // Amarillo
            case "accepted":
                return "#4CAF50"; // Verde
            case "on_the_way":
                return "#2196F3"; // Azul
            case "in_progress":
                return "#9C27B0"; // Morado
            case "completed":
                return "#4CAF50"; // Verde
            case "cancelled":
            case "rejected":
                return "#F44336"; // Rojo
            default:
                return "#9E9E9E"; // Gris
        }
    }

    public String getTimeAgo() {
        if (timestamp == 0) return "Hace un momento";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return "Hace " + days + (days == 1 ? " día" : " días");
        } else if (hours > 0) {
            return "Hace " + hours + (hours == 1 ? " hora" : " horas");
        } else if (minutes > 0) {
            return "Hace " + minutes + (minutes == 1 ? " minuto" : " minutos");
        } else {
            return "Hace un momento";
        }
    }

    public String getFormattedTimestamp() {
        if (timestamp == 0) return "Fecha desconocida";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedDate() {
        if (timestamp == 0) return "Fecha desconocida";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedTime() {
        if (timestamp == 0) return "Hora desconocida";

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public boolean isPending() {
        return status != null && status.equalsIgnoreCase("pending");
    }

    public boolean isAccepted() {
        return status != null && status.equalsIgnoreCase("accepted");
    }

    public boolean isOnTheWay() {
        return status != null && status.equalsIgnoreCase("on_the_way");
    }

    public boolean isInProgress() {
        return status != null && status.equalsIgnoreCase("in_progress");
    }

    public boolean isCompleted() {
        return status != null && status.equalsIgnoreCase("completed");
    }

    public boolean isCancelled() {
        return status != null && status.equalsIgnoreCase("cancelled");
    }

    public boolean isRejected() {
        return status != null && status.equalsIgnoreCase("rejected");
    }

    public boolean isActive() {
        return isAccepted() || isOnTheWay() || isInProgress();
    }

    /**
     * El cliente puede cancelar mientras no haya un precio acordado
     * (mismo criterio que las reglas de Firebase).
     */
    public boolean canBeCancelled() {
        return isPending() || (isAccepted() && !isQuoteAccepted());
    }

    public boolean canBeRated() {
        return isCompleted() && isPaid() && !is_rated;
    }

    // ---------- Cotización y pago ----------

    public boolean hasQuote() {
        return quoted_price > 0 && quote_status != null;
    }

    public boolean isQuotePendingAnswer() {
        return "quoted".equals(quote_status);
    }

    public boolean isQuoteAccepted() {
        return "accepted".equals(quote_status);
    }

    public boolean isQuoteRejected() {
        return "rejected".equals(quote_status);
    }

    public boolean isPaid() {
        return "approved".equals(payment_status);
    }

    public boolean isPaymentPending() {
        return "pending".equals(payment_status);
    }

    /** Formato $85.000 (pesos colombianos, sin decimales). */
    public static String formatCop(long amount) {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(new Locale("es", "CO"));
        symbols.setGroupingSeparator('.');
        return "$" + new java.text.DecimalFormat("#,##0", symbols).format(amount);
    }

    public String getFormattedQuotedPrice() {
        return formatCop(quoted_price);
    }

    // Getters y Setters
    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public String getClient_phone() {
        return client_phone;
    }

    public void setClient_phone(String client_phone) {
        this.client_phone = client_phone;
    }

    public String getClient_email() {
        return client_email;
    }

    public void setClient_email(String client_email) {
        this.client_email = client_email;
    }

    public String getWorker_id() {
        return worker_id;
    }

    public void setWorker_id(String worker_id) {
        this.worker_id = worker_id;
    }

    public String getWorker_name() {
        return worker_name;
    }

    public void setWorker_name(String worker_name) {
        this.worker_name = worker_name;
    }

    public String getWorker_phone() {
        return worker_phone;
    }

    public void setWorker_phone(String worker_phone) {
        this.worker_phone = worker_phone;
    }

    public String getService_type() {
        return service_type;
    }

    public void setService_type(String service_type) {
        this.service_type = service_type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrgency_level() {
        return urgency_level;
    }

    public void setUrgency_level(String urgency_level) {
        this.urgency_level = urgency_level;
    }

    public double getEstimated_cost() {
        return estimated_cost;
    }

    public void setEstimated_cost(double estimated_cost) {
        this.estimated_cost = estimated_cost;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getLast_updated() {
        return last_updated;
    }

    public void setLast_updated(long last_updated) {
        this.last_updated = last_updated;
    }

    public long getStarted_trip_at() {
        return started_trip_at;
    }

    public void setStarted_trip_at(long started_trip_at) {
        this.started_trip_at = started_trip_at;
    }

    public long getStarted_work_at() {
        return started_work_at;
    }

    public void setStarted_work_at(long started_work_at) {
        this.started_work_at = started_work_at;
    }

    public long getCompleted_at() {
        return completed_at;
    }

    public void setCompleted_at(long completed_at) {
        this.completed_at = completed_at;
    }

    public long getCancelled_at() {
        return cancelled_at;
    }

    public void setCancelled_at(long cancelled_at) {
        this.cancelled_at = cancelled_at;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getRating_comment() {
        return rating_comment;
    }

    public void setRating_comment(String rating_comment) {
        this.rating_comment = rating_comment;
    }

    public boolean isRated() {
        return is_rated;
    }

    public void setIs_rated(boolean is_rated) {
        this.is_rated = is_rated;
    }

    public long getQuoted_price() {
        return quoted_price;
    }

    public String getQuote_status() {
        return quote_status;
    }

    public String getQuote_rejection_reason() {
        return quote_rejection_reason;
    }

    public String getPayment_status() {
        return payment_status;
    }

    public String getPayment_reference() {
        return payment_reference;
    }
}