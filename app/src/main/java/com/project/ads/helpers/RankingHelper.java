package com.project.ads.helpers;

import com.project.ads.models.Worker;

import java.util.Comparator;

/**
 * Orden de la lista de técnicos (D13, HU-10): puntaje ponderado por cercanía y
 * calificación. La calificación es orientativa, no el criterio principal, así
 * que la cercanía pesa más.
 *
 *   puntaje = 0,6 · cercanía + 0,4 · calificación
 *   cercanía     = 1 − min(distancia, 10 km) / 10 km   (Ubaté cabe en ese radio)
 *   calificación = promedio bayesiano / 5, con 3 calificaciones «virtuales» de 3,0
 *
 * El promedio bayesiano da a un técnico nuevo (sin calificaciones) un puntaje
 * neutro de 3,0 y evita que una sola calificación de 5 lo ponga por encima de
 * uno con muchas buenas.
 */
public final class RankingHelper {

    static final double WEIGHT_DISTANCE = 0.6;
    static final double WEIGHT_RATING = 0.4;
    static final double MAX_DISTANCE_KM = 10.0;
    static final double PRIOR_RATING = 3.0;
    static final int PRIOR_COUNT = 3;

    /** Mayor puntaje primero; a igual puntaje, el más cercano. */
    public static final Comparator<Worker> BY_SCORE = (a, b) -> {
        int byScore = Double.compare(score(b.getDistance(), b.getRating(), b.getTotalRatings()),
                score(a.getDistance(), a.getRating(), a.getTotalRatings()));
        return byScore != 0 ? byScore : Float.compare(a.getDistance(), b.getDistance());
    };

    private RankingHelper() {
    }

    /** Promedio con PRIOR_COUNT calificaciones virtuales de PRIOR_RATING. */
    public static double smoothedRating(double rating, int totalRatings) {
        int count = Math.max(totalRatings, 0);
        return (rating * count + PRIOR_RATING * PRIOR_COUNT) / (count + PRIOR_COUNT);
    }

    /** Entre 0 y 1. distanceKm negativa o desconocida cuenta como cercana. */
    public static double score(double distanceKm, double rating, int totalRatings) {
        double distance = Math.max(0, Math.min(distanceKm, MAX_DISTANCE_KM));
        double proximity = 1 - distance / MAX_DISTANCE_KM;
        double quality = smoothedRating(rating, totalRatings) / 5.0;
        return WEIGHT_DISTANCE * proximity + WEIGHT_RATING * quality;
    }
}
