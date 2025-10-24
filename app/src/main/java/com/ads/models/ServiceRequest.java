package com.ads.models;

import static com.ads.helpers.ServiceRequestHelper.*;

import java.util.Map;
import java.util.HashMap;

public class ServiceRequest {
    private String request_id;
    private String client_id;
    private String client_name;
    private String client_phone;
    private String client_email;
    private String client_image;

    private String worker_id;
    private String worker_name;
    private String worker_phone;
    private String worker_image;

    private String address;
    private String description;
    private String service_type;
    private String status; // pending, accepted, on_the_way, in_progress, completed, rejected, cancelled

    // Timestamps para cada etapa
    private long timestamp; // Cuando se creó la solicitud
    private long accepted_timestamp;
    private long on_the_way_timestamp;
    private long started_timestamp;
    private long completion_timestamp;

    // Información adicional
    private double estimated_cost;
    private double final_cost;
    private String urgency_level;

    // Rating y feedback
    private float client_rating; // Rating que el cliente da al trabajador
    private String client_comment;
    private boolean is_rated;

    // Razón de rechazo/cancelación
    private String rejection_reason;
    private String cancellation_reason;

    // Constructor vacío requerido por Firebase
    public ServiceRequest() {
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
        this.is_rated = false;
    }

    // Constructor básico
    public ServiceRequest(String request_id, String client_id, String client_name, String address,
                          String description, String service_type, String status, long timestamp) {
        this.request_id = request_id;
        this.client_id = client_id;
        this.client_name = client_name;
        this.address = address;
        this.description = description;
        this.service_type = service_type;
        this.status = status;
        this.timestamp = timestamp;
        this.is_rated = false;
    }

    // Constructor desde Map (Firebase)
    public static ServiceRequest fromMap(Map<String, Object> map) {
        ServiceRequest request = new ServiceRequest();
        if (map != null) {
            // IDs
            String requestId = (String) map.get("request_id");
            if (requestId == null) requestId = (String) map.get("id");
            request.setRequest_id(requestId);

            // Cliente
            request.setClient_id((String) map.get("client_id"));
            request.setClient_name((String) map.get("client_name"));
            request.setClient_phone((String) map.get("client_phone"));
            request.setClient_email((String) map.get("client_email"));
            request.setClient_image((String) map.get("client_image"));

            // Trabajador
            request.setWorker_id((String) map.get("worker_id"));
            request.setWorker_name((String) map.get("worker_name"));
            request.setWorker_phone((String) map.get("worker_phone"));
            request.setWorker_image((String) map.get("worker_image"));

            // Detalles del servicio
            request.setAddress((String) map.get("address"));
            request.setDescription((String) map.get("description"));
            request.setService_type((String) map.get("service_type"));
            request.setStatus((String) map.get("status"));

            // Timestamps
            request.setTimestamp(getLongValue(map, "timestamp"));
            request.setAccepted_timestamp(getLongValue(map, "accepted_timestamp"));
            request.setOn_the_way_timestamp(getLongValue(map, "on_the_way_timestamp"));
            request.setStarted_timestamp(getLongValue(map, "started_timestamp"));
            request.setCompletion_timestamp(getLongValue(map, "completion_timestamp"));

            // Costos
            request.setEstimated_cost(getDoubleValue(map, "estimated_cost"));
            request.setFinal_cost(getDoubleValue(map, "final_cost"));
            request.setUrgency_level((String) map.get("urgency_level"));

            // Rating
            request.setClient_rating(getFloatValue(map, "client_rating"));
            request.setClient_comment((String) map.get("client_comment"));
            request.setIs_rated(getBooleanValue(map, "is_rated"));

            // Razones
            request.setRejection_reason((String) map.get("rejection_reason"));
            request.setCancellation_reason((String) map.get("cancellation_reason"));
        }
        return request;
    }



    // Convertir a Map para Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        // IDs
        map.put("request_id", request_id);
        map.put("id", request_id);

        // Cliente
        map.put("client_id", client_id);
        map.put("client_name", client_name);
        map.put("client_phone", client_phone);
        map.put("client_email", client_email);
        map.put("client_image", client_image);

