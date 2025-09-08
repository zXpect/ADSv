package com.ads.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ads.models.Worker;

import java.util.HashMap;
import java.util.Map;

public class WorkerProvider {
    private final DatabaseReference mDataBase;

    public WorkerProvider() {
        mDataBase = FirebaseDatabase.getInstance().getReference().child("User").child("Trabajadores");
    }

    // Getter para mDatabase
    public DatabaseReference getmDatabase() {
        return mDataBase;
    }

    // Getter para workers
    public DatabaseReference getWorkers() {
        return mDataBase;
    }

    // MÉTODOS DE CREACIÓN Y ACTUALIZACIÓN

    public Task<Void> create(Worker worker) {
        return mDataBase.child(worker.getId()).setValue(worker.toMap());
    }
    public Task<Void> createBasic(Worker worker) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", worker.getName());
        map.put("lastName", worker.getLastName());
        map.put("email", worker.getEmail());
        map.put("work", worker.getWork());
        map.put("isAvailable", worker.isAvailable());
        map.put("timestamp", System.currentTimeMillis());

        return mDataBase.child(worker.getId()).setValue(map);
    }
    public Task<Void> updateWorkerProfile(String workerId, Worker worker) {
        worker.updateLastActivity();
        return mDataBase.child(workerId).setValue(worker.toMap());
    }

    // MÉTODOS DE CONSULTA

    public Task<DataSnapshot> getWorker(String id) {
        return mDataBase.child(id).get();
    }

    public Task<DataSnapshot> getAvailableWorkers() {
        Query availableWorkersQuery = mDataBase.orderByChild("isAvailable").equalTo(true);
        return availableWorkersQuery.get();
    }

    public Task<DataSnapshot> getFullyAvailableWorkers() {
        return mDataBase.get();
    }
    public Task<DataSnapshot> getWorkersByCategory(String workCategory) {
        Query categoryQuery = mDataBase.orderByChild("work").equalTo(workCategory);
        return categoryQuery.get();
    }
    public Task<DataSnapshot> getWorkersByRating() {
        Query ratingQuery = mDataBase.orderByChild("rating").limitToLast(50);
        return ratingQuery.get();
    }
    public Task<DataSnapshot> getOnlineWorkers() {
        Query onlineQuery = mDataBase.orderByChild("isOnline").equalTo(true);
        return onlineQuery.get();
    }

    // MÉTODOS DE ACTUALIZACIÓN DE CAMPOS ESPECÍFICOS
    public Task<Void> updateWorkerFCMToken(String workerId, String fcmToken) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", fcmToken);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }
    public Task<DataSnapshot> getWorkerFCMToken(String workerId) {
        return mDataBase.child(workerId).child("fcmToken").get();
    }

    public Task<Void> updateWorkerAvailability(String workerId, boolean isAvailable) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAvailable", isAvailable);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerOnlineStatus(String workerId, boolean isOnline) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOnline", isOnline);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerLocation(String workerId, double latitude, double longitude) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerImage(String workerId, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("image", imageUrl);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerDescription(String workerId, String description) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", description);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerPhone(String workerId, String phone) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", phone);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerPrice(String workerId, double pricePerHour) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("pricePerHour", pricePerHour);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerExperience(String workerId, String experience) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("experience", experience);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerRating(String workerId, float rating, int totalRatings) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", rating);
        updates.put("totalRatings", totalRatings);
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    public Task<Void> updateWorkerFields(String workerId, Map<String, Object> updates) {
        // Agregar timestamp automáticamente
        updates.put("timestamp", System.currentTimeMillis());
        return mDataBase.child(workerId).updateChildren(updates);
    }

    // MÉTODOS DE BÚSQUEDA Y FILTRADO

    public Task<DataSnapshot> getWorkersByPriceRange(double minPrice, double maxPrice) {
        // Para filtros de rango, necesitaremos hacer el filtrado en el cliente
        // Firebase solo permite un orderByChild por consulta
        return mDataBase.get();
    }

    public Task<DataSnapshot> getWorkersWithMinRating(float minRating) {
        // Necesita filtrado adicional en el cliente
        return mDataBase.get();
    }

    // MÉTODOS DE UTILIDAD

    public Task<Void> updateLastActivity(String workerId) {
        return mDataBase.child(workerId).child("timestamp").setValue(System.currentTimeMillis());
    }

    public Task<Void> deleteWorker(String workerId) {
        return mDataBase.child(workerId).removeValue();
    }

    public Task<DataSnapshot> workerExists(String workerId) {
        return mDataBase.child(workerId).get();
    }


    public Task<DataSnapshot> countWorkers() {
        return mDataBase.get();
    }

    public Task<DataSnapshot> getWorkersByLastActivity() {
        Query activityQuery = mDataBase.orderByChild("timestamp").limitToLast(50);
        return activityQuery.get();
    }

    // LISTENERS PARA TIEMPO REAL

    public void listenForAvailableWorkers(ValueEventListener listener) {
        Query availableQuery = mDataBase.orderByChild("isAvailable").equalTo(true);
        availableQuery.addValueEventListener(listener);
    }


    public void listenForWorkerChanges(String workerId, ValueEventListener listener) {
        mDataBase.child(workerId).addValueEventListener(listener);
    }


    public void listenForOnlineWorkers(ValueEventListener listener) {
        Query onlineQuery = mDataBase.orderByChild("isOnline").equalTo(true);
        onlineQuery.addValueEventListener(listener);
    }


    public void removeListener(ValueEventListener listener) {
        mDataBase.removeEventListener(listener);
    }


    public void removeWorkerListener(String workerId, ValueEventListener listener) {
        mDataBase.child(workerId).removeEventListener(listener);
    }
}