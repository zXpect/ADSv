package com.ads.activities.worker;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ads.includes.MyToolbar;
import com.ads.models.Worker;
import com.ads.providers.AuthProvider;
import com.ads.providers.WorkerProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.project.ads.R;

public class RegisterWorkerActivity extends AppCompatActivity {

    private AuthProvider mAuthProvider;
    private WorkerProvider mWorkerProvider;
    private Button mButtonRegister;
    private TextInputEditText mTextInputNames, mTextInputLastNames, mTextInputEmail, mTextInputPassword;
    private AutoCompleteTextView mSpinnerWork;
    private ProgressDialog mProgressDialog;
    private CheckBox mCheckBoxTerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_worker);
        MyToolbar.showTransparent(this, "Registro de Trabajador", true);


        initializeViews();
        initializeProviders();
        setupServiceTypeDropdown();


        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRegister();
            }
        });

        setupStatusBar();
    }

    private void initializeViews() {
        mButtonRegister = findViewById(R.id.continue_register);
        mTextInputNames = findViewById(R.id.names_user_client);
        mTextInputLastNames = findViewById(R.id.last_names);
        mTextInputEmail = findViewById(R.id.emailAddress);
        mTextInputPassword = findViewById(R.id.password);
        mSpinnerWork = findViewById(R.id.spinner_service_type);
        mCheckBoxTerms = findViewById(R.id.checkBox2);
        mProgressDialog = new ProgressDialog(this);
    }

    private void setupServiceTypeDropdown() {
        String[] items = getResources().getStringArray(R.array.service_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        mSpinnerWork.setAdapter(adapter);
    }

    private void initializeProviders() {
        mAuthProvider = new AuthProvider();
        mWorkerProvider = new WorkerProvider();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void clickRegister() {
        String name = mTextInputNames.getText().toString().trim();
        String lastName = mTextInputLastNames.getText().toString().trim();
        String email = mTextInputEmail.getText().toString().trim();
        String password = mTextInputPassword.getText().toString();
        String work = mSpinnerWork.getText().toString();

        if (name.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || work.isEmpty()) {
            Toast.makeText(this, "Ingrese todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mCheckBoxTerms.isChecked()) {
            Toast.makeText(this, "Debe aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog.setMessage("Registrando...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        register(name, lastName, email, work, password);
    }

    private void register(final String name, final String lastName, final String email, final String work, String password) {
        mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                mProgressDialog.dismiss();
                if (task.isSuccessful()) {
                    String id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Worker worker = new Worker(id, name, lastName, email, work);
                    createWorker(worker);
                } else {
                    Toast.makeText(RegisterWorkerActivity.this, "No se pudo registrar el usuario: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createWorker(Worker worker) {
        mWorkerProvider.create(worker).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(RegisterWorkerActivity.this, "El registro se realizó con éxito", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterWorkerActivity.this, HomeWorkerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(RegisterWorkerActivity.this, "No se pudo crear el Trabajador: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}