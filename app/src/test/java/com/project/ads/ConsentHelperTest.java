package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.project.ads.helpers.ConsentHelper;
import com.project.ads.models.Worker;

import org.junit.Test;

import java.util.Map;

/** Forma de consent que exigen las reglas de Firebase (consent_rule, D18). */
public class ConsentHelperTest {

    @Test
    public void consentHasTheThreeFields() {
        Map<String, Object> consent = ConsentHelper.consent(1_792_000_000_000L);
        assertEquals(3, consent.size());
        assertTrue(consent.get("termsVersion") instanceof String);
        assertTrue(consent.get("privacyVersion") instanceof String);
        assertEquals(1_792_000_000_000L, consent.get("acceptedAt"));
    }

    @Test
    public void workerNodeCarriesConsentOnlyWhenAccepted() {
        Worker worker = new Worker("w1", "Ana", "Ruiz", "ana@test.co", "Plomero");
        assertNull(worker.toMap().get("consent"));

        worker.setConsentAcceptedAt(1_792_000_000_000L);
        @SuppressWarnings("unchecked")
        Map<String, Object> consent = (Map<String, Object>) worker.toMap().get("consent");
        assertEquals(1_792_000_000_000L, consent.get("acceptedAt"));
    }
}
