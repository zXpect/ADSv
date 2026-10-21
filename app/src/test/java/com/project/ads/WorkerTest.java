package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.project.ads.helpers.CategoryHelper;
import com.project.ads.models.Worker;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Verificación (B9: la búsqueda solo muestra verificados) y oficios del trabajador (D16). */
public class WorkerTest {

    private static Worker withStatus(String status) {
        Worker worker = new Worker();
        Worker.VerificationStatus verification = new Worker.VerificationStatus();
        verification.setStatus(status);
        worker.setVerificationStatus(verification);
        return worker;
    }

    private static Worker withWork(String work) {
        return new Worker("w1", "Ana", "Ruiz", "ana@test.co", work);
    }

    @Test
    public void onlyApprovedWorkersAreVerified() {
        assertTrue(withStatus("approved").isVerified());
        assertTrue(withStatus(" Approved ").isVerified());
        assertTrue(withStatus("verified").isVerified());
        for (String status : new String[]{"pending", "documents_submitted", "rejected", "incomplete", ""}) {
            assertFalse(status, withStatus(status).isVerified());
            assertFalse(status, withStatus(status).canWorkOnline());
        }
        assertFalse(new Worker().isVerified());
        assertFalse(withStatus(null).isVerified());
    }

    @Test
    public void verificationStatusStringDefaultsToIncomplete() {
        assertEquals("incomplete", new Worker().getVerificationStatusString());
        assertEquals("rejected", withStatus(" REJECTED").getVerificationStatusString());
    }

    @Test
    public void categoriesWinOverLegacyWork() {
        Worker worker = withWork("Pintor");
        worker.setCategories(Arrays.asList("plomeria", "electricidad", "plomeria", null));
        assertEquals(Arrays.asList("plomeria", "electricidad"), worker.getCategorySlugs());
        assertFalse(worker.offersCategory("pintura"));
    }

    @Test
    public void legacyWorkIsTranslatedWhenThereAreNoCategories() {
        assertEquals(Collections.singletonList("plomeria"), withWork("Plomería ").getCategorySlugs());
        assertEquals(Collections.singletonList("plomeria"), withWork("Plomero").getCategorySlugs());
        assertEquals(Collections.singletonList("albanileria"), withWork("ALBAÑIL").getCategorySlugs());
        assertEquals(Collections.singletonList("electricidad"), withWork("Electricista").getCategorySlugs());

        Worker emptyCategories = withWork("Carpintero");
        emptyCategories.setCategories(Collections.<String>emptyList());
        assertEquals(Collections.singletonList("carpinteria"), emptyCategories.getCategorySlugs());
    }

    @Test
    public void unknownWorkHasNoCategory() {
        // Ferretería no es un oficio del catálogo: queda para revisión manual
        assertTrue(withWork("Ferretería").getCategorySlugs().isEmpty());
        assertTrue(withWork(null).getCategorySlugs().isEmpty());
        assertFalse(withWork("Ferretería").offersCategory(null));
    }

    @Test
    public void categoriesDisplay() {
        Worker worker = withWork("Plomero");
        worker.setCategories(Arrays.asList("plomeria", "pintura"));
        assertEquals("Plomería, Pintura", worker.getCategoriesDisplay());
        assertEquals("Plomería", withWork("Plomero").getCategoriesDisplay());
        // Sin oficio reconocido se muestra el texto original
        assertEquals("Ferretería", withWork("Ferretería").getCategoriesDisplay());
    }

    @Test
    public void categoryFilter() {
        Worker worker = withWork("Pintor");
        assertTrue(CategoryHelper.matches(worker, CategoryHelper.ALL));
        assertTrue(CategoryHelper.matches(worker, "pintura"));
        assertFalse(CategoryHelper.matches(worker, "plomeria"));
        assertFalse(CategoryHelper.matches(withWork("Ferretería"), "pintura"));
    }

    @Test
    public void ratingAverageIgnoresOutOfRangeValues() {
        Worker worker = new Worker();
        worker.addRating(5);
        worker.addRating(4);
        worker.addRating(0);
        worker.addRating(6);
        assertEquals(2, worker.getTotalRatings());
        assertEquals(4.5f, worker.getRating(), 0.001f);
        assertTrue(worker.hasRating());
        assertFalse(new Worker().hasRating());
    }

    @Test
    public void statusText() {
        Worker worker = new Worker();
        assertEquals("Desconectado", worker.getStatusText());
        worker.setOnline(true);
        assertEquals("Disponible", worker.getStatusText());
        assertTrue(worker.isFullyAvailable());
        worker.setAvailable(false);
        assertEquals("Ocupado", worker.getStatusText());
        assertFalse(worker.isFullyAvailable());
    }

    @Test
    public void fullName() {
        assertEquals("Ana Ruiz", withWork(null).getFullName());
        assertEquals("Usuario", new Worker().getFullName());
    }
}
