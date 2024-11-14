package com.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.ads.activities.MainActivity;
import com.ads.providers.AuthProvider;
import com.project.ads.R;

public class FixDepotActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private Button mButtonFindStores;
    private AuthProvider mAuthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fix_depot);

        mAuthProvider = new AuthProvider();

        mToolbar = findViewById(R.id.toolbar_color);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Fix Depot");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mButtonFindStores = findViewById(R.id.buttonFindStores);

        mButtonFindStores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findStores();
            }
        });

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void findStores() {
        // Implement store finder functionality here
        // For example, you could start a new activity:
        // Intent intent = new Intent(FixDepotActivity.this, StoreFinderActivity.class);
        // startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.worker_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuthProvider.logOut();
            Intent intent = new Intent(FixDepotActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}