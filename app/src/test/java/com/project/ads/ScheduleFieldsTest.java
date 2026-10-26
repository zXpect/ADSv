package com.project.ads;

import static org.junit.Assert.assertEquals;

import com.project.ads.helpers.ScheduleFields;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

/** El selector de fecha de Material trabaja con la medianoche UTC del día elegido (DEF-29). */
public class ScheduleFieldsTest {

    private static final TimeZone BOGOTA = TimeZone.getTimeZone("America/Bogota");

    @Test
    public void lateNightInColombiaKeepsTheLocalDay() {
        // 27/10 a las 23:30 en Colombia ya es 28/10 en UTC; el día elegido sigue siendo el 27
        Calendar local = Calendar.getInstance(BOGOTA);
        local.clear();
        local.set(2026, Calendar.OCTOBER, 27, 23, 30);

        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(ScheduleFields.utcMidnight(local));

        assertEquals(2026, utc.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, utc.get(Calendar.MONTH));
        assertEquals(27, utc.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, utc.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, utc.get(Calendar.MINUTE));
    }

    @Test
    public void earlyMorningKeepsTheLocalDay() {
        Calendar local = Calendar.getInstance(BOGOTA);
        local.clear();
        local.set(2026, Calendar.NOVEMBER, 1, 0, 5);

        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(ScheduleFields.utcMidnight(local));

        assertEquals(Calendar.NOVEMBER, utc.get(Calendar.MONTH));
        assertEquals(1, utc.get(Calendar.DAY_OF_MONTH));
    }
}
