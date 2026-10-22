package com.project.ads;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import com.project.ads.helpers.SearchTimer;

import org.junit.Test;

/** Validez de la medición del tiempo de búsqueda (D22, HU-28). */
public class SearchTimerTest {

    private static final long T0 = 1_792_000_000_000L;

    @Test
    public void validPairKeepsBothMarks() {
        assertArrayEquals(new long[]{T0, T0 + 95_000}, SearchTimer.validPair(T0, T0 + 95_000));
        assertArrayEquals(new long[]{T0, T0}, SearchTimer.validPair(T0, T0));
    }

    @Test
    public void missingOrOutOfOrderMarksAreDropped() {
        assertNull(SearchTimer.validPair(0, T0));            // sin inicio (entró sin pasar por el inicio)
        assertNull(SearchTimer.validPair(T0, 0));            // sin «Solicitar»
        assertNull(SearchTimer.validPair(T0, T0 - 1));       // selección anterior a la búsqueda
    }

    @Test
    public void staleSearchIsDropped() {
        long thirteenHours = 13 * 60 * 60 * 1000L;
        assertNull(SearchTimer.validPair(T0, T0 + thirteenHours));
    }
}
