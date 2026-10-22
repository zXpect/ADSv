package com.project.ads;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.project.ads.helpers.ConnectivityBanner;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Configurar Firebase antes de cualquier uso
        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
        // Aviso sin conexión en cualquier pantalla (HU-35)
        ConnectivityBanner.install(this);
    }
}