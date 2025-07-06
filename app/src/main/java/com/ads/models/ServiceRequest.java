package com.ads.models;

import java.util.Map;

public class ServiceRequest {
    private String request_id;
    private String client_id;
    private String client_name;
    private String address;
    private String description;
    private String service_type;
    private String status;
    private long timestamp;
    private String client_phone;
    private String client_email;
    private double estimated_cost;
    private String urgency_level;

    public ServiceRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(ServiceRequest.class)
    }

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
    }

    // Constructor from Map (useful for Firebase data)
    public static ServiceRequest fromMap(Map<String, Object> map) {
        ServiceRequest request = new ServiceRequest();
        if (map != null) {
            request.setRequest_id((String) map.get("request_id"));
            request.setClient_id((String) map.get("client_id"));
            request.setClient_name((String) map.get("client_name"));
            request.setAddress((String) map.get("address"));
            request.setDescription((String) map.get("description"));
            request.setService_type((String) map.get("service_type"));
            request.setStatus((String) map.get("status"));
            request.setClient_phone((String) map.get("client_phone"));
            request.setClient_email((String) map.get("client_email"));
            
            Object timestamp = map.get("timestamp");
            if (timestamp instanceof Long) {
                request.setTimestamp((Long) timestamp);
            }
            
            Object cost = map.get("estimated_cost");
            if (cost instanceof Double) {
                request.setEstimated_cost((Double) cost);
            } else if (cost instanceof Long) {
                request.setEstimated_cost(((Long) cost).doubleValue());
            }
            
            request.setUrgency_level((String) map.get("urgency_level"));
        }
        return request;
    }

    // Getters and Setters
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

    public String getService_type() {
        return service_type;
    }

    public void setService_type(String service_type) {
        this.service_type = service_type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public double getEstimated_cost() {
        return estimated_cost;
    }

    public void setEstimated_cost(double estimated_cost) {
        this.estimated_cost = estimated_cost;
    }

    public String getUrgency_level() {
        return urgency_level;
    }

    public void setUrgency_level(String urgency_level) {
        this.urgency_level = urgency_level;
    }

    public String getStatusDisplayText() {
        switch (status != null ? status.toLowerCase() : "") {
            case "pending":
                return "Pendiente";
            case "accepted":
                return "Aceptado";
            case "in_progress":
                return "En Progreso";
            case "completed":
                return "Completado";
            case "rejected":
                return "Rechazado";
            default:
                return "Desconocido";
        }
    }

    public String getFormattedTimestamp() {
        if (timestamp > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(timestamp));
        }
        return "";
    }
}
