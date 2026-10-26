package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.project.ads.helpers.RankingHelper;

import org.junit.Test;

/** Ranking orientativo de la búsqueda (D13, HU-10). */
public class RankingHelperTest {

    private static final double EPS = 1e-9;

    @Test
    public void newWorkerGetsNeutralRating() {
        assertEquals(3.0, RankingHelper.smoothedRating(0, 0), EPS);
        // Una sola calificación de 5 apenas lo sube: (5 + 9) / 4
        assertEquals(3.5, RankingHelper.smoothedRating(5, 1), EPS);
    }

    @Test
    public void distanceWeighsMoreThanRating() {
        // A 0,5 km y nuevo, frente a 6 km con 4,9 en 40 calificaciones: gana el cercano
        double near = RankingHelper.score(0.5, 0, 0);
        double farGood = RankingHelper.score(6, 4.9, 40);
        assertTrue(near > farGood);
    }

    @Test
    public void ratingBreaksCloseDistances() {
        // Casi a la misma distancia, la mejor calificación va primero
        assertTrue(RankingHelper.score(1.2, 4.8, 20) > RankingHelper.score(1.0, 3.2, 20));
    }

    @Test
    public void distanceIsCappedAndNeverNegative() {
        assertEquals(RankingHelper.score(10, 3, 0), RankingHelper.score(25, 3, 0), EPS);
        assertEquals(RankingHelper.score(0, 3, 0), RankingHelper.score(-1, 3, 0), EPS);
    }
}
