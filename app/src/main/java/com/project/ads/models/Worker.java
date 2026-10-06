package com.project.ads.models;

public class Worker {
    private String id;
    private String name;
    private String lastName;
    private String email;
    private String work;
    private String fcmToken;
    private boolean isAvailable;

    // Campos adicionales para el dashboard mejorado
    private String image;
    private String phone;
    private String description;
    private double latitude;
    private double longitude;

    private float rating;
    private int totalRatings;
    private double pricePerHour;
    private String experience;
    private long timestamp;
    private boolean isOnline;
    private float distance;

    // ⭐ NUEVO: Campo de verificación
    private VerificationStatus verificationStatus;

    // ⭐ CLASE INTERNA para el estado de verificación
    public static class VerificationStatus {
        private String status;
        private long timestamp;
        private String reviewedBy;
        private String rejectionReason;

        public VerificationStatus() {
            // Constructor vacío requerido por Firebase
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

        public String getReviewedBy() {
            return reviewedBy;
        }

        public void setReviewedBy(String reviewedBy) {
            this.reviewedBy = reviewedBy;
        }

        public String getRejectionReason() {
            return rejectionReason;
        }

        public void setRejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
        }
    }

    // Constructores
    public Worker() {
        this.isAvailable = true;
        this.isOnline = false;
        this.rating = 0.0f;
        this.totalRatings = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public Worker(String id, String name, String lastName, String email, String work) {
        this();
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.work = work;
    }

    public Worker(String id, String name, String lastName, String email, String work, String phone, String image) {
        this(id, name, lastName, email, work);
        this.phone = phone;
        this.image = image;
    }

    // ⭐ GETTER Y SETTER para verificationStatus
    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    // ⭐ MÉTODOS DE UTILIDAD para verificación
    /**
     * Verifica si el trabajador está verificado
     * @return true si el estado es "approved"
     */
    public boolean isVerified() {
        if (verificationStatus == null || verificationStatus.getStatus() == null) {
            return false;
        }
        String status = verificationStatus.getStatus().trim().toLowerCase();
        return status.equals("approved") || status.equals("verified");
    }

    /**
     * Obtiene el estado de verificación como string
     * @return Estado de verificación o "incomplete" si no existe
     */
    public String getVerificationStatusString() {
        if (verificationStatus == null || verificationStatus.getStatus() == null) {
            return "incomplete";
        }
        return verificationStatus.getStatus().trim().toLowerCase();
    }

    /**
     * Verifica si puede conectarse para trabajar
     * @return true si está verificado y puede trabajar
     */
    public boolean canWorkOnline() {
        return isVerified();
    }

    // Getters y Setters básicos (los que ya tenías)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public double getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    // Métodos de utilidad existentes
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();

        if (name != null && !name.trim().isEmpty()) {
            fullName.append(name.trim());
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }

        return fullName.length() > 0 ? fullName.toString() : "Usuario";
    }

    public boolean isFullyAvailable() {
        return isAvailable && isOnline;
    }

    public String getStatusText() {
        if (!isOnline) {
            return "Desconectado";
        } else if (!isAvailable) {
            return "Ocupado";
        } else {
            return "Disponible";
        }
    }

    public boolean hasImage() {
        return image != null && !image.trim().isEmpty() && !image.equals("default");
    }

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }

    public boolean hasValidLocation() {
        return latitude != 0.0 && longitude != 0.0;
    }

    public boolean hasRating() {
        return rating > 0 && totalRatings > 0;
    }

    public String getFormattedRating() {
        if (hasRating()) {
            return String.format("%.1f", rating);
        }
        return "0";
    }

    public String getFormattedPrice() {
        if (pricePerHour > 0) {
            return String.format("$%.0f/hora", pricePerHour);
        }
        return "Precio por consultar";
    }

    public void addRating(float newRating) {
        if (newRating >= 1 && newRating <= 5) {
            if (totalRatings == 0) {
                rating = newRating;
                totalRatings = 1;
            } else {
                float totalScore = rating * totalRatings;
                totalRatings++;
                rating = (totalScore + newRating) / totalRatings;
            }
        }
    }

    public void updateLastActivity() {
        this.timestamp = System.currentTimeMillis();
    }

    public boolean matchesSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        String searchQuery = query.toLowerCase().trim();

        if (name != null && name.toLowerCase().contains(searchQuery)) {
            return true;
        }

        if (lastName != null && lastName.toLowerCase().contains(searchQuery)) {
            return true;
        }

        if (getFullName().toLowerCase().contains(searchQuery)) {
            return true;
        }

        if (work != null && work.toLowerCase().contains(searchQuery)) {
            return true;
        }

        if (description != null && description.toLowerCase().contains(searchQuery)) {
            return true;
        }

        return false;
    }

    public boolean belongsToCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return true;
        }

        if (category.equalsIgnoreCase("Todos los servicios") ||
                category.equalsIgnoreCase("Todos")) {
            return true;
        }

        return work != null && work.equalsIgnoreCase(category.trim());
    }

    public double calculateDistanceFrom(double fromLat, double fromLng) {
        if (!hasValidLocation()) {
            return Double.MAX_VALUE;
        }

        final double R = 6371000;

        double latDistance = Math.toRadians(latitude - fromLat);
        double lngDistance = Math.toRadians(longitude - fromLng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public String getFormattedDistanceFrom(double fromLat, double fromLng) {
        double distanceMeters = calculateDistanceFrom(fromLat, fromLng);

        if (distanceMeters == Double.MAX_VALUE) {
            return "Ubicación no disponible";
        }

        if (distanceMeters < 1000) {
            return String.format("%.0f m", distanceMeters);
        } else {
            return String.format("%.1f km", distanceMeters / 1000);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Worker worker = (Worker) obj;
        return id != null ? id.equals(worker.id) : worker.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Worker{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", work='" + work + '\'' +
                ", isAvailable=" + isAvailable +
                ", isOnline=" + isOnline +
                ", rating=" + rating +
                ", totalRatings=" + totalRatings +
                ", verified=" + isVerified() +
                '}';
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();

        map.put("id", id);
        map.put("name", name);
        map.put("lastName", lastName);
        map.put("email", email);
        map.put("work", work);
        map.put("fcmToken", fcmToken);
        map.put("isAvailable", isAvailable);
        map.put("image", image);
        map.put("phone", phone);
        map.put("description", description);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("rating", rating);
        map.put("totalRatings", totalRatings);
        map.put("pricePerHour", pricePerHour);
        map.put("experience", experience);
        map.put("timestamp", timestamp);
        map.put("isOnline", isOnline);

        return map;
    }
}