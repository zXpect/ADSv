package com.project.ads.helpers;

import com.project.ads.models.WorkerDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Número de las cartas y los títulos del trabajador y archivos repetidos (DEF-23).
 * Antes todas las cartas quedaban con orden 1: se numeran por posición, ordenando
 * por el orden guardado y luego por la fecha de subida.
 */
public final class DocumentOrderHelper {

    private DocumentOrderHelper() {
    }

    /** Pone a cada carta y a cada título su número (1, 2, 3...) dentro de su subcategoría. */
    public static void renumber(List<WorkerDocument> documents) {
        renumberSubcategory(documents, WorkerDocument.SUBCATEGORY_CARTAS);
        renumberSubcategory(documents, WorkerDocument.SUBCATEGORY_TITULOS);
    }

    private static void renumberSubcategory(List<WorkerDocument> documents, String subcategory) {
        List<WorkerDocument> group = new ArrayList<>();
        for (WorkerDocument doc : documents) {
            if (subcategory.equals(doc.getSubcategory())) {
                group.add(doc);
            }
        }
        Collections.sort(group, (a, b) -> {
            if (a.getOrden() != b.getOrden()) {
                return Integer.compare(a.getOrden(), b.getOrden());
            }
            return Long.compare(a.getUploadedAt(), b.getUploadedAt());
        });
        for (int i = 0; i < group.size(); i++) {
            group.get(i).setOrden(i + 1);
        }
    }

    /** Número para un documento nuevo de la subcategoría: el mayor que haya más uno. */
    public static int nextOrder(List<WorkerDocument> documents, String subcategory) {
        int max = 0;
        for (WorkerDocument doc : documents) {
            if (subcategory != null && subcategory.equals(doc.getSubcategory())) {
                max = Math.max(max, doc.getOrden());
            }
        }
        return max + 1;
    }

    /** Documento ya subido con el mismo contenido (mismo SHA-256), o null. */
    public static WorkerDocument findSameFile(List<WorkerDocument> documents, String fileHash) {
        if (fileHash == null) {
            return null;
        }
        for (WorkerDocument doc : documents) {
            if (fileHash.equals(doc.getFileHash())) {
                return doc;
            }
        }
        return null;
    }

    /** Documento ya subido con el mismo nombre de archivo (sin distinguir mayúsculas), o null. */
    public static WorkerDocument findSameName(List<WorkerDocument> documents, String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return null;
        }
        String name = originalFileName.trim();
        for (WorkerDocument doc : documents) {
            if (doc.getOriginalFileName() != null && name.equalsIgnoreCase(doc.getOriginalFileName().trim())) {
                return doc;
            }
        }
        return null;
    }
}
