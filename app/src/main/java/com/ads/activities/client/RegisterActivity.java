package com.ads.activities.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.project.ads.R;
import com.ads.includes.MyToolbar;
import com.ads.models.Client;
import com.ads.providers.AuthProvider;
import com.ads.providers.ClientProvider;

public class RegisterActivity extends AppCompatActivity {

    SharedPreferences mPref;

    AuthProvider mAuthProvider;
    ClientProvider mClientProvider;

    // VIEWS
    Button mButtonRegister;
    TextInputEditText mTextInputNames;
    TextInputEditText mTextInputLastNames;
    TextInputEditText mTextInputEmail;
    TextInputEditText mTextInputPassword;
    CheckBox mCheckBoxTerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        MyToolbar.showTransparent(this, "Registro de Cliente", true);

        mAuthProvider = new AuthProvider();
        mClientProvider = new ClientProvider();

        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);

        mButtonRegister = findViewById(R.id.continue_register);
        mTextInputNames = findViewById(R.id.names_user_client);
        mTextInputLastNames = findViewById(R.id.last_names);
        mTextInputEmail = findViewById(R.id.emailAddress);
        mTextInputPassword = findViewById(R.id.Password);
        mCheckBoxTerms = findViewById(R.id.checkBox2);

        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRegister();
            }
        });

        // Configurar el color de la barra de estado al color por defecto del sistema
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

        if (!name.isEmpty() && !lastName.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
            if (password.length() >= 8) {
                if (mCheckBoxTerms.isChecked()) {
                    register(name, lastName, email, password);
                } else {
                    Toast.makeText(this, "Debe aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Ingrese todos los campos", Toast.LENGTH_SHORT).show();
        }
    }

    void register(final String name, final String lastName, final String email, String password) {
        mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    String id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Client client = new Client(id, name, lastName, email);
                    create(client);
                } else {
                    Toast.makeText(RegisterActivity.this, "No se pudo registrar el usuario", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void create(Client client) {
        mClientProvider.create(client).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "El registro se realizó con éxito", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, HomeUserActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(RegisterActivity.this, "No se pudo crear el cliente", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}