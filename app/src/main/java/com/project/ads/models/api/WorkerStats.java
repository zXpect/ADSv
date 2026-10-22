package com.project.ads.models.api;

/**
 * Respuesta de GET /api/workers/<id>/stats/ (B12, HU-11). completed_jobs cuenta
 * las solicitudes completadas, con o sin calificación: no es totalRatings.
 */
public class WorkerStats {
    private String worker_id;
    private int completed_jobs;
    private double rating;
    private int total_ratings;

    public int getCompletedJobs() {
        return completed_jobs;
    }

    public double getRating() {
        return rating;
    }

    public int getTotalRatings() {
        return total_ratings;
    }
}
