package com.project.ads;

import static org.junit.Assert.assertEquals;

import com.project.ads.models.Pqr;

import org.junit.Test;

/** Etiquetas de tipo y estado, y fechas de la lista de PQR (mockups 42 y 54). */
public class PqrTest {

    @Test
    public void typeLabel() {
        assertEquals(R.string.pqr_type_petition, Pqr.typeLabel("peticion"));
        assertEquals(R.string.pqr_type_complaint, Pqr.typeLabel("queja"));
        assertEquals(R.string.pqr_type_claim, Pqr.typeLabel("reclamo"));
    }

    @Test
    public void statusLabel() {
        assertEquals(R.string.pqr_status_open, Pqr.statusLabel("abierta"));
        assertEquals(R.string.pqr_status_in_progress, Pqr.statusLabel("en_tramite"));
        // Cerrar exige respuesta en el panel
        assertEquals(R.string.pqr_status_closed, Pqr.statusLabel("cerrada"));
    }

    @Test
    public void shortDate() {
        // La fecha ya viene en hora de Colombia: se toma la parte de la fecha
        assertEquals("14 oct", Pqr.shortDate("2026-10-14T23:50:12.123456-05:00"));
        assertEquals("4 nov", Pqr.shortDate("2026-11-04T08:00:00-05:00"));
        assertEquals("", Pqr.shortDate(null));
        assertEquals("", Pqr.shortDate("roto"));
    }
}
