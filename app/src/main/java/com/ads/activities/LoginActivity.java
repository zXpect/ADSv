package com.ads.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.ads.activities.client.HomeUserActivity;
import com.ads.activities.client.RegisterActivity;
import com.ads.activities.worker.HomeWorkerActivity;
import com.ads.activities.worker.RegisterWorkerActivity;
import com.ads.includes.MyToolbar;
import com.ads.providers.TokenProvider;
import com.ads.helpers.NotificationPermissionHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.project.ads.R;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCK_DURATION_MILLIS = 300000; // 5 minutos
    private static final String TAG = "LoginActivity";

    private SharedPreferences mPref;
    private ProgressDialog progressDialog;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText mTextInputEmail;
    private TextInputEditText mTextInputPassword;
    private Button mButtonUserLogin;
    private TextView mButtonUserRegister;
    private SignInButton mGoogleSignInButton;
    private TextView mForgotPasswordTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference mDataBase;
    private GoogleSignInClient mGoogleSignInClient;
    private int loginAttempts = 0;
    private long lastLoginAttemptTime = 0;
    private ImageButton mGoogleSignInImageButton;

    private static final String USER_TYPE_KEY = "typeUser";

    // Launcher para solicitar permisos de notificación
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        // Registrar el launcher para permisos de notificación
        notificationPermissionLauncher = NotificationPermissionHelper.registerForPermissionResult(this);

        initializeViews();
        setupToolbar();
        setupFirebase();
        setupListeners();
        setupStatusBar();
        setupGoogleSignIn();

        // Obtener y guardar el token FCM
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Obtener el nuevo token FCM
                    String token = task.getResult();

                    // Si el usuario está autenticado, guardar el token
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        new TokenProvider().saveToken(token);
                        Log.d("FCM", "Token guardado para: " + userId);
                    }

                    // Log y debug
                    Log.d("FCM", "Token: " + token);
                });
    }

    private void initializeViews() {

        mPref = getApplicationContext().getSharedPreferences(USER_TYPE_KEY, MODE_PRIVATE);

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        mTextInputEmail = findViewById(R.id.editTextTextEmailAddress2);
        mTextInputPassword = findViewById(R.id.editTextTextPassword);
        mButtonUserLogin = findViewById(R.id.login);
        mButtonUserRegister = findViewById(R.id.register_text);
        mGoogleSignInButton = findViewById(R.id.google_sign_in_button);
        mGoogleSignInImageButton = findViewById(R.id.google_sign_in_image_button);
        mForgotPasswordTextView = findViewById(R.id.forgot_password);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Iniciando sesión...");
        progressDialog.setCancelable(false);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDataBase = FirebaseDatabase.getInstance().getReference();
    }

    private void setupToolbar() {
        String typeUser = capitalizeFirstLetter(mPref.getString("user", ""));
        MyToolbar.showTransparent(this, typeUser, true);
    }

    private void setupListeners() {
        mButtonUserLogin.setOnClickListener(v -> {
            if (isLoginAllowed()) {
                validateAndLogin();
            } else {
                showLockoutMessage();
            }
        });

        mButtonUserRegister.setOnClickListener(v -> register());
        mGoogleSignInImageButton.setOnClickListener(v -> signInWithGoogle());
        mForgotPasswordTextView.setOnClickListener(v -> resetPassword());
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressDialog.show();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        handleSuccessfulLogin();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetPassword() {
        String email = mTextInputEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Ingrese su correo electrónico para restablecer la contraseña");
            return;
        }

        progressDialog.setMessage("Enviando correo de restablecimiento...");
        progressDialog.show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Se ha enviado un correo para restablecer su contraseña", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error al enviar el correo de restablecimiento", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isLoginAllowed() {
        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            long timeElapsed = System.currentTimeMillis() - lastLoginAttemptTime;
            if (timeElapsed < LOCK_DURATION_MILLIS) {
                return false;
            }
            loginAttempts = 0;
        }
        return true;
    }

    private void showLockoutMessage() {
        long timeRemaining = LOCK_DURATION_MILLIS - (System.currentTimeMillis() - lastLoginAttemptTime);
        int minutesRemaining = (int) (timeRemaining / 60000);
        Toast.makeText(this,
                "Demasiados intentos fallidos. Por favor, espere " + minutesRemaining + " minutos.",
                Toast.LENGTH_LONG).show();
    }

    private void validateAndLogin() {
        String email = mTextInputEmail.getText().toString().trim();
        String password = mTextInputPassword.getText().toString();

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        performLogin(email, password);
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("El correo electrónico es requerido");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingrese un correo electrónico válido");
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("La contraseña es requerida");
            return false;
        }
        return true;
    }

    private void performLogin(String email, String password) {
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        handleSuccessfulLogin();
                    } else {
                        handleFailedLogin(task.getException());
                    }
                });
    }

    private void handleSuccessfulLogin() {
        loginAttempts = 0;
        String user = mPref.getString("user", "");
        Class<?> destinationActivity = user.equals("cliente") ?
                HomeUserActivity.class : HomeWorkerActivity.class;

        // Para Android 13+ (TIRAMISU), solicitar el permiso de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Registrar el token FCM después de iniciar sesión
            registerFCMToken();

            // Solicitar el permiso de notificaciones y esperar la respuesta
            NotificationPermissionHelper.requestNotificationPermissionWithCallback(
                    this,
                    notificationPermissionLauncher,
                    granted -> proceedToNextActivity(destinationActivity)
            );
        } else {
            // Para versiones anteriores, continuar directamente
            registerFCMToken();
            proceedToNextActivity(destinationActivity);
        }
    }

    /**
     * Registra el token FCM para el usuario actual
     */
    private void registerFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        if (mAuth.getCurrentUser() != null) {
                            new TokenProvider().saveToken(token);
                        }
                    }
                });
    }

    /**
     * Navega a la siguiente actividad
     */
    private void proceedToNextActivity(Class<?> destinationActivity) {
        Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, destinationActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleFailedLogin(Exception exception) {
        loginAttempts++;
        lastLoginAttemptTime = System.currentTimeMillis();

        if (exception instanceof FirebaseAuthInvalidUserException) {
            tilEmail.setError("No existe una cuenta con este correo");
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            tilPassword.setError("Contraseña incorrecta");
        } else {
            Toast.makeText(this,
                    "Error de autenticación: " + exception.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            showLockoutMessage();
        }
    }

    private void register() {
        String typeUser = mPref.getString("user", "");
        Class<?> registrationActivity = typeUser.equals("cliente") ?
                RegisterActivity.class : RegisterWorkerActivity.class;

        Intent intent = new Intent(this, registrationActivity);
        startActivity(intent);
    }

    private String capitalizeFirstLetter(String input) {
        if (TextUtils.isEmpty(input)) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }
}