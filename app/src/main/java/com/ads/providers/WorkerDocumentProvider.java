package com.ads.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ads.models.WorkerDocument;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public class WorkerDocumentProvider {

    private final DatabaseReference mDatabase;
    private final StorageReference mStorage;

    // Rutas base
    private static final String DOCUMENTS_NODE = "WorkerDocuments";
    private static final String STORAGE_PATH = "worker_documents";

    public WorkerDocumentProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child(DOCUMENTS_NODE);
        mStorage = FirebaseStorage.getInstance().getReference().child(STORAGE_PATH);
    }

    // ==================== MÉTODOS DE CREACIÓN Y SUBIDA ====================

    /**
     * Sube un archivo a Firebase Storage
     * @param workerId ID del trabajador
     * @param category Categoría del documento (hojaDeVida, antecedentesJudiciales, certificaciones)
     * @param subcategory Subcategoría (titulos, cartasRecomendacion) - puede ser null
     * @param fileUri URI del archivo local
     * @param fileName Nombre del archivo
     * @return UploadTask para monitorear el progreso
     */
    public UploadTask uploadFile(String workerId, String category, String subcategory, Uri fileUri, String fileName) {
        StorageReference fileRef;

        if (subcategory != null && !subcategory.isEmpty()) {
            // Para certificaciones con subcategoría
            fileRef = mStorage.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(fileName);
        } else {
            // Para hoja de vida y antecedentes
            fileRef = mStorage.child(workerId)
                    .child(category)
                    .child(fileName);
        }

        return fileRef.putFile(fileUri);
    }

    /**
     * Crea el registro del documento en Realtime Database
     * @param document Documento a crear
     * @return Task con el resultado
     */
    public Task<Void> createDocument(WorkerDocument document) {
        String workerId = document.getWorkerId();
        String category = document.getCategory();
        String documentId = document.getId();

        DatabaseReference docRef;

        // Determinar la ruta según el tipo de documento
        if (category.equals(WorkerDocument.CATEGORY_CERTIFICACIONES)) {
            String subcategory = document.isTitulo() ?
                    WorkerDocument.SUBCATEGORY_TITULOS :
                    WorkerDocument.SUBCATEGORY_CARTAS;

            docRef = mDatabase.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(documentId);
        } else {
            // Para hoja de vida y antecedentes (nodos únicos)
            docRef = mDatabase.child(workerId)
                    .child(category);
        }

        return docRef.setValue(document.toMap());
    }

    /**
     * Sube un documento completo (archivo + metadatos)
     * @param document Documento con metadatos
     * @param fileUri URI del archivo local
     * @return Task con el resultado
     */
    public Task<Void> uploadDocument(WorkerDocument document, Uri fileUri) {
        // Primero subir el archivo a Storage
        String subcategory = null;
        if (document.getCategory().equals(WorkerDocument.CATEGORY_CERTIFICACIONES)) {
            subcategory = document.isTitulo() ?
                    WorkerDocument.SUBCATEGORY_TITULOS :
                    WorkerDocument.SUBCATEGORY_CARTAS;
        }

        return uploadFile(
                document.getWorkerId(),
                document.getCategory(),
                subcategory,
                fileUri,
                document.getFileName()
        ).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            // Obtener la URL de descarga
            return task.getResult().getStorage().getDownloadUrl();
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            // Guardar la URL en el documento
            document.setFileUrl(task.getResult().toString());
            // Crear el registro en Database
            return createDocument(document);
        });
    }

    // ==================== MÉTODOS DE CONSULTA ====================

    /**
     * Obtiene todos los documentos de un trabajador
     */
    public Task<DataSnapshot> getWorkerDocuments(String workerId) {
        return mDatabase.child(workerId).get();
    }

    /**
     * Obtiene la referencia a los documentos de un trabajador para listeners en tiempo real
     */
    public DatabaseReference getWorkerDocumentsReference(String workerId) {
        return mDatabase.child(workerId);
    }

    /**
     * Obtiene la hoja de vida de un trabajador
     */
    public Task<DataSnapshot> getHojaVida(String workerId) {
        return mDatabase.child(workerId)
                .child(WorkerDocument.CATEGORY_HOJA_VIDA)
                .get();
    }

    /**
     * Obtiene los antecedentes judiciales de un trabajador
     */
    public Task<DataSnapshot> getAntecedentes(String workerId) {
        return mDatabase.child(workerId)
                .child(WorkerDocument.CATEGORY_ANTECEDENTES)
                .get();
    }

    /**
     * Obtiene todos los títulos de un trabajador
     */
    public Task<DataSnapshot> getTitulos(String workerId) {
        return mDatabase.child(workerId)
                .child(WorkerDocument.CATEGORY_CERTIFICACIONES)
                .child(WorkerDocument.SUBCATEGORY_TITULOS)
                .get();
    }

    /**
     * Obtiene todas las cartas de recomendación de un trabajador
     */
    public Task<DataSnapshot> getCartasRecomendacion(String workerId) {
        return mDatabase.child(workerId)
                .child(WorkerDocument.CATEGORY_CERTIFICACIONES)
                .child(WorkerDocument.SUBCATEGORY_CARTAS)
                .get();
    }

    /**
     * Obtiene un documento específico
     */
    public Task<DataSnapshot> getDocument(String workerId, String category, String subcategory, String documentId) {
        if (subcategory != null && !subcategory.isEmpty()) {
            return mDatabase.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(documentId)
                    .get();
        } else {
            return mDatabase.child(workerId)
                    .child(category)
                    .get();
        }
    }

    /**
     * Cuenta cuántas cartas de recomendación tiene un trabajador
     */
    public Task<DataSnapshot> countCartasRecomendacion(String workerId) {
        return getCartasRecomendacion(workerId);
    }

    /**
     * Verifica si un trabajador tiene todos los documentos obligatorios
     */
    public Task<Boolean> hasAllRequiredDocuments(String workerId) {
        return getWorkerDocuments(workerId).continueWith(task -> {
            if (!task.isSuccessful() || !task.getResult().exists()) {
                return false;
            }

            DataSnapshot snapshot = task.getResult();

            // Verificar hoja de vida
            boolean hasHojaVida = snapshot.child(WorkerDocument.CATEGORY_HOJA_VIDA).exists();

            // Verificar antecedentes
            boolean hasAntecedentes = snapshot.child(WorkerDocument.CATEGORY_ANTECEDENTES).exists();

            // Verificar mínimo 3 cartas de recomendación
            DataSnapshot cartas = snapshot.child(WorkerDocument.CATEGORY_CERTIFICACIONES)
                    .child(WorkerDocument.SUBCATEGORY_CARTAS);
            long cartasCount = cartas.getChildrenCount();

            return hasHojaVida && hasAntecedentes && cartasCount >= 3;
        });
    }

    // ==================== MÉTODOS DE ACTUALIZACIÓN ====================

    /**
     * Actualiza el estado de un documento
     */
    public Task<Void> updateDocumentStatus(String workerId, String category, String subcategory,
                                           String documentId, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("reviewedAt", System.currentTimeMillis());

        DatabaseReference docRef;
        if (subcategory != null && !subcategory.isEmpty()) {
            docRef = mDatabase.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(documentId);
        } else {
            docRef = mDatabase.child(workerId).child(category);
        }

        return docRef.updateChildren(updates);
    }

    /**
     * Aprueba un documento
     */
    public Task<Void> approveDocument(String workerId, String category, String subcategory,
                                      String documentId, String reviewerId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", WorkerDocument.STATUS_APPROVED);
        updates.put("reviewedAt", System.currentTimeMillis());
        updates.put("reviewedBy", reviewerId);
        updates.put("rejectionReason", null);

        DatabaseReference docRef;
        if (subcategory != null && !subcategory.isEmpty()) {
            docRef = mDatabase.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(documentId);
        } else {
            docRef = mDatabase.child(workerId).child(category);
        }

        return docRef.updateChildren(updates);
    }

    /**
     * Rechaza un documento
     */
    public Task<Void> rejectDocument(String workerId, String category, String subcategory,
                                     String documentId, String reviewerId, String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", WorkerDocument.STATUS_REJECTED);
        updates.put("reviewedAt", System.currentTimeMillis());
        updates.put("reviewedBy", reviewerId);
        updates.put("rejectionReason", reason);

        DatabaseReference docRef;
        if (subcategory != null && !subcategory.isEmpty()) {
            docRef = mDatabase.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(documentId);
        } else {
            docRef = mDatabase.child(workerId).child(category);
        }

        return docRef.updateChildren(updates);
    }

    /**
     * Actualiza campos específicos de un documento
     */
    public Task<Void> updateDocument(String workerId, String category, String subcategory,
                                     String documentId, Map<String, Object> updates) {
        DatabaseReference docRef;
        if (subcategory != null && !subcategory.isEmpty()) {
            docRef = mDatabase.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(documentId);
        } else {
            docRef = mDatabase.child(workerId).child(category);
        }

        return docRef.updateChildren(updates);
    }

    // ==================== MÉTODOS DE ELIMINACIÓN ====================

    /**
     * Elimina un documento (archivo + metadatos)
     */
    public Task<Void> deleteDocument(String workerId, String category, String subcategory,
                                     String documentId, String fileName) {
        // Primero eliminar el archivo de Storage
        StorageReference fileRef;
        if (subcategory != null && !subcategory.isEmpty()) {
            fileRef = mStorage.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(fileName);
        } else {
            fileRef = mStorage.child(workerId)
                    .child(category)
                    .child(fileName);
        }

        return fileRef.delete().continueWithTask(task -> {
            // Luego eliminar el registro de Database
            DatabaseReference docRef;
            if (subcategory != null && !subcategory.isEmpty()) {
                docRef = mDatabase.child(workerId)
                        .child(category)
                        .child(subcategory)
                        .child(documentId);
            } else {
                docRef = mDatabase.child(workerId).child(category);
            }

            return docRef.removeValue();
        });
    }

    /**
     * Elimina todos los documentos de un trabajador
     */
    public Task<Void> deleteAllWorkerDocuments(String workerId) {
        // Eliminar de Database
        return mDatabase.child(workerId).removeValue().continueWithTask(task -> {
            // Eliminar de Storage
            StorageReference workerStorage = mStorage.child(workerId);
            return deleteStorageFolder(workerStorage);
        });
    }

    /**
     * Método auxiliar para eliminar carpetas de Storage recursivamente
     */
    private Task<Void> deleteStorageFolder(StorageReference folderRef) {
        return folderRef.listAll().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            // Eliminar todos los archivos
            for (StorageReference item : task.getResult().getItems()) {
                item.delete();
            }

            // Eliminar subcarpetas recursivamente
            for (StorageReference prefix : task.getResult().getPrefixes()) {
                deleteStorageFolder(prefix);
            }

            return null;
        });
    }

    // ==================== MÉTODOS PARA ADMIN ====================

    /**
     * Obtiene todos los documentos pendientes de revisión
     */
    public Task<DataSnapshot> getAllPendingDocuments() {
        Query pendingQuery = mDatabase.orderByChild("status")
                .equalTo(WorkerDocument.STATUS_PENDING);
        return pendingQuery.get();
    }

    /**
     * Obtiene todos los trabajadores con documentos pendientes
     */
    public Task<DataSnapshot> getWorkersWithPendingDocuments() {
        return mDatabase.get();
    }

    /**
     * Obtener referencia a los documentos de un trabajador
     */
    public DatabaseReference getWorkerDocumentsRef(String workerId) {
        return mDatabase.child(workerId).child("documents");
    }

    /**
     * Obtener todos los documentos de un trabajador (Query)
     */


    // ==================== LISTENERS TIEMPO REAL ====================

    /**
     * Escucha cambios en los documentos de un trabajador
     */
    public void listenToWorkerDocuments(String workerId, ValueEventListener listener) {
        mDatabase.child(workerId).addValueEventListener(listener);
    }

    /**
     * Escucha cambios en un documento específico
     */
    public void listenToDocument(String workerId, String category, String subcategory,
                                 String documentId, ValueEventListener listener) {
        DatabaseReference docRef;
        if (subcategory != null && !subcategory.isEmpty()) {
            docRef = mDatabase.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(documentId);
        } else {
            docRef = mDatabase.child(workerId).child(category);
        }

        docRef.addValueEventListener(listener);
    }

    /**
     * Remueve un listener
     */
    public void removeListener(String workerId, ValueEventListener listener) {
        mDatabase.child(workerId).removeEventListener(listener);
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Genera un ID único para un documento
     */
    public String generateDocumentId() {
        return mDatabase.push().getKey();
    }

    /**
     * Genera un nombre de archivo único
     */
    public String generateFileName(String workerId, String documentType, String extension) {
        long timestamp = System.currentTimeMillis();
        return documentType + "_" + workerId + "_" + timestamp + "." + extension;
    }

    /**
     * Obtiene la referencia de Storage para un archivo
     */
    public StorageReference getFileReference(String workerId, String category,
                                             String subcategory, String fileName) {
        if (subcategory != null && !subcategory.isEmpty()) {
            return mStorage.child(workerId)
                    .child(category)
                    .child(subcategory)
                    .child(fileName);
        } else {
            return mStorage.child(workerId)
                    .child(category)
                    .child(fileName);
        }
    }

    /**
     * Descarga la URL de un archivo
     */
    public Task<Uri> getFileDownloadUrl(String workerId, String category,
                                        String subcategory, String fileName) {
        return getFileReference(workerId, category, subcategory, fileName)
                .getDownloadUrl();
    }

    /**
     * Actualiza el estado de verificación en el perfil del trabajador
     */
    public Task<Void> updateWorkerVerificationStatus(String workerId, String status) {
        DatabaseReference workerRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("User")
                .child("Trabajadores")
                .child(workerId)
                .child("verificationStatus");

        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("status", status);
        verificationData.put("submittedAt", System.currentTimeMillis());

        return workerRef.setValue(verificationData);
    }

    /**
     * Obtiene el estado de verificación de un trabajador
     */
    public Task<DataSnapshot> getWorkerVerificationStatus(String workerId) {
        return FirebaseDatabase.getInstance()
                .getReference()
                .child("User")
                .child("Trabajadores")
                .child(workerId)
                .child("verificationStatus")
                .get();
    }

    // Getters
    public DatabaseReference getDatabase() {
        return mDatabase;
    }

    public StorageReference getStorage() {
        return mStorage;
    }
}