package com.ads.providers;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ServiceRequestProvider {
    private DatabaseReference mDatabase;

    public ServiceRequestProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("service_requests");
    }

    public Task<Void> createServiceRequest(String clientId, String workerId, LatLng clientLocation) {
        String requestId = mDatabase.push().getKey();
        ServiceRequest request = new ServiceRequest(clientId, workerId, clientLocation.latitude, clientLocation.longitude);
        return mDatabase.child(requestId).setValue(request);
    }

    public class ServiceRequest {
        private String clientId;
        private String workerId;
        private double latitude;
        private double longitude;
        private String status; // "pending", "accepted", "rejected", "completed"

        public ServiceRequest(String clientId, String workerId, double latitude, double longitude) {
        }
    }
}