package com.project.ads.activities.worker;

import android.Manifest;
import android.content.Intent;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import com.project.ads.activities.LegalActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.project.ads.R;
import com.project.ads.helpers.ConsentHelper;
import com.project.ads.activities.LoginActivity;
import com.project.ads.helpers.RegisterStepsHelper;

import com.project.ads.models.api.CatalogData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class FirtsRegisterWorkerActivity extends AppCompatActivity {

    private static final String TAG = "FirtsRegisterWorker";
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MIN_PHONE_LENGTH = 7;
    private static final int MAX_PHONE_LENGTH = 10;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]+$");

    private Button mButtonContinue;
    private TextInputEditText mTextInputNames, mTextInputLastNames, mTextInputEmail, mTextInputPassword, mTextInputPhone;
    private TextInputLayout mTextInputLayoutNames, mTextInputLayoutLastNames, mTextInputLayoutEmail, mTextInputLayoutPassword, mTextInputLayoutPhone;
    private CheckBox mCheckBoxTerms;
    private CheckBox mCheckBoxData;
    private TextView mTextViewTerms;
    private CircleImageView mCircleImageProfile;
    private ImageView mImageViewAddPhoto;

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
        setContentView(R.layout.activity_first_register_worker);
        RegisterStepsHelper.bind(this, 1, R.string.register_step1);
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_register_login).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        initializeViews();
        handleGoogleSignInData();
        setupTermsAndConditionsLink();
        setupClickListeners();
    }

    @Override
    public void onBackPressed() {
        if (isFromGoogleSignIn) {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar registro")
                    .setMessage("Si cancelas ahora, no se tendrá en cuenta el registro. ¿Estás seguro?")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                        deleteUserAndSignOut();
                    })
                    .setNegativeButton("Continuar registro", null)
                    .setCancelable(false)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void deleteUserAndSignOut() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
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

    private void initializeViews() {
        mButtonContinue = findViewById(R.id.continue_register);
        mTextInputNames = findViewById(R.id.names_user_client);
        mTextInputLastNames = findViewById(R.id.last_names);
        mTextInputEmail = findViewById(R.id.emailAddress);
        mTextInputPassword = findViewById(R.id.password);
        mTextInputPhone = findViewById(R.id.phone_number); // ⭐ NUEVO
        mCheckBoxTerms = findViewById(R.id.checkBox2);
        mCheckBoxData = findViewById(R.id.checkbox_data);
        mTextViewTerms = findViewById(R.id.textViewTerms);
        mCircleImageProfile = findViewById(R.id.circleImageProfile);
        mImageViewAddPhoto = findViewById(R.id.imageViewAddPhoto);

        // Inicializar TextInputLayouts
        mTextInputLayoutNames = findViewById(R.id.til_names);
        mTextInputLayoutLastNames = findViewById(R.id.til_last_names);
        mTextInputLayoutEmail = findViewById(R.id.til_email);
        mTextInputLayoutPassword = findViewById(R.id.til_password);
        mTextInputLayoutPhone = findViewById(R.id.til_phone); // ⭐ NUEVO
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
                mTextInputLayoutEmail.setEnabled(false);
                mTextInputLayoutEmail.setHelperText("Autenticado con Google");
            }

            findViewById(R.id.layout_password).setVisibility(View.GONE);
            mButtonContinue.setText(R.string.register_continue);

            showToast("Completando registro con Google");
        }
    }

    private void setupClickListeners() {
        mButtonContinue.setOnClickListener(v -> {
            mButtonContinue.setEnabled(false);
            try {
                validateAndContinue();
            } finally {
                mButtonContinue.postDelayed(() -> mButtonContinue.setEnabled(true), 1000);
            }
        });

        // Listeners para seleccionar foto de perfil
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
        mTextViewTerms.setText(linkText(R.string.register_terms_text, R.string.register_terms_link, LegalActivity.DOCUMENT_TERMS));
        mTextViewTerms.setMovementMethod(LinkMovementMethod.getInstance());
        TextView data = findViewById(R.id.textViewData);
        data.setText(linkText(R.string.register_data_text, R.string.register_data_link, LegalActivity.DOCUMENT_PRIVACY));
        data.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /** "Acepto los Términos y Condiciones" con el enlace en negrita sobre la parte indicada. */
    private SpannableString linkText(int textRes, int linkRes, String document) {
        String text = getString(textRes);
        String link = getString(linkRes);
        SpannableString spannable = new SpannableString(text);
        int start = Math.max(0, text.indexOf(link));
        int end = Math.min(text.length(), start + link.length());
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Términos o Aviso de privacidad, dentro de la app
                startActivity(LegalActivity.intent(FirtsRegisterWorkerActivity.this, document));
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }


    private void validateAndContinue() {
        clearErrors();

        String name = mTextInputNames.getText().toString().trim();
        String lastName = mTextInputLastNames.getText().toString().trim();
        String phone = mTextInputPhone.getText().toString().trim(); // ⭐ NUEVO
        String email = mTextInputEmail.getText().toString().trim().toLowerCase();
        String password = isFromGoogleSignIn ? "" : mTextInputPassword.getText().toString();

        boolean isValid = true;

        if (!validateName(name)) {
            mTextInputLayoutNames.setError("Nombre inválido (solo letras, máx. 50 caracteres)");
            isValid = false;
        }

        if (!validateName(lastName)) {
            mTextInputLayoutLastNames.setError("Apellido inválido (solo letras, máx. 50 caracteres)");
            isValid = false;
        }

        // ⭐ VALIDACIÓN DE TELÉFONO
        if (!validatePhone(phone)) {
            mTextInputLayoutPhone.setError("Teléfono inválido (7-10 dígitos)");
            isValid = false;
        }

        if (!validateEmail(email)) {
            mTextInputLayoutEmail.setError("Ingrese un correo electrónico válido");
            isValid = false;
        }

        if (!isFromGoogleSignIn && !validatePassword(password)) {
            mTextInputLayoutPassword.setError("La contraseña debe tener al menos 8 caracteres, incluir mayúsculas, minúsculas y números");
            isValid = false;
        }

        if (!mCheckBoxTerms.isChecked()) {
            showToast("Debe aceptar los términos y condiciones");
            isValid = false;
        } else if (!mCheckBoxData.isChecked()) {
            showToast(getString(R.string.register_error_data));
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Paso 2 (oficios); de ahí los datos siguen a SecondRegisterWorkerActivity
        Intent intent = new Intent(FirtsRegisterWorkerActivity.this, WorkerSkillsActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("lastName", lastName);
        intent.putExtra("phone", phone);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        // Las dos casillas ya se validaron: momento de la aceptación (D18)
        intent.putExtra(ConsentHelper.EXTRA_ACCEPTED_AT, System.currentTimeMillis());

        // Pasar información de la imagen
        if (mImageSelected && mImageUri != null) {
            intent.putExtra("imageUri", mImageUri.toString());
            intent.putExtra("hasImage", true);
        } else {
            intent.putExtra("hasImage", false);
        }

        // Pasar datos de Google Sign-In si aplica
        intent.putExtra("fromGoogleSignIn", isFromGoogleSignIn);
        if (isFromGoogleSignIn) {
            intent.putExtra("userId", googleUserId);
        }

        startActivity(intent);
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

    private boolean validatePhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return false;
        }

        // Validar que solo contenga números
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return false;
        }

        // Validar longitud
        int length = phone.length();
        return length >= MIN_PHONE_LENGTH && length <= MAX_PHONE_LENGTH;
    }

    private boolean validatePassword(String password) {
        return !TextUtils.isEmpty(password) &&
                password.length() >= MIN_PASSWORD_LENGTH &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*");
    }

    private void clearErrors() {
        mTextInputLayoutNames.setError(null);
        mTextInputLayoutLastNames.setError(null);
        mTextInputLayoutPhone.setError(null);
        mTextInputLayoutEmail.setError(null);
        if (!isFromGoogleSignIn) {
            mTextInputLayoutPassword.setError(null);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}