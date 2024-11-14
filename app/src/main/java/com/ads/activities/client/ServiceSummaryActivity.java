package com.ads.activities.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.RatingBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.project.ads.R;

public class ServiceSummaryActivity extends AppCompatActivity {

    private TextView tvUserName, tvAddress, tvServiceType, tvStartTime, tvEndTime, tvWorkerName, tvRatingValue;
    private RatingBar ratingBar;
    private MaterialButton btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_summary);

        initViews();
        setData();
        setupListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvAddress = findViewById(R.id.tv_address);
        tvServiceType = findViewById(R.id.tv_service_type);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        tvWorkerName = findViewById(R.id.tv_worker_name);
        tvRatingValue = findViewById(R.id.tv_rating_value);
        ratingBar = findViewById(R.id.rating_bar);
        btnFinish = findViewById(R.id.btn_finish);
    }

    private void setData() {
        // Aquí deberías obtener los datos reales del servicio, ya sea de la base de datos,
        // de los extras del Intent, o de donde sea que los estés pasando.
        // Por ahora, usaremos datos de ejemplo.

        String userName = "Worker01";
        String address = "Calle 85";
        String serviceType = "Plomería";
        String startTime = "10:00 AM";
        String endTime = "11:30 AM";
        String workerName = "Cliente01";
        float rating = 4.5f;

        tvUserName.setText("Nombre del Usuario: " + userName);
        tvAddress.setText("Dirección: " + address);
        tvServiceType.setText("Servicio Solicitado: " + serviceType);
        tvStartTime.setText("Hora de Inicio: " + startTime);
        tvEndTime.setText("Hora de Finalización: " + endTime);
        tvWorkerName.setText("Trabajador: " + workerName);

        ratingBar.setRating(rating);
        tvRatingValue.setText(String.format("%.1f", rating));
    }

    private void setupListeners() {
        btnFinish.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeUserActivity.class);
            startActivity(intent);
            finish();
        });
    }
}