package com.project.ads.models.api;

public class RatingData {
    private String request_id;
    private boolean is_rated;
    private Integer client_rating;

    public String getRequestId() {
        return request_id;
    }

    public boolean isRated() {
        return is_rated;
    }

    public Integer getClientRating() {
        return client_rating;
    }
}
