package com.ads.activities.client;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.project.ads.R;

public class VerifyRequestActivity extends AppCompatActivity {

    private ImageView mWorkerPhotoImageView;
    private TextView mWorkerNameTextView;
    private TextView mAddressTextView;
    private TextView mServiceTypeTextView;
    private TextView mAmountTextView;
    private Button mConfirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_request);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }

        mWorkerPhotoImageView = findViewById(R.id.worker_photo);
        mWorkerNameTextView = findViewById(R.id.worker_name);
        mAddressTextView = findViewById(R.id.tv_address);
        mServiceTypeTextView = findViewById(R.id.tv_service_type_content);
        mAmountTextView = findViewById(R.id.tv_amount_content);
        mConfirmButton = findViewById(R.id.btn_confirm);


        if (getIntent() != null) {
            // Obtener los datos del Intent
            int workerPhotoResId = getIntent().getIntExtra("worker_photo", R.drawable.workerlogo);
            String workerName = getIntent().getStringExtra("worker_name");
            String address = getIntent().getStringExtra("address");
            String serviceType = getIntent().getStringExtra("service_type");
            String amount = getIntent().getStringExtra("amount");

            // Configurar las vistas con los datos recibidos
            mWorkerPhotoImageView.setImageResource(workerPhotoResId);
            mWorkerNameTextView.setText(workerName);
            mAddressTextView.setText(address);
            mServiceTypeTextView.setText(serviceType);
            mAmountTextView.setText(amount);
        }

        // Configurar el botón de confirmación
        mConfirmButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ServiceCompletionActivity.class);
            startActivity(intent);
        });
    }
}
