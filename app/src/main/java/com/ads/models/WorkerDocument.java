package com.ads.models;

import java.util.HashMap;
import java.util.Map;

public class WorkerDocument {

    // Tipos de documentos
    public static final String TYPE_HOJA_VIDA = "hoja_de_vida";
    public static final String TYPE_ANTECEDENTES = "antecedentes_judiciales";
    public static final String TYPE_TITULO = "titulo";
    public static final String TYPE_CARTA_RECOMENDACION = "carta_recomendacion";

    // Estados de documentos
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    // Categorías de certificaciones
    public static final String CATEGORY_HOJA_VIDA = "hojaDeVida";
    public static final String CATEGORY_ANTECEDENTES = "antecedentesJudiciales";
    public static final String CATEGORY_CERTIFICACIONES = "certificaciones";
    public static final String SUBCATEGORY_TITULOS = "titulos";
    public static final String SUBCATEGORY_CARTAS = "cartasRecomendacion";

    // Campos principales
    private String id;
    private String workerId;
    private String documentType;
    private String category;
    private String subcategory;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private long fileSize;
    private String status;
    private long uploadedAt;
    private long reviewedAt;
    private String reviewedBy;
    private String rejectionReason;
    private String description;
    private int orden; // Para cartas de recomendación (1, 2, 3)
    private String verificationUrl; // Para antecedentes judiciales

    // Constructores
    public WorkerDocument() {
        this.status = STATUS_PENDING;
        this.uploadedAt = System.currentTimeMillis();
        this.reviewedAt = 0;
        this.orden = 0;
    }

    public WorkerDocument(String id, String workerId, String documentType, String category) {
        this();
        this.id = id;
        this.workerId = workerId;
        this.documentType = documentType;
        this.category = category;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public long getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(long reviewedAt) {
        this.reviewedAt = reviewedAt;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public String getVerificationUrl() {
        return verificationUrl;
    }

    public void setVerificationUrl(String verificationUrl) {
        this.verificationUrl = verificationUrl;
    }

    // Métodos de utilidad

    /**
     * Verifica si el documento está pendiente de revisión
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /**
     * Verifica si el documento está aprobado
     */
    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    /**
     * Verifica si el documento está rechazado
     */
    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    /**
     * Verifica si el documento es una hoja de vida
     */
    public boolean isHojaVida() {
        return TYPE_HOJA_VIDA.equals(documentType);
    }

    /**
     * Verifica si el documento son antecedentes judiciales
     */
    public boolean isAntecedentes() {
        return TYPE_ANTECEDENTES.equals(documentType);
    }

    /**
     * Verifica si el documento es un título
     */
    public boolean isTitulo() {
        return TYPE_TITULO.equals(documentType);
    }

    /**
     * Verifica si el documento es una carta de recomendación
     */
    public boolean isCartaRecomendacion() {
        return TYPE_CARTA_RECOMENDACION.equals(documentType);
    }

    /**
     * Obtiene el tamaño del archivo formateado
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * Obtiene el estado formateado en español
     */
    public String getFormattedStatus() {
        switch (status) {
            case STATUS_PENDING:
                return "Pendiente de revisión";
            case STATUS_APPROVED:
                return "Aprobado";
            case STATUS_REJECTED:
                return "Rechazado";
            default:
                return "Desconocido";
        }
    }

    /**
     * Obtiene el tipo de documento formateado en español
     */
    public String getFormattedDocumentType() {
        switch (documentType) {
            case TYPE_HOJA_VIDA:
                return "Hoja de Vida";
            case TYPE_ANTECEDENTES:
                return "Antecedentes Judiciales";
            case TYPE_TITULO:
                return "Título Profesional";
            case TYPE_CARTA_RECOMENDACION:
                return "Carta de Recomendación #" + orden;
            default:
                return "Documento";
        }
    }

    /**
     * Verifica si el archivo es un PDF
     */
    public boolean isPDF() {
        return fileType != null && fileType.equals("application/pdf");
    }

    /**
     * Verifica si el archivo es una imagen
     */
    public boolean isImage() {
        return fileType != null && (
                fileType.equals("image/jpeg") ||
                        fileType.equals("image/jpg") ||
                        fileType.equals("image/png")
        );
    }

    /**
     * Verifica si el documento ha sido revisado
     */
    public boolean isReviewed() {
        return reviewedAt > 0;
    }

    /**
     * Obtiene la fecha de subida formateada
     */
    public String getFormattedUploadDate() {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(uploadedAt));
    }

    /**
     * Obtiene la fecha de revisión formateada
     */
    public String getFormattedReviewDate() {
        if (reviewedAt > 0) {
            return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date(reviewedAt));
        }
        return "No revisado";
    }

