package com.ads.activities.worker;

public class DocumentItem {
    private String type;
    private String status;
    private String url;
    private String rejectionReason;
    private long uploadedAt;

    public DocumentItem() {
    }

    public DocumentItem(String type, String status, String url, String rejectionReason, long uploadedAt) {
        this.type = type;
        this.status = status;
        this.url = url;
        this.rejectionReason = rejectionReason;
        this.uploadedAt = uploadedAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}