package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.project.ads.helpers.DocumentOrderHelper;
import com.project.ads.models.WorkerDocument;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DocumentOrderHelperTest {

    private static WorkerDocument doc(String subcategory, int orden, long uploadedAt, String hash) {
        WorkerDocument d = new WorkerDocument();
        d.setSubcategory(subcategory);
        d.setOrden(orden);
        d.setUploadedAt(uploadedAt);
        d.setFileHash(hash);
        return d;
    }

    @Test
    public void lettersAllNumberedOneAreNumberedByUploadDate() {
        WorkerDocument late = doc(WorkerDocument.SUBCATEGORY_CARTAS, 1, 300, null);
        WorkerDocument early = doc(WorkerDocument.SUBCATEGORY_CARTAS, 1, 100, null);
        WorkerDocument middle = doc(WorkerDocument.SUBCATEGORY_CARTAS, 1, 200, null);
        WorkerDocument title = doc(WorkerDocument.SUBCATEGORY_TITULOS, 1, 50, null);
        List<WorkerDocument> docs = new ArrayList<>(Arrays.asList(late, early, middle, title));

        DocumentOrderHelper.renumber(docs);

        assertEquals(1, early.getOrden());
        assertEquals(2, middle.getOrden());
        assertEquals(3, late.getOrden());
        assertEquals(1, title.getOrden());
    }

    @Test
    public void savedOrderWinsOverUploadDate() {
        // Una carta 1 reemplazada (subida después) sigue siendo la 1
        WorkerDocument replaced = doc(WorkerDocument.SUBCATEGORY_CARTAS, 1, 900, null);
        WorkerDocument second = doc(WorkerDocument.SUBCATEGORY_CARTAS, 2, 100, null);
        DocumentOrderHelper.renumber(Arrays.asList(second, replaced));
        assertEquals(1, replaced.getOrden());
        assertEquals(2, second.getOrden());
    }

    @Test
    public void nextOrderIsMaxPlusOneWithinSubcategory() {
        List<WorkerDocument> docs = Arrays.asList(
                doc(WorkerDocument.SUBCATEGORY_CARTAS, 1, 1, null),
                doc(WorkerDocument.SUBCATEGORY_CARTAS, 3, 2, null),
                doc(WorkerDocument.SUBCATEGORY_TITULOS, 5, 3, null));
        assertEquals(4, DocumentOrderHelper.nextOrder(docs, WorkerDocument.SUBCATEGORY_CARTAS));
        assertEquals(6, DocumentOrderHelper.nextOrder(docs, WorkerDocument.SUBCATEGORY_TITULOS));
        assertEquals(1, DocumentOrderHelper.nextOrder(new ArrayList<>(), WorkerDocument.SUBCATEGORY_CARTAS));
    }

    @Test
    public void sameFileIsFoundByHash() {
        WorkerDocument withHash = doc(WorkerDocument.SUBCATEGORY_CARTAS, 1, 1, "abc");
        List<WorkerDocument> docs = Arrays.asList(doc(WorkerDocument.SUBCATEGORY_CARTAS, 2, 2, null), withHash);
        assertSame(withHash, DocumentOrderHelper.findSameFile(docs, "abc"));
        assertNull(DocumentOrderHelper.findSameFile(docs, "otro"));
        assertNull(DocumentOrderHelper.findSameFile(docs, null));
    }
}
