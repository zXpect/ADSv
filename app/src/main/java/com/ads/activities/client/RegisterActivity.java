package com.ads.activities.client;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.project.ads.R;
import com.ads.activities.TermsConditionsActivity;
import com.ads.includes.MyToolbar;
import com.ads.models.Client;
import com.ads.providers.AuthProvider;
import com.ads.providers.ClientProvider;

import java.io.IOException;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 50;
    private static final int MAX_NAME_LENGTH = 50;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");
    private static final String TERMS_URL = "https://terminosycondicionesads.netlify.app";

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
    private TextView mTextViewTerms;
    private CircleImageView mCircleImageProfile;
    private ImageView mImageViewAddPhoto;

    // TextInputLayouts para mostrar errores
    private TextInputLayout mTextInputLayoutNames;
    private TextInputLayout mTextInputLayoutLastNames;
    private TextInputLayout mTextInputLayoutEmail;
    private TextInputLayout mTextInputLayoutPassword;

    // Variables para Google Sign-In
    private boolean isFromGoogleSignIn = false;
    private String googleUserId;
    private String googleEmail;

    // Variable para la imagen seleccionada
    private Uri mImageUri;
    private boolean mImageSelected = false;

    // Launcher para seleccionar imagen de galería
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleSelectedImage(imageUri);
                    }
                }
            });

    // Launcher para tomar foto con cámara
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        mCircleImageProfile.setImageBitmap(imageBitmap);
                        mImageUri = getImageUriFromBitmap(imageBitmap);
                        mImageSelected = true;
                    }
                }
            });

    // Launcher para permisos de cámara
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    showToast("Permiso de cámara denegado");
                }
            });

    // Launcher para permisos de galería (Android 13+)
    private final ActivityResultLauncher<String> galleryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    showToast("Permiso de galería denegado");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeComponents();
        setupStatusBar();
        handleGoogleSignInData();
        setupClickListeners();
        setupTermsAndConditionsLink();
    }

    @Override
    public void onBackPressed() {
        if (isFromGoogleSignIn) {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar registro")
                    .setMessage("Si cancelas ahora, no se tendrá en cuenta el progreso de registro. ¿Estás seguro?")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> deleteUserAndSignOut())
                    .setNegativeButton("Continuar registro", null)
                    .setCancelable(false)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private void deleteUserAndSignOut() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Usuario eliminado correctamente");
                } else {
                    Log.e(TAG, "Error al eliminar usuario", task.getException());
                }

                auth.signOut();

                com.google.android.gms.auth.api.signin.GoogleSignInOptions gso =
                        new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build();

                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut()
                        .addOnCompleteListener(this, task1 -> {
                            showToast("Registro cancelado");
                            finish();
                        });
            });
        } else {
            finish();
        }
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
        mTextViewTerms = findViewById(R.id.textViewTerms);
        mCircleImageProfile = findViewById(R.id.circleImageProfile);
        mImageViewAddPhoto = findViewById(R.id.imageViewAddPhoto);

        // Initialize TextInputLayouts
        mTextInputLayoutNames = findViewById(R.id.textInputLayoutNames);
        mTextInputLayoutLastNames = findViewById(R.id.textInputLayoutLastNames);
        mTextInputLayoutEmail = findViewById(R.id.textInputLayoutEmail);
        mTextInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);
    }

    private void handleGoogleSignInData() {
        Intent intent = getIntent();
        isFromGoogleSignIn = intent.getBooleanExtra("fromGoogleSignIn", false);

        if (isFromGoogleSignIn) {
            googleUserId = intent.getStringExtra("userId");
            googleEmail = intent.getStringExtra("email");
            String name = intent.getStringExtra("name");
            String lastName = intent.getStringExtra("lastName");

            if (name != null) {
                mTextInputNames.setText(name);
            }
            if (lastName != null) {
                mTextInputLastNames.setText(lastName);
            }
            if (googleEmail != null) {
                mTextInputEmail.setText(googleEmail);
                mTextInputEmail.setEnabled(false);
            }

            mTextInputLayoutPassword.setVisibility(View.GONE);
            mButtonRegister.setText("Completar Registro");
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private void setupClickListeners() {
        mButtonRegister.setOnClickListener(v -> {
            mButtonRegister.setEnabled(false);
            try {
                clickRegister();
            } finally {
                mButtonRegister.postDelayed(() -> mButtonRegister.setEnabled(true), 1000);
            }
        });

        // Listener para seleccionar foto de perfil
        mCircleImageProfile.setOnClickListener(v -> showImagePickerDialog());
        mImageViewAddPhoto.setOnClickListener(v -> showImagePickerDialog());
    }

    private void showImagePickerDialog() {
        String[] options = {"Tomar foto", "Seleccionar de galería", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar foto de perfil");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    checkCameraPermission();
                    break;
                case 1:
                    checkGalleryPermission();
                    break;
                case 2:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            showToast("No se encontró aplicación de cámara");
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void handleSelectedImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            mCircleImageProfile.setImageBitmap(bitmap);
            mImageUri = imageUri;
            mImageSelected = true;
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar imagen", e);
            showToast("Error al cargar la imagen");
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "profile_" + System.currentTimeMillis(),
                null
        );
        return Uri.parse(path);
    }

    private void setupTermsAndConditionsLink() {
        SpannableString spannableString = new SpannableString("Términos y Condiciones");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openTermsAndConditions();
            }
        };
        spannableString.setSpan(clickableSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTextViewTerms.setText(spannableString);
        mTextViewTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void openTermsAndConditions() {
        Intent intent = new Intent(RegisterActivity.this, TermsConditionsActivity.class);
        intent.putExtra("terms_url", TERMS_URL);
        startActivity(intent);
    }

    private void clickRegister() {
        clearErrors();

        String name = sanitizeInput(mTextInputNames.getText().toString());
        String lastName = sanitizeInput(mTextInputLastNames.getText().toString());
        String email = sanitizeInput(mTextInputEmail.getText().toString().toLowerCase());
        String password = isFromGoogleSignIn ? "" : mTextInputPassword.getText().toString();

        if (!validateFields(name, lastName, email, password)) {
            return;
        }

        if (!mCheckBoxTerms.isChecked()) {
            showToast("Debe aceptar los términos y condiciones");
            return;
        }

        if (isFromGoogleSignIn) {
            createClientFromGoogle(name, lastName, email);
        } else {
            register(name, lastName, email, password);
        }
    }

    private boolean validateFields(String name, String lastName, String email, String password) {
        boolean isValid = true;

        if (!validateName(name)) {
            mTextInputLayoutNames.setError("Nombre inválido (solo letras, máx. 50 caracteres)");
            isValid = false;
        }

        if (!validateName(lastName)) {
            mTextInputLayoutLastNames.setError("Apellido inválido (solo letras, máx. 50 caracteres)");
            isValid = false;
        }

        if (!validateEmail(email)) {
            mTextInputLayoutEmail.setError("Email inválido");
            isValid = false;
        }

        if (!isFromGoogleSignIn && !validatePassword(password)) {
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
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*");
    }

    private String sanitizeInput(String input) {
        return input != null ? input.trim() : "";
    }

    private void clearErrors() {
        mTextInputLayoutNames.setError(null);
        mTextInputLayoutLastNames.setError(null);
        mTextInputLayoutEmail.setError(null);
        if (!isFromGoogleSignIn) {
            mTextInputLayoutPassword.setError(null);
        }
    }

    private void createClientFromGoogle(String name, String lastName, String email) {
        Client client = new Client(googleUserId, name, lastName, email);

        if (mImageSelected && mImageUri != null) {
            // Crear cliente con imagen
            mClientProvider.createWithImage(client, mImageUri)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            showToast("Registro completado con éxito");
                            navigateToHome();
                        } else {
                            Log.e(TAG, "Error creating client from Google", task.getException());
                            showToast("Error al completar el registro. Por favor, intente nuevamente");
                        }
                    });
        } else {
            // Crear cliente sin imagen
            mClientProvider.create(client)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            showToast("Registro completado con éxito");
                            navigateToHome();
                        } else {
                            Log.e(TAG, "Error creating client from Google", task.getException());
                            showToast("Error al completar el registro. Por favor, intente nuevamente");
                        }
                    });
        }
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
        if (mImageSelected && mImageUri != null) {
            // Crear cliente con imagen
            mClientProvider.createWithImage(client, mImageUri)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            showToast("El registro se realizó con éxito");
                            navigateToHome();
                        } else {
                            Log.e(TAG, "Error creating client", task.getException());
                            showToast("Error al crear el cliente. Por favor, intente nuevamente");
                            FirebaseAuth.getInstance().getCurrentUser().delete();
                        }
                    });
        } else {
            // Crear cliente sin imagen
            mClientProvider.create(client)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            showToast("El registro se realizó con éxito");
                            navigateToHome();
                        } else {
                            Log.e(TAG, "Error creating client", task.getException());
                            showToast("Error al crear el cliente. Por favor, intente nuevamente");
                            FirebaseAuth.getInstance().getCurrentUser().delete();
                        }
                    });
        }
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