package com.project.ads.activities.client;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.project.ads.R;

/**
 * Sin uso desde el rediseño (Fase 6, 17/10/2026): ninguna pantalla la abre.
 * Se conserva sin borrar hasta que el equipo lo apruebe; lint la ignora (app/lint.xml).
 */
@Deprecated
public class ServiceCompletionActivity extends AppCompatActivity {

    private Button mButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_completion);

        mButton = findViewById(R.id.finishButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextA();
            }

        private void nextA() {
            Intent intent = new Intent(ServiceCompletionActivity.this, ServiceSummaryActivity.class);
            startActivity(intent);
        }
    });
    }
}