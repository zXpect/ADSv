package com.project.ads.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.project.ads.activities.client.HomeUserActivity;
import com.project.ads.activities.client.RegisterActivity;
import com.project.ads.activities.worker.HomeWorkerActivity;
import com.project.ads.activities.worker.FirtsRegisterWorkerActivity;
import com.project.ads.includes.MyToolbar;
import com.project.ads.providers.ClientProvider;
import com.project.ads.providers.WorkerProvider;
import com.project.ads.providers.TokenProvider;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.project.ads.R;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
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
    private ClientProvider mClientProvider;
    private WorkerProvider mWorkerProvider;
    private int loginAttempts = 0;
    private long lastLoginAttemptTime = 0;
    private ImageButton mGoogleSignInImageButton;

    private static final String USER_TYPE_KEY = "typeUser";

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    Log.d(TAG, "Permiso de notificaciones: " + (isGranted ? "concedido" : "denegado"));
                    navigateToNextActivity();
                }
        );

        initializeViews();
        setupToolbar();
        setupFirebase();
        setupListeners();
        setupStatusBar();
        setupGoogleSignIn();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        new TokenProvider().saveToken(token);
                    }
                    Log.d("FCM", "Token FCM obtenido");
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
        mClientProvider = new ClientProvider();
        mWorkerProvider = new WorkerProvider();
    }

    private void setupToolbar() {
        String typeUser = capitalizeFirstLetter(mPref.getString("user", ""));
        MyToolbar.showTransparent(this, typeUser, true);
        // El toolbar va sobre la cabecera navy: título claro
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.ads_on_navy));
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
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
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
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressDialog.setMessage("Autenticando con Google...");
        progressDialog.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkIfUserExistsInDatabase(user);
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Error de autenticación: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfUserExistsInDatabase(FirebaseUser user) {
        String userId = user.getUid();
        String typeUser = mPref.getString("user", "");

        DatabaseReference currentUserRef;
        DatabaseReference oppositeUserRef;
        String oppositeUserType;

        if (typeUser.equals("cliente")) {
            currentUserRef = mDataBase.child("User").child("Clientes").child(userId);
            oppositeUserRef = mDataBase.child("User").child("Trabajadores").child(userId);
            oppositeUserType = "trabajador";
        } else {
            currentUserRef = mDataBase.child("User").child("Trabajadores").child(userId);
            oppositeUserRef = mDataBase.child("User").child("Clientes").child(userId);
            oppositeUserType = "cliente";
        }

        oppositeUserRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().exists()) {
                    progressDialog.dismiss();
                    mAuth.signOut();
                    mGoogleSignInClient.signOut();
                    showUserExistsAsOtherTypeDialog(oppositeUserType);
                    return;
                }
                checkCurrentUserType(currentUserRef, user, typeUser);
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Error al verificar usuario en la base de datos",
                        Toast.LENGTH_SHORT).show();
                mAuth.signOut();
            }
        });
    }

    private void checkCurrentUserType(DatabaseReference currentUserRef, FirebaseUser user, String typeUser) {
        currentUserRef.get().addOnCompleteListener(task -> {
            progressDialog.dismiss();

            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().exists()) {
                    Log.d(TAG, "Usuario encontrado en la base de datos como " + typeUser);
                    handleSuccessfulLogin();
                } else {
                    Log.d(TAG, "Usuario nuevo, redirigiendo a registro de " + typeUser);
                    redirectToCompleteRegistration(user);
                }
            } else {
                Toast.makeText(this, "Error al verificar usuario en la base de datos",
                        Toast.LENGTH_SHORT).show();
                mAuth.signOut();
            }
        });
    }

    /**
     * ⭐ MEJORADO: Ahora ofrece cambiar de tipo de usuario
     */
    private void showUserExistsAsOtherTypeDialog(String existingUserType) {
        String currentType = mPref.getString("user", "");

        new AlertDialog.Builder(this)
                .setTitle("Tipo de usuario incorrecto")
                .setMessage("Esta cuenta de Google ya está registrada como " + existingUserType +
                        ", no como " + currentType + ".\n\n" +
                        "¿Deseas cambiar a iniciar sesión como " + existingUserType + "?")
                .setPositiveButton("Sí, cambiar", (dialog, which) -> {
                    // Cambiar el tipo de usuario
                    SharedPreferences.Editor editor = mPref.edit();
                    editor.putString("user", existingUserType);
                    editor.apply();

                    // Recargar la actividad
                    Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .setCancelable(false)
                .show();
    }

    private void redirectToCompleteRegistration(FirebaseUser user) {
        String typeUser = mPref.getString("user", "");

        Intent intent;
        if (typeUser.equals("cliente")) {
            intent = new Intent(this, RegisterActivity.class);
        } else {
            intent = new Intent(this, FirtsRegisterWorkerActivity.class);
        }

        intent.putExtra("fromGoogleSignIn", true);
        intent.putExtra("userId", user.getUid());
        intent.putExtra("email", user.getEmail());

        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            String[] nameParts = user.getDisplayName().split(" ", 2);
            intent.putExtra("name", nameParts[0]);
            if (nameParts.length > 1) {
                intent.putExtra("lastName", nameParts[1]);
            }
        }

        Toast.makeText(this, "Por favor, completa tu registro", Toast.LENGTH_SHORT).show();
        startActivity(intent);
        finish();
    }

    /**
     * Recuperación de contraseña.
     *
     * Envía el correo directamente con Firebase Auth, sin consultar antes la base
     * de datos: esa consulta exigía leer User/Clientes y User/Trabajadores sin
     * sesión (bloqueado por las reglas de Firebase) y permitía averiguar qué
     * correos están registrados. La cuenta de Auth es la misma para ambos roles,
     * así que no hace falta saber el tipo de usuario.
     */
    private void resetPassword() {
        String email = mTextInputEmail.getText().toString().trim();

        tilEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.forgot_password_enter_email));
            mTextInputEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingrese un correo electrónico válido");
            mTextInputEmail.requestFocus();
            return;
        }

        showConfirmationToSendEmail(email);
    }

    private void showConfirmationToSendEmail(String email) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password_title)
                .setMessage(getString(R.string.forgot_password_confirm_message, email))
                .setPositiveButton(R.string.forgot_password_confirm_yes,
                        (dialog, which) -> sendPasswordResetEmailDirect(email))
                .setNegativeButton(R.string.forgot_password_cancel, null)
                .show();
    }

    private void sendPasswordResetEmailDirect(String email) {
        progressDialog.setMessage(getString(R.string.forgot_password_sending));
        progressDialog.show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();

                    // Un correo no registrado se trata igual que un envío exitoso,
                    // para no revelar qué cuentas existen
                    if (task.isSuccessful() || task.getException() instanceof FirebaseAuthInvalidUserException) {
                        showResetPasswordSuccess(email);
                    } else {
                        handleResetPasswordError(task.getException());
                    }
                });
    }

    private void handleResetPasswordError(Exception exception) {
        String errorMessage;

        if (exception != null && exception.getMessage() != null) {
            if (exception.getMessage().contains("badly formatted")) {
                errorMessage = "El formato del correo electrónico no es válido.";
            } else if (exception.getMessage().contains("network")) {
                errorMessage = "Error de conexión. Verifica tu conexión a internet e intenta nuevamente.";
            } else {
                errorMessage = getString(R.string.forgot_password_send_error);
            }
        } else {
            errorMessage = getString(R.string.forgot_password_send_error);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password_error_title)
                .setMessage(errorMessage)
                .setPositiveButton("Entendido", null)
                .show();

        Log.e(TAG, "Error al enviar email de recuperación: ", exception);
    }

    private void showResetPasswordSuccess(String email) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password_success_title)
                .setMessage(getString(R.string.forgot_password_neutral_message, email))
                .setPositiveButton("Entendido", null)
                .show();
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
        int secondsRemaining = (int) ((timeRemaining % 60000) / 1000);

        String message = minutesRemaining > 0
                ? "Demasiados intentos fallidos. Por favor, espere " + minutesRemaining + " minuto(s) y " + secondsRemaining + " segundo(s)."
                : "Demasiados intentos fallidos. Por favor, espere " + secondsRemaining + " segundo(s).";

        new AlertDialog.Builder(this)
                .setTitle("Cuenta bloqueada temporalmente")
                .setMessage(message)
                .setPositiveButton("Entendido", null)
                .show();
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
            mTextInputEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingrese un correo electrónico válido");
            mTextInputEmail.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("La contraseña es requerida");
            mTextInputPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            mTextInputPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void performLogin(String email, String password) {
        progressDialog.setMessage("Iniciando sesión...");
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            verifyUserTypeAfterLogin(user.getUid(), email);
                        }
                    } else {
                        progressDialog.dismiss();
                        handleFailedLogin(task.getException());
                    }
                });
    }

    private void verifyUserTypeAfterLogin(String userId, String email) {
        String typeUser = mPref.getString("user", "");
        DatabaseReference currentUserRef;
        DatabaseReference oppositeUserRef;
        String oppositeUserType;

        if (typeUser.equals("cliente")) {
            currentUserRef = mDataBase.child("User").child("Clientes").child(userId);
            oppositeUserRef = mDataBase.child("User").child("Trabajadores").child(userId);
            oppositeUserType = "trabajador";
        } else {
            currentUserRef = mDataBase.child("User").child("Trabajadores").child(userId);
            oppositeUserRef = mDataBase.child("User").child("Clientes").child(userId);
            oppositeUserType = "cliente";
        }

        currentUserRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().exists()) {
                    progressDialog.dismiss();
                    handleSuccessfulLogin();
                } else {
                    checkOppositeNodeAfterLogin(oppositeUserRef, oppositeUserType, typeUser);
                }
            } else {
                progressDialog.dismiss();
                Log.e(TAG, "Error al verificar tipo de usuario: ", task.getException());
                Toast.makeText(this, "Error al verificar usuario", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
            }
        });
    }

    private void checkOppositeNodeAfterLogin(DatabaseReference oppositeRef, String oppositeUserType, String currentUserType) {
        oppositeRef.get().addOnCompleteListener(task -> {
            progressDialog.dismiss();

            if (task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().exists()) {
                    mAuth.signOut();
                    showLoginWrongTypeDialog(currentUserType, oppositeUserType);
                } else {
                    mAuth.signOut();
                    new AlertDialog.Builder(this)
                            .setTitle("Registro incompleto")
                            .setMessage("Tu cuenta existe pero no has completado el registro. Por favor, regístrate como " + currentUserType + ".")
                            .setPositiveButton("Registrarme", (dialog, which) -> register())
                            .setNegativeButton("Cancelar", null)
                            .show();
                }
            } else {
                mAuth.signOut();
                Toast.makeText(this, "Error al verificar usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoginWrongTypeDialog(String currentType, String correctType) {
        new AlertDialog.Builder(this)
                .setTitle("Tipo de usuario incorrecto")
                .setMessage(
                        "Este correo está registrado como " + correctType + ", no como " + currentType + ".\n\n" +
                                "¿Deseas cambiar a iniciar sesión como " + correctType + "?"
                )
                .setPositiveButton("Sí, cambiar", (dialog, which) -> {
                    SharedPreferences.Editor editor = mPref.edit();
                    editor.putString("user", correctType);
                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .setCancelable(false)
                .show();
    }

    private void handleSuccessfulLogin() {
        loginAttempts = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                navigateToNextActivity();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            navigateToNextActivity();
        }
    }

    private void navigateToNextActivity() {
        String user = mPref.getString("user", "");
        Class<?> destinationActivity = user.equals("cliente") ?
                HomeUserActivity.class : HomeWorkerActivity.class;

        Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, destinationActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * ⭐ MANEJO MEJORADO DE ERRORES DE LOGIN
     */
    private void handleFailedLogin(Exception exception) {
        loginAttempts++;
        lastLoginAttemptTime = System.currentTimeMillis();

        Log.e(TAG, "Error de login: ", exception);

        if (exception instanceof FirebaseAuthInvalidUserException) {
            // Usuario no existe
            tilEmail.setError("No existe una cuenta con este correo");
            tilEmail.setErrorEnabled(true);

            new AlertDialog.Builder(this)
                    .setTitle("Correo no registrado")
                    .setMessage("No existe una cuenta con este correo electrónico.\n\n" +
                            "¿Deseas crear una cuenta nueva?")
                    .setPositiveButton("Registrarme", (dialog, which) -> register())
                    .setNegativeButton("Cancelar", null)
                    .show();

        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            // Contraseña incorrecta
            tilPassword.setError("Contraseña incorrecta");
            tilPassword.setErrorEnabled(true);

            if (loginAttempts >= 2) {
                showPasswordRecoveryHint();
            } else {
                Toast.makeText(this,
                        "Contraseña incorrecta. Intento " + loginAttempts + " de " + MAX_LOGIN_ATTEMPTS,
                        Toast.LENGTH_LONG).show();
            }

        } else {
            // Otros errores
            String errorMessage = "Error al iniciar sesión";

            if (exception != null && exception.getMessage() != null) {
                if (exception.getMessage().contains("network")) {
                    errorMessage = "Error de conexión. Verifica tu internet e intenta nuevamente.";
                } else if (exception.getMessage().contains("disabled")) {
                    errorMessage = "Esta cuenta ha sido deshabilitada. Contacta con soporte.";
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(errorMessage)
                    .setPositiveButton("Entendido", null)
                    .show();
        }

        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            showLockoutMessage();
        }
    }

    private void showPasswordRecoveryHint() {
        new AlertDialog.Builder(this)
                .setTitle("¿Olvidaste tu contraseña?")
                .setMessage("Has tenido " + loginAttempts + " intentos fallidos.\n\n" +
                        "¿Deseas recuperar tu contraseña?")
                .setPositiveButton("Sí, recuperar", (dialog, which) -> {
                    mForgotPasswordTextView.performClick();
                })
                .setNegativeButton("No, intentar de nuevo", null)
                .show();
    }

    private void register() {
        String typeUser = mPref.getString("user", "");
        Class<?> registrationActivity = typeUser.equals("cliente") ?
                RegisterActivity.class : FirtsRegisterWorkerActivity.class;

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
        // Cabecera navy (mockup 01): barra de estado navy con íconos claros
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.ads_header));
        new WindowInsetsControllerCompat(window, window.getDecorView())
                .setAppearanceLightStatusBars(false);
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }
}