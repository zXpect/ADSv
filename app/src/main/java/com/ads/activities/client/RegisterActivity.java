package com.ads.activities.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.project.ads.R;
import com.ads.includes.MyToolbar;
import com.ads.models.Client;
import com.ads.providers.AuthProvider;
import com.ads.providers.ClientProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 50;
    private static final int MAX_NAME_LENGTH = 50;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");

    private SharedPreferences mPref;
    private AuthProvider mAuthProvider;
    private ClientProvider mClientProvider;

    // Views
    private Button mButtonRegister;
    private TextInputEditText mTextInputNames;
    private TextInputEditText mTextInputLastNames;
    private TextInputEditText mTextInputEmail;
    private TextInputEditText mTextInputPassword;
    private CheckBox mCheckBoxTerms;

    // TextInputLayouts para mostrar errores
    private TextInputLayout mTextInputLayoutNames;
    private TextInputLayout mTextInputLayoutLastNames;
    private TextInputLayout mTextInputLayoutEmail;
    private TextInputLayout mTextInputLayoutPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeComponents();
        setupStatusBar();
        setupClickListeners();
    }

    private void initializeComponents() {
        MyToolbar.showTransparent(this, "Registro de Cliente", true);

        mAuthProvider = new AuthProvider();
        mClientProvider = new ClientProvider();
        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);

        // Initialize views
        mButtonRegister = findViewById(R.id.continue_register);
        mTextInputNames = findViewById(R.id.names_user_client);
        mTextInputLastNames = findViewById(R.id.last_names);
        mTextInputEmail = findViewById(R.id.emailAddress);
        mTextInputPassword = findViewById(R.id.Password);
        mCheckBoxTerms = findViewById(R.id.checkBox2);

        // Initialize TextInputLayouts
        mTextInputLayoutNames = findViewById(R.id.textInputLayoutNames);
        mTextInputLayoutLastNames = findViewById(R.id.textInputLayoutLastNames);
        mTextInputLayoutEmail = findViewById(R.id.textInputLayoutEmail);
        mTextInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void setupClickListeners() {
        mButtonRegister.setOnClickListener(v -> {
            // Deshabilitar el botón para prevenir múltiples clicks
            mButtonRegister.setEnabled(false);
            try {
                clickRegister();
            } finally {
                // Reactivar el botón después de 1 segundo
                mButtonRegister.postDelayed(() -> mButtonRegister.setEnabled(true), 1000);
            }
        });
    }

    private void clickRegister() {
        // Limpiar errores previos
        clearErrors();

        // Obtener y validar datos
        String name = sanitizeInput(mTextInputNames.getText().toString());
        String lastName = sanitizeInput(mTextInputLastNames.getText().toString());
        String email = sanitizeInput(mTextInputEmail.getText().toString().toLowerCase());
        String password = mTextInputPassword.getText().toString();

        // Validar campos
        if (!validateFields(name, lastName, email, password)) {
            return;
        }

        // Verificar términos y condiciones
        if (!mCheckBoxTerms.isChecked()) {
            showToast("Debe aceptar los términos y condiciones");
            return;
        }

        // Proceder con el registro
        register(name, lastName, email, password);
    }

    private boolean validateFields(String name, String lastName, String email, String password) {
        boolean isValid = true;

        // Validar nombre
        if (!validateName(name)) {
            mTextInputLayoutNames.setError("Nombre inválido (solo letras, máx. 50 caracteres)");
            isValid = false;
        }

        // Validar apellido
        if (!validateName(lastName)) {
            mTextInputLayoutLastNames.setError("Apellido inválido (solo letras, máx. 50 caracteres)");
            isValid = false;
        }

        // Validar email
        if (!validateEmail(email)) {
            mTextInputLayoutEmail.setError("Email inválido");
            isValid = false;
        }

        // Validar contraseña
        if (!validatePassword(password)) {
            mTextInputLayoutPassword.setError("La contraseña debe tener entre 8 y 50 caracteres, incluir mayúsculas, minúsculas y números");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateName(String name) {
        return !TextUtils.isEmpty(name) &&
                name.length() <= MAX_NAME_LENGTH &&
                NAME_PATTERN.matcher(name).matches();
    }

    private boolean validateEmail(String email) {
        return !TextUtils.isEmpty(email) &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean validatePassword(String password) {
        return !TextUtils.isEmpty(password) &&
                password.length() >= MIN_PASSWORD_LENGTH &&
                password.length() <= MAX_PASSWORD_LENGTH &&
                password.matches(".*[A-Z].*") && // Al menos una mayúscula
                password.matches(".*[a-z].*") && // Al menos una minúscula
                password.matches(".*\\d.*");     // Al menos un número
    }

    private String sanitizeInput(String input) {
        return input != null ? input.trim() : "";
    }

    private void clearErrors() {
        mTextInputLayoutNames.setError(null);
        mTextInputLayoutLastNames.setError(null);
        mTextInputLayoutEmail.setError(null);
        mTextInputLayoutPassword.setError(null);
    }

    private void register(final String name, final String lastName, final String email, String password) {
        mAuthProvider.register(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        Client client = new Client(id, name, lastName, email);
                        createClient(client);
                    } else {
                        handleRegistrationError(task.getException());
                    }
                });
    }

    private void createClient(Client client) {
        mClientProvider.create(client)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        showToast("El registro se realizó con éxito");
                        navigateToHome();
                    } else {
                        Log.e(TAG, "Error creating client", task.getException());
                        showToast("Error al crear el cliente. Por favor, intente nuevamente");
                        // Eliminar el usuario de autenticación si falla la creación del cliente
                        FirebaseAuth.getInstance().getCurrentUser().delete();
                    }
                });
    }

    private void handleRegistrationError(Exception exception) {
        String errorMessage = "Error en el registro. ";

        if (exception instanceof FirebaseAuthWeakPasswordException) {
            errorMessage += "La contraseña es muy débil";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            errorMessage += "El email no es válido";
        } else if (exception instanceof FirebaseAuthUserCollisionException) {
            errorMessage += "El email ya está registrado";
        } else {
            errorMessage += "Por favor, intente nuevamente";
            Log.e(TAG, "Registration error", exception);
        }

        showToast(errorMessage);
    }

    private void navigateToHome() {
        Intent intent = new Intent(RegisterActivity.this, HomeUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}