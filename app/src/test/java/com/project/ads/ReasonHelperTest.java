package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.project.ads.helpers.ReasonHelper;

import org.junit.Test;

/** Motivos de ajuste (D5) y de cancelación por rol (D8); deben ser los códigos que acepta el backend. */
public class ReasonHelperTest {

    @Test
    public void cancellationReasonsDependOnRole() {
        assertSame(ReasonHelper.CANCELLATION_WORKER, ReasonHelper.cancellationReasons(ReasonHelper.ROLE_WORKER));
        assertSame(ReasonHelper.CANCELLATION_CLIENT, ReasonHelper.cancellationReasons(ReasonHelper.ROLE_CLIENT));
        assertSame(ReasonHelper.CANCELLATION_CLIENT, ReasonHelper.cancellationReasons(null));
    }

    @Test
    public void everyListOffersOther() {
        assertTrue(ReasonHelper.ADJUSTMENT.containsKey(ReasonHelper.OTHER));
        assertTrue(ReasonHelper.CANCELLATION_CLIENT.containsKey(ReasonHelper.OTHER));
        assertTrue(ReasonHelper.CANCELLATION_WORKER.containsKey(ReasonHelper.OTHER));
    }

    @Test
    public void labelUsesFreeTextOnlyForOther() {
        assertEquals("Revisar humedad", ReasonHelper.label(ReasonHelper.ADJUSTMENT, "otro", "Revisar humedad"));
        assertEquals("Otro", ReasonHelper.label(ReasonHelper.ADJUSTMENT, "otro", ""));
        assertEquals("Materiales", ReasonHelper.label(ReasonHelper.ADJUSTMENT, "materiales", "ignorado"));
        // Un código que la app aún no conoce se muestra tal cual
        assertEquals("codigo_nuevo", ReasonHelper.label(ReasonHelper.ADJUSTMENT, "codigo_nuevo", null));
        assertNull(ReasonHelper.label(ReasonHelper.ADJUSTMENT, null, "texto"));
    }

    @Test
    public void cancellationLabelSearchesBothRoles() {
        assertEquals("Por el precio", ReasonHelper.cancellationLabel("precio", null));
        assertEquals("Está fuera de mi zona", ReasonHelper.cancellationLabel("fuera_de_zona", null));
        assertEquals("Se mudó", ReasonHelper.cancellationLabel("otro", "Se mudó"));
        assertNull(ReasonHelper.cancellationLabel(null, null));
    }
}
