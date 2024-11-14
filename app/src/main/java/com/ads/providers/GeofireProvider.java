package com.ads.providers;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ads.models.Worker;

public class GeofireProvider {
    private final DatabaseReference mDatabase;
    private final GeoFire mGeofire;

    public GeofireProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("active_workers");
        mGeofire = new GeoFire(mDatabase);
    }

    public void saveLocation(String idWorker, LatLng latLng) {
        mGeofire.setLocation(idWorker, new GeoLocation(latLng.latitude, latLng.longitude));
    }

    public void removeLocation(String idWorker) {
        mGeofire.removeLocation(idWorker);
    }

    public GeoQuery getActiveWorkers(LatLng currentLatLng, double radius) {
        return mGeofire.queryAtLocation(new GeoLocation(currentLatLng.latitude, currentLatLng.longitude), radius);
    }

    // Método para obtener información del trabajador
    public void getWorkerInfo(String idWorker, final WorkerInfoCallback callback) {
        DatabaseReference workerRef = FirebaseDatabase.getInstance().getReference().child("User").child("Trabajadores").child(idWorker);

        workerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Worker worker = dataSnapshot.getValue(Worker.class);
                    if (worker != null) {
                        // Llamar al callback con la información del trabajador
                        callback.onWorkerInfoRetrieved(worker);
                    } else {
                        callback.onWorkerInfoRetrieved(null); // Si la conversión falla
                    }
                } else {
                    callback.onWorkerInfoRetrieved(null);  // Si el trabajador no existe
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onWorkerInfoError(databaseError.toException());
            }
        });
    }

    // Definir una interfaz para el callback
    public interface WorkerInfoCallback {
        void onWorkerInfoRetrieved(Worker worker);
        void onWorkerInfoError(Exception e);
    }
}
