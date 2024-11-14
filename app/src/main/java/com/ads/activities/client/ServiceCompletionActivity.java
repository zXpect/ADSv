package com.ads.activities.client;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.project.ads.R;

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