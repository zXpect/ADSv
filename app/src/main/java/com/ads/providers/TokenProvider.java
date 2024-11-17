package com.ads.providers;

import androidx.annotation.NonNull;

import com.ads.models.Token;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.messaging.FirebaseMessaging;

public class TokenProvider {

    public DatabaseReference mDatabase;

    public TokenProvider() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Tokens");
    }

    public void create(final String idUser) {
        if (idUser == null) return;
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                Token token = new Token(task.getResult());
                mDatabase.child(idUser).setValue(token);

            }
        });
    }
}
