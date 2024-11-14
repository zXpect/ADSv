package com.ads.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ads.models.Worker;

import java.util.HashMap;
import java.util.Map;

public class WorkerProvider {
    DatabaseReference mDataBase;

    public WorkerProvider() {
        mDataBase = FirebaseDatabase.getInstance().getReference().child("User").child("Trabajadores");
    }

    public Task<Void> create(Worker worker) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", worker.getName());
        map.put("lastName", worker.getLastName());
        map.put("email", worker.getEmail());
        map.put("work", worker.getWork());
        return mDataBase.child(worker.getId()).setValue(map);
    }

    public Task<DataSnapshot> getWorker(String id) {
        return mDataBase.child(id).get();
    }

    public Task<Void> updateWorkerFCMToken(String workerId, String fcmToken) {
        return mDataBase.child(workerId).child("fcmToken").setValue(fcmToken);
    }

    public Task<DataSnapshot> getWorkerFCMToken(String workerId) {
        return mDataBase.child(workerId).child("fcmToken").get();
    }

    public Task<Void> updateWorkerAvailability(String workerId, boolean isAvailable) {
        return mDataBase.child(workerId).child("isAvailable").setValue(isAvailable);
    }

    public Task<Void> updateWorkerFields(String workerId, Map<String, Object> updates) {
        return mDataBase.child(workerId).updateChildren(updates);
    }
}