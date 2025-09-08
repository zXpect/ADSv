package com.ads.models;

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

    // Getters y Setters básicos
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

    // Getters y Setters adicionales
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

    // Métodos de utilidad

    /**
     * Obtiene el nombre completo del trabajador
     * @return Nombre completo (nombre + apellido)
     */
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

    /**
     * Verifica si el trabajador está completamente disponible
     * @return true si está disponible y en línea
     */
    public boolean isFullyAvailable() {
        return isAvailable && isOnline;
    }

    /**
     * Obtiene el estado del trabajador como string
     * @return Estado del trabajador
     */
    public String getStatusText() {
        if (!isOnline) {
            return "Desconectado";
        } else if (!isAvailable) {
            return "Ocupado";
        } else {
            return "Disponible";
        }
    }

    /**
     * Verifica si el trabajador tiene una imagen válida
     * @return true si tiene imagen
     */
    public boolean hasImage() {
        return image != null && !image.trim().isEmpty() && !image.equals("default");
    }

    /**
     * Verifica si el trabajador tiene una descripción
     * @return true si tiene descripción
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Verifica si el trabajador tiene un teléfono válido
     * @return true si tiene teléfono
     */
    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }

    /**
     * Verifica si el trabajador tiene ubicación válida
     * @return true si tiene coordenadas válidas
     */
    public boolean hasValidLocation() {
        return latitude != 0.0 && longitude != 0.0;
    }

    /**
     * Verifica si el trabajador tiene rating
     * @return true si tiene rating mayor a 0
     */
    public boolean hasRating() {
        return rating > 0 && totalRatings > 0;
    }

    /**
     * Obtiene el rating formateado como string
     * @return Rating formateado (ej: "4.5")
     */
    public String getFormattedRating() {
        if (hasRating()) {
            return String.format("%.1f", rating);
        }
        return "Sin calificar";
    }

    /**
     * Obtiene el precio formateado como string
     * @return Precio formateado
     */
    public String getFormattedPrice() {
        if (pricePerHour > 0) {
            return String.format("$%.0f/hora", pricePerHour);
        }
        return "Precio por consultar";
    }

    /**
     * Actualiza el rating con una nueva calificación
     * @param newRating Nueva calificación (1-5)
     */
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

    /**
     * Actualiza la timestamp de última actividad
     */
    public void updateLastActivity() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Verifica si el trabajador coincide con un filtro de búsqueda
     * @param query Texto de búsqueda
     * @return true si coincide con el filtro
     */
    public boolean matchesSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        String searchQuery = query.toLowerCase().trim();

        // Buscar en nombre
        if (name != null && name.toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Buscar en apellido
        if (lastName != null && lastName.toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Buscar en nombre completo
        if (getFullName().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Buscar en tipo de trabajo
        if (work != null && work.toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Buscar en descripción
        if (description != null && description.toLowerCase().contains(searchQuery)) {
            return true;
        }

        return false;
    }

    /**
     * Verifica si el trabajador pertenece a una categoría específica
     * @param category Categoría a verificar
     * @return true si pertenece a la categoría
     */
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

    /**
     * Calcula la distancia desde una ubicación específica
     * @param fromLat Latitud de origen
     * @param fromLng Longitud de origen
     * @return Distancia en metros
     */
    public double calculateDistanceFrom(double fromLat, double fromLng) {
        if (!hasValidLocation()) {
            return Double.MAX_VALUE;
        }

        final double R = 6371000; // Radio de la Tierra en metros

        double latDistance = Math.toRadians(latitude - fromLat);
        double lngDistance = Math.toRadians(longitude - fromLng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Obtiene la distancia formateada desde una ubicación
     * @param fromLat Latitud de origen
     * @param fromLng Longitud de origen
     * @return Distancia formateada (ej: "1.2 km")
     */
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

    // Métodos override para comparaciones

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
                '}';
    }

    /**
     * Convierte el objeto Worker a un mapa para Firebase
     * @return Mapa con los datos del trabajador
     */
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