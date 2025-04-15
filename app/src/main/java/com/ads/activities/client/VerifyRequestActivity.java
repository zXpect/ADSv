package com.ads.activities.client;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.project.ads.R;

public class VerifyRequestActivity extends AppCompatActivity {

    private static final String TAG = "VerifyRequestActivity";

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

        // Configurar la barra de estado transparente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }

        // Inicializar vistas
        initViews();

        // Cargar datos recibidos desde la actividad anterior
        loadReceivedData();

        // Configurar el botón de confirmación
        setupConfirmButton();
    }

    private void initViews() {
        mWorkerPhotoImageView = findViewById(R.id.worker_photo);
        mWorkerNameTextView = findViewById(R.id.worker_name);
        mAddressTextView = findViewById(R.id.tv_address_content); // Corregido para usar tv_address_content
        mServiceTypeTextView = findViewById(R.id.tv_service_type_content);
        mAmountTextView = findViewById(R.id.tv_amount_content);
        mConfirmButton = findViewById(R.id.btn_confirm);
    }

    private void loadReceivedData() {
        if (getIntent() != null) {
            // Obtener los datos del Intent
            int workerPhotoResId = getIntent().getIntExtra("worker_photo", R.drawable.workerlogo);
            String workerName = getIntent().getStringExtra("worker_name");
            String address = getIntent().getStringExtra("address");
            String serviceType = getIntent().getStringExtra("service_type");
            String amount = getIntent().getStringExtra("amount");

            Log.d(TAG, "Datos recibidos - Worker name: " + workerName +
                    ", Address: " + address +
                    ", Service type: " + serviceType);

            // Configurar las vistas con los datos recibidos
            mWorkerPhotoImageView.setImageResource(workerPhotoResId);

            // Asegurar que tenemos un nombre para mostrar
            if (workerName != null && !workerName.isEmpty()) {
                mWorkerNameTextView.setText(workerName);
            } else {
                mWorkerNameTextView.setText("Trabajador");
            }

            // Asegurar que tenemos una dirección para mostrar
            if (address != null && !address.isEmpty()) {
                mAddressTextView.setText(address);
            } else {
                mAddressTextView.setText("No especificada");
            }

            // Asegurar que tenemos un tipo de servicio para mostrar
            if (serviceType != null && !serviceType.isEmpty()) {
                mServiceTypeTextView.setText(serviceType);
            } else {
                mServiceTypeTextView.setText("No especificado");
            }

            // Asegurar que tenemos un monto para mostrar
            if (amount != null && !amount.isEmpty()) {
                mAmountTextView.setText(amount);
            } else {
                mAmountTextView.setText("A confirmar");
            }
        } else {
            Log.e(TAG, "No se recibieron datos en el Intent");
            // Si no hay datos, mostrar valores por defecto
            setDefaultValues();
        }
    }

    private void setDefaultValues() {
        mWorkerPhotoImageView.setImageResource(R.drawable.workerlogo);
        mWorkerNameTextView.setText("Trabajador");
        mAddressTextView.setText("No especificada");
        mServiceTypeTextView.setText("No especificado");
        mAmountTextView.setText("A confirmar");
    }

    private void setupConfirmButton() {
        mConfirmButton.setOnClickListener(v -> {
            // Crear intent para la siguiente actividad
            Intent intent = new Intent(this, ServiceCompletionActivity.class);

            // Opcionalmente, puedes pasar datos adicionales a la siguiente actividad
            intent.putExtra("worker_name", mWorkerNameTextView.getText().toString());
            intent.putExtra("service_type", mServiceTypeTextView.getText().toString());

            // Iniciar la actividad
            startActivity(intent);

            // Opcional: cerrar esta actividad si no deseas que el usuario vuelva aquí al presionar atrás
            // finish();
        });
    }
}