    /**
     * Valida que el documento tenga los campos obligatorios
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() &&
                workerId != null && !workerId.isEmpty() &&
                documentType != null && !documentType.isEmpty() &&
                category != null && !category.isEmpty() &&
                fileName != null && !fileName.isEmpty() &&
                fileUrl != null && !fileUrl.isEmpty() &&
                fileType != null && !fileType.isEmpty();
    }

    /**
     * Marca el documento como aprobado
     */
    public void approve(String reviewerId) {
        this.status = STATUS_APPROVED;
        this.reviewedAt = System.currentTimeMillis();
        this.reviewedBy = reviewerId;
        this.rejectionReason = null;
    }

    /**
     * Marca el documento como rechazado
     */
    public void reject(String reviewerId, String reason) {
        this.status = STATUS_REJECTED;
        this.reviewedAt = System.currentTimeMillis();
        this.reviewedBy = reviewerId;
        this.rejectionReason = reason;
    }

    /**
     * Reinicia el estado del documento a pendiente
     */
    public void resetToPending() {
        this.status = STATUS_PENDING;
        this.reviewedAt = 0;
        this.reviewedBy = null;
        this.rejectionReason = null;
    }

    /**
     * Convierte el objeto a un mapa para Firebase
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id);
        map.put("workerId", workerId);
        map.put("documentType", documentType);
        map.put("category", category);
        map.put("subcategory", subcategory);
        map.put("fileName", fileName);
        map.put("fileUrl", fileUrl);
        map.put("fileType", fileType);
        map.put("fileSize", fileSize);
        map.put("status", status);
        map.put("uploadedAt", uploadedAt);
        map.put("reviewedAt", reviewedAt);
        map.put("reviewedBy", reviewedBy);
        map.put("rejectionReason", rejectionReason);
        map.put("description", description);
        map.put("orden", orden);
        map.put("verificationUrl", verificationUrl);

        return map;
    }

    /**
     * Crea un WorkerDocument desde un DataSnapshot de Firebase
     */
    public static WorkerDocument fromMap(Map<String, Object> map) {
        WorkerDocument document = new WorkerDocument();

        if (map.containsKey("id")) document.setId((String) map.get("id"));
        if (map.containsKey("workerId")) document.setWorkerId((String) map.get("workerId"));
        if (map.containsKey("documentType")) document.setDocumentType((String) map.get("documentType"));
        if (map.containsKey("category")) document.setCategory((String) map.get("category"));
        if (map.containsKey("subcategory")) document.setSubcategory((String) map.get("subcategory"));
        if (map.containsKey("fileName")) document.setFileName((String) map.get("fileName"));
        if (map.containsKey("fileUrl")) document.setFileUrl((String) map.get("fileUrl"));
        if (map.containsKey("fileType")) document.setFileType((String) map.get("fileType"));
        if (map.containsKey("fileSize")) document.setFileSize(((Number) map.get("fileSize")).longValue());
        if (map.containsKey("status")) document.setStatus((String) map.get("status"));
        if (map.containsKey("uploadedAt")) document.setUploadedAt(((Number) map.get("uploadedAt")).longValue());
        if (map.containsKey("reviewedAt")) document.setReviewedAt(((Number) map.get("reviewedAt")).longValue());
        if (map.containsKey("reviewedBy")) document.setReviewedBy((String) map.get("reviewedBy"));
        if (map.containsKey("rejectionReason")) document.setRejectionReason((String) map.get("rejectionReason"));
        if (map.containsKey("description")) document.setDescription((String) map.get("description"));
        if (map.containsKey("orden")) document.setOrden(((Number) map.get("orden")).intValue());
        if (map.containsKey("verificationUrl")) document.setVerificationUrl((String) map.get("verificationUrl"));

        return document;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        WorkerDocument document = (WorkerDocument) obj;
        return id != null ? id.equals(document.id) : document.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "WorkerDocument{" +
                "id='" + id + '\'' +
                ", workerId='" + workerId + '\'' +
                ", documentType='" + documentType + '\'' +
                ", category='" + category + '\'' +
                ", fileName='" + fileName + '\'' +
                ", status='" + status + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", orden=" + orden +
                '}';
    }
}