package com.ads;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Configurar Firebase antes de cualquier uso
        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
    }
}