        // Trabajador
        map.put("worker_id", worker_id);
        map.put("worker_name", worker_name);
        map.put("worker_phone", worker_phone);
        map.put("worker_image", worker_image);

        // Detalles
        map.put("address", address);
        map.put("description", description);
        map.put("service_type", service_type);
        map.put("status", status);

        // Timestamps
        map.put("timestamp", timestamp);
        map.put("accepted_timestamp", accepted_timestamp);
        map.put("on_the_way_timestamp", on_the_way_timestamp);
        map.put("started_timestamp", started_timestamp);
        map.put("completion_timestamp", completion_timestamp);

        // Costos
        map.put("estimated_cost", estimated_cost);
        map.put("final_cost", final_cost);
        map.put("urgency_level", urgency_level);

        // Rating
        map.put("client_rating", client_rating);
        map.put("client_comment", client_comment);
        map.put("is_rated", is_rated);

        // Razones
        map.put("rejection_reason", rejection_reason);
        map.put("cancellation_reason", cancellation_reason);

        return map;
    }

    // GETTERS Y SETTERS

    public String getRequest_id() { return request_id; }
    public void setRequest_id(String request_id) { this.request_id = request_id; }

    public String getClient_id() { return client_id; }
    public void setClient_id(String client_id) { this.client_id = client_id; }

    public String getClient_name() { return client_name; }
    public void setClient_name(String client_name) { this.client_name = client_name; }

    public String getClient_phone() { return client_phone; }
    public void setClient_phone(String client_phone) { this.client_phone = client_phone; }

    public String getClient_email() { return client_email; }
    public void setClient_email(String client_email) { this.client_email = client_email; }

    public String getClient_image() { return client_image; }
    public void setClient_image(String client_image) { this.client_image = client_image; }

    public String getWorker_id() { return worker_id; }
    public void setWorker_id(String worker_id) { this.worker_id = worker_id; }

    public String getWorker_name() { return worker_name; }
    public void setWorker_name(String worker_name) { this.worker_name = worker_name; }

    public String getWorker_phone() { return worker_phone; }
    public void setWorker_phone(String worker_phone) { this.worker_phone = worker_phone; }

    public String getWorker_image() { return worker_image; }
    public void setWorker_image(String worker_image) { this.worker_image = worker_image; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getService_type() { return service_type; }
    public void setService_type(String service_type) { this.service_type = service_type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getAccepted_timestamp() { return accepted_timestamp; }
    public void setAccepted_timestamp(long accepted_timestamp) { this.accepted_timestamp = accepted_timestamp; }

    public long getOn_the_way_timestamp() { return on_the_way_timestamp; }
    public void setOn_the_way_timestamp(long on_the_way_timestamp) { this.on_the_way_timestamp = on_the_way_timestamp; }

    public long getStarted_timestamp() { return started_timestamp; }
    public void setStarted_timestamp(long started_timestamp) { this.started_timestamp = started_timestamp; }

    public long getCompletion_timestamp() { return completion_timestamp; }
    public void setCompletion_timestamp(long completion_timestamp) { this.completion_timestamp = completion_timestamp; }

    public double getEstimated_cost() { return estimated_cost; }
    public void setEstimated_cost(double estimated_cost) { this.estimated_cost = estimated_cost; }

    public double getFinal_cost() { return final_cost; }
    public void setFinal_cost(double final_cost) { this.final_cost = final_cost; }

    public String getUrgency_level() { return urgency_level; }
    public void setUrgency_level(String urgency_level) { this.urgency_level = urgency_level; }

    public float getClient_rating() { return client_rating; }
    public void setClient_rating(float client_rating) { this.client_rating = client_rating; }

    public String getClient_comment() { return client_comment; }
    public void setClient_comment(String client_comment) { this.client_comment = client_comment; }

    public boolean isIs_rated() { return is_rated; }
    public void setIs_rated(boolean is_rated) { this.is_rated = is_rated; }

    public String getRejection_reason() { return rejection_reason; }
    public void setRejection_reason(String rejection_reason) { this.rejection_reason = rejection_reason; }

    public String getCancellation_reason() { return cancellation_reason; }
    public void setCancellation_reason(String cancellation_reason) { this.cancellation_reason = cancellation_reason; }

    // MÉTODOS DE UTILIDAD

    /**
     * Obtiene el texto del estado en español
     */
    public String getStatusDisplayText() {
        if (status == null) return "Desconocido";

        switch (status.toLowerCase()) {
            case "pending":
                return "Pendiente";
            case "accepted":
                return "Aceptado";
            case "on_the_way":
                return "En camino";
            case "in_progress":
                return "En progreso";
            case "completed":
                return "Completado";
            case "rejected":
                return "Rechazado";
            case "cancelled":
                return "Cancelado";
            default:
                return "Desconocido";
        }
    }

    /**
     * Obtiene el color del estado
     */
    public String getStatusColor() {
        if (status == null) return "#9E9E9E";

        switch (status.toLowerCase()) {
            case "pending":
                return "#FF9800"; // Naranja
            case "accepted":
                return "#2196F3"; // Azul
            case "on_the_way":
                return "#03A9F4"; // Azul claro
            case "in_progress":
                return "#9C27B0"; // Púrpura
            case "completed":
                return "#4CAF50"; // Verde
            case "rejected":
                return "#F44336"; // Rojo
            case "cancelled":
                return "#757575"; // Gris
            default:
                return "#9E9E9E";
        }
    }

    /**
     * Formatea el timestamp principal
     */
    public String getFormattedTimestamp() {
        if (timestamp > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    java.util.Locale.getDefault()
            );
            return sdf.format(new java.util.Date(timestamp));
        }
        return "";
    }

    /**
     * Obtiene el tiempo transcurrido desde la creación
     */
    public String getTimeAgo() {
        if (timestamp == 0) return "Desconocido";

        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + (days == 1 ? " día" : " días");
        if (hours > 0) return hours + (hours == 1 ? " hora" : " horas");
        if (minutes > 0) return minutes + (minutes == 1 ? " minuto" : " minutos");
        return "Hace un momento";
    }

    /**
     * Verifica si la solicitud puede ser cancelada
     */
    public boolean canBeCancelled() {
        if (status == null) return false;
        return status.equalsIgnoreCase("pending") || status.equalsIgnoreCase("accepted");
    }

    /**
     * Verifica si la solicitud puede ser calificada
     */
    public boolean canBeRated() {
        if (status == null || is_rated) return false;
        return status.equalsIgnoreCase("completed");
    }

    /**
     * Verifica si el trabajador está asignado
     */
    public boolean hasWorkerAssigned() {
        return worker_id != null && !worker_id.trim().isEmpty();
    }

    /**
     * Obtiene el progreso de la solicitud (0-100)
     */
    public int getProgress() {
        if (status == null) return 0;

        switch (status.toLowerCase()) {
            case "pending": return 20;
            case "accepted": return 40;
            case "on_the_way": return 60;
            case "in_progress": return 80;
            case "completed": return 100;
            case "rejected":
            case "cancelled": return 0;
            default: return 0;
        }
    }

    /**
     * Obtiene el número de etapa actual (1-5)
     */
    public int getCurrentStage() {
        if (status == null) return 0;

        switch (status.toLowerCase()) {
            case "pending": return 1;
            case "accepted": return 2;
            case "on_the_way": return 3;
            case "in_progress": return 4;
            case "completed": return 5;
            default: return 0;
        }
    }

    /**
     * Verifica si la solicitud está activa
     */
    public boolean isActive() {
        if (status == null) return false;
        String s = status.toLowerCase();
        return s.equals("pending") || s.equals("accepted") ||
                s.equals("on_the_way") || s.equals("in_progress");
    }

    /**
     * Verifica si la solicitud está finalizada
     */
    public boolean isFinished() {
        if (status == null) return false;
        String s = status.toLowerCase();
        return s.equals("completed") || s.equals("rejected") || s.equals("cancelled");
    }
}