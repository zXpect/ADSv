package com.ads.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ads.models.Client;

import java.util.HashMap;
import java.util.Map;

public class ClientProvider {

    DatabaseReference mDataBase;

    public ClientProvider() {
        mDataBase = FirebaseDatabase.getInstance().getReference().child("User").child("Clientes");
    }

    public Task<Void> create(Client client){
        Map<String, Object> map = new HashMap<>();
        map.put("name", client.getName());
        map.put("lastName", client.getLastName());
        map.put("email", client.getEmail());

        return mDataBase.child(client.getId()).setValue(map);
    }

}
