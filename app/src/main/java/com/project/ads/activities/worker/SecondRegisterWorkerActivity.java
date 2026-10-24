package com.project.ads.activities.worker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.project.ads.R;
import com.project.ads.helpers.ConsentHelper;
import com.project.ads.helpers.FileHashHelper;
import com.project.ads.models.Worker;
import com.project.ads.models.WorkerDocument;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.WorkerProvider;
import com.project.ads.providers.WorkerDocumentProvider;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import android.util.Log;

public class SecondRegisterWorkerActivity extends AppCompatActivity {

    private static final String TAG = "SecondRegisterWorker";

    // UI Components
    private CardView layoutHojaVida, layoutAntecedentes;
    private RadioGroup rgCertificationType;
    private RadioButton rbCartas, rbTitulos;
    private LinearLayout layoutCartasSection, layoutTitulosSection;

    // Cartas
    private CardView layoutCarta1, layoutCarta2, layoutCarta3;
    private Button btnSelectCarta1, btnSelectCarta2, btnSelectCarta3;
    private TextView tvCarta1Status, tvCarta2Status, tvCarta3Status;
    private ImageView ivCarta1Check, ivCarta2Check, ivCarta3Check;

    // Títulos
    private CardView layoutTitulo1, layoutTitulo2, layoutTitulo3;
    private Button btnSelectTitulo1, btnSelectTitulo2, btnSelectTitulo3;
    private TextView tvTitulo1Status, tvTitulo2Status, tvTitulo3Status;
    private ImageView ivTitulo1Check, ivTitulo2Check, ivTitulo3Check;

    private Button btnSelectHojaVida, btnSelectAntecedentes;
    private Button btnSubmitDocuments, btnSkip;
    private TextView tvHojaVidaStatus, tvAntecedentesStatus;
    private ImageView ivHojaVidaCheck, ivAntecedentesCheck;
    private ProgressBar progressBar;
    private TextView tvProgress;

    // Providers
    private AuthProvider mAuthProvider;
    private WorkerProvider mWorkerProvider;
    private WorkerDocumentProvider documentProvider;
    private ProgressDialog mProgressDialog;

    // Datos del usuario
    private String name, lastName, email, password, work, phone, workerId; // ⭐ AGREGADO phone
    private java.util.ArrayList<String> categories;   // slugs de los oficios (D16)
    private java.util.ArrayList<String> tasks;        // slugs de las tareas que resuelve (mockup 32)
    private String description;                        // descripción opcional (paso 2)

    // Variables para Google Sign-In
    private boolean isFromGoogleSignIn = false;
    private String googleUserId;

    // Documentos obligatorios
    private Uri hojaVidaUri, antecedentesUri;
    private String hojaVidaFileName, antecedentesFileName;

    // Documentos opcionales - Cartas
    private Uri carta1Uri, carta2Uri, carta3Uri;
    private String carta1FileName, carta2FileName, carta3FileName;

    // Documentos opcionales - Títulos
    private Uri titulo1Uri, titulo2Uri, titulo3Uri;
    private String titulo1FileName, titulo2FileName, titulo3FileName;

    private int uploadedCount = 0;
    private int totalDocuments = 0;
    private boolean isUploading = false;
    private boolean isRegistered = false;
    private String selectedCertificationType = "cartas";

    // File picker launchers
    private ActivityResultLauncher<String> hojaVidaLauncher;
    private ActivityResultLauncher<String> antecedentesLauncher;
    private ActivityResultLauncher<String> carta1Launcher, carta2Launcher, carta3Launcher;
    private ActivityResultLauncher<String> titulo1Launcher, titulo2Launcher, titulo3Launcher;

    // Image profile
    private Uri profileImageUri;
    private boolean hasProfileImage = false;
    private StorageReference mStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_register_worker);
        com.project.ads.helpers.RegisterStepsHelper.bind(this, 3, R.string.register_step3);
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        getIntentData();
        initializeViews();
        initializeProviders();
        initializeFilePickers();
        setupListeners();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        lastName = intent.getStringExtra("lastName");
        phone = intent.getStringExtra("phone"); // ⭐ NUEVO
        email = intent.getStringExtra("email");
        password = intent.getStringExtra("password");
        work = intent.getStringExtra("work");
        categories = intent.getStringArrayListExtra("categories");
        tasks = intent.getStringArrayListExtra("tasks");
        description = intent.getStringExtra("description");

        // Datos de Google Sign-In
        isFromGoogleSignIn = intent.getBooleanExtra("fromGoogleSignIn", false);
        googleUserId = intent.getStringExtra("userId");

        // Recibir imagen
        hasProfileImage = intent.getBooleanExtra("hasImage", false);
        if (hasProfileImage) {
            String imageUriString = intent.getStringExtra("imageUri");
            if (imageUriString != null) {
                profileImageUri = Uri.parse(imageUriString);
                Log.d(TAG, "Imagen de perfil recibida: " + imageUriString);
            }
        }

        if (name == null || email == null) {
            Toast.makeText(this, "Error: No se recibieron los datos de registro", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!isFromGoogleSignIn && (password == null || password.isEmpty())) {
            Toast.makeText(this, "Error: No se recibió la contraseña", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        // Layouts obligatorios
        layoutHojaVida = findViewById(R.id.layoutHojaVida);
        layoutAntecedentes = findViewById(R.id.layoutAntecedentes);

        // Radio buttons para selección
        rgCertificationType = findViewById(R.id.rgCertificationType);
        rbCartas = findViewById(R.id.rbCartas);
        rbTitulos = findViewById(R.id.rbTitulos);

        layoutCartasSection = findViewById(R.id.layoutCartasSection);
        layoutTitulosSection = findViewById(R.id.layoutTitulosSection);

        // Cartas
        layoutCarta1 = findViewById(R.id.layoutCarta1);
        layoutCarta2 = findViewById(R.id.layoutCarta2);
        layoutCarta3 = findViewById(R.id.layoutCarta3);
        btnSelectCarta1 = findViewById(R.id.btnSelectCarta1);
        btnSelectCarta2 = findViewById(R.id.btnSelectCarta2);
        btnSelectCarta3 = findViewById(R.id.btnSelectCarta3);
        tvCarta1Status = findViewById(R.id.tvCarta1Status);
        tvCarta2Status = findViewById(R.id.tvCarta2Status);
        tvCarta3Status = findViewById(R.id.tvCarta3Status);
        ivCarta1Check = findViewById(R.id.ivCarta1Check);
        ivCarta2Check = findViewById(R.id.ivCarta2Check);
        ivCarta3Check = findViewById(R.id.ivCarta3Check);

        // Títulos
        layoutTitulo1 = findViewById(R.id.layoutTitulo1);
        layoutTitulo2 = findViewById(R.id.layoutTitulo2);
        layoutTitulo3 = findViewById(R.id.layoutTitulo3);
        btnSelectTitulo1 = findViewById(R.id.btnSelectTitulo1);
        btnSelectTitulo2 = findViewById(R.id.btnSelectTitulo2);
        btnSelectTitulo3 = findViewById(R.id.btnSelectTitulo3);
        tvTitulo1Status = findViewById(R.id.tvTitulo1Status);
        tvTitulo2Status = findViewById(R.id.tvTitulo2Status);
        tvTitulo3Status = findViewById(R.id.tvTitulo3Status);
        ivTitulo1Check = findViewById(R.id.ivTitulo1Check);
        ivTitulo2Check = findViewById(R.id.ivTitulo2Check);
        ivTitulo3Check = findViewById(R.id.ivTitulo3Check);

        // Buttons obligatorios
        btnSelectHojaVida = findViewById(R.id.btnSelectHojaVida);
        btnSelectAntecedentes = findViewById(R.id.btnSelectAntecedentes);
        btnSubmitDocuments = findViewById(R.id.btnSubmitDocuments);
        btnSkip = findViewById(R.id.btnSkip);

        // Status
        tvHojaVidaStatus = findViewById(R.id.tvHojaVidaStatus);
        tvAntecedentesStatus = findViewById(R.id.tvAntecedentesStatus);
        ivHojaVidaCheck = findViewById(R.id.ivHojaVidaCheck);
        ivAntecedentesCheck = findViewById(R.id.ivAntecedentesCheck);

        // Progress
        progressBar = findViewById(R.id.progressBar);
        tvProgress = findViewById(R.id.tvProgress);

        mProgressDialog = new ProgressDialog(this);

        // Mostrar cartas por defecto
        layoutCartasSection.setVisibility(View.VISIBLE);
        layoutTitulosSection.setVisibility(View.GONE);
    }

    private void initializeProviders() {
        mAuthProvider = new AuthProvider();
        mWorkerProvider = new WorkerProvider();
        documentProvider = new WorkerDocumentProvider();
        mStorageReference = FirebaseStorage.getInstance().getReference();
    }

    private void initializeFilePickers() {
        hojaVidaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !isRepeatedFile(uri, hojaVidaUri)) {
                        hojaVidaUri = uri;
                        hojaVidaFileName = "hoja_vida_" + System.currentTimeMillis() + ".pdf";
                        updateDocumentStatus(tvHojaVidaStatus, ivHojaVidaCheck, getString(R.string.docs_selected), true);
                        checkSubmitButton();
                    }
                }
        );

        antecedentesLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !isRepeatedFile(uri, antecedentesUri)) {
                        antecedentesUri = uri;
                        antecedentesFileName = "antecedentes_" + System.currentTimeMillis() + ".pdf";
                        updateDocumentStatus(tvAntecedentesStatus, ivAntecedentesCheck, getString(R.string.docs_selected), true);
                        checkSubmitButton();
                    }
                }
        );

        // Launchers para Cartas
        carta1Launcher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !isRepeatedFile(uri, carta1Uri)) {
                        carta1Uri = uri;
                        carta1FileName = "carta_recomendacion_1_" + System.currentTimeMillis() + ".pdf";
                        updateDocumentStatus(tvCarta1Status, ivCarta1Check, getString(R.string.docs_selected), true);
                        checkSubmitButton();
                    }
                }
        );

        carta2Launcher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !isRepeatedFile(uri, carta2Uri)) {
                        carta2Uri = uri;
                        carta2FileName = "carta_recomendacion_2_" + System.currentTimeMillis() + ".pdf";
                        updateDocumentStatus(tvCarta2Status, ivCarta2Check, getString(R.string.docs_selected), true);
                        checkSubmitButton();
                    }
                }
        );

        carta3Launcher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !isRepeatedFile(uri, carta3Uri)) {
                        carta3Uri = uri;
                        carta3FileName = "carta_recomendacion_3_" + System.currentTimeMillis() + ".pdf";
                        updateDocumentStatus(tvCarta3Status, ivCarta3Check, getString(R.string.docs_selected), true);
                        checkSubmitButton();
                    }
                }
        );

        // Launchers para Títulos
        titulo1Launcher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !isRepeatedFile(uri, titulo1Uri)) {
                        titulo1Uri = uri;
                        titulo1FileName = "titulo_certificado_1_" + System.currentTimeMillis() + ".pdf";
                        updateDocumentStatus(tvTitulo1Status, ivTitulo1Check, getString(R.string.docs_selected), true);
                        checkSubmitButton();
                    }
                }
        );

        titulo2Launcher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !isRepeatedFile(uri, titulo2Uri)) {
                        titulo2Uri = uri;
                        titulo2FileName = "titulo_certificado_2_" + System.currentTimeMillis() + ".pdf";
                        updateDocumentStatus(tvTitulo2Status, ivTitulo2Check, getString(R.string.docs_selected), true);
                        checkSubmitButton();
                    }
                }
        );

        titulo3Launcher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !isRepeatedFile(uri, titulo3Uri)) {
                        titulo3Uri = uri;
                        titulo3FileName = "titulo_certificado_3_" + System.currentTimeMillis() + ".pdf";
                        updateDocumentStatus(tvTitulo3Status, ivTitulo3Check, getString(R.string.docs_selected), true);
                        checkSubmitButton();
                    }
                }
        );
    }

    /** El mismo PDF, o uno con el mismo nombre, ya está en otra casilla: se avisa y no se usa (DEF-23). */
    private boolean isRepeatedFile(Uri uri, Uri currentSlot) {
        String hash = FileHashHelper.sha256(getContentResolver(), uri);
        String name = FileHashHelper.displayName(getContentResolver(), uri);
        Uri[] slots = {hojaVidaUri, antecedentesUri, carta1Uri, carta2Uri, carta3Uri,
                titulo1Uri, titulo2Uri, titulo3Uri};
        for (Uri other : slots) {
            if (other == null || other == currentSlot) {
                continue;
            }
            if (hash != null && hash.equals(FileHashHelper.sha256(getContentResolver(), other))) {
                Toast.makeText(this, R.string.mydocs_same_file, Toast.LENGTH_LONG).show();
                return true;
            }
            String otherName = FileHashHelper.displayName(getContentResolver(), other);
            if (name != null && otherName != null && name.trim().equalsIgnoreCase(otherName.trim())) {
                Toast.makeText(this, getString(R.string.mydocs_same_name, name), Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }

    private void setupListeners() {
        // Obligatorios
        btnSelectHojaVida.setOnClickListener(v -> hojaVidaLauncher.launch("application/pdf"));
        btnSelectAntecedentes.setOnClickListener(v -> antecedentesLauncher.launch("application/pdf"));

        // Cartas
        btnSelectCarta1.setOnClickListener(v -> carta1Launcher.launch("application/pdf"));
        btnSelectCarta2.setOnClickListener(v -> carta2Launcher.launch("application/pdf"));
        btnSelectCarta3.setOnClickListener(v -> carta3Launcher.launch("application/pdf"));

        // Títulos
        btnSelectTitulo1.setOnClickListener(v -> titulo1Launcher.launch("application/pdf"));
        btnSelectTitulo2.setOnClickListener(v -> titulo2Launcher.launch("application/pdf"));
        btnSelectTitulo3.setOnClickListener(v -> titulo3Launcher.launch("application/pdf"));

        // Radio Group para cambiar entre cartas y títulos
        rgCertificationType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCartas) {
                selectedCertificationType = "cartas";
                layoutCartasSection.setVisibility(View.VISIBLE);
                layoutTitulosSection.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbTitulos) {
                selectedCertificationType = "titulos";
                layoutCartasSection.setVisibility(View.GONE);
                layoutTitulosSection.setVisibility(View.VISIBLE);
            }
            checkSubmitButton();
        });

        btnSubmitDocuments.setOnClickListener(v -> showConfirmationDialog());

        btnSkip.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Omitir documentos")
                    .setMessage("Podrás subir tus documentos más tarde desde tu perfil. ¿Deseas continuar?")
                    .setPositiveButton("Sí, continuar", (dialog, which) -> registerUserAndFinish())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void showConfirmationDialog() {
        totalDocuments = 0;
        if (hojaVidaUri != null) totalDocuments++;
        if (antecedentesUri != null) totalDocuments++;

        int certCount = 0;
        if (selectedCertificationType.equals("cartas")) {
            if (carta1Uri != null) { totalDocuments++; certCount++; }
            if (carta2Uri != null) { totalDocuments++; certCount++; }
            if (carta3Uri != null) { totalDocuments++; certCount++; }
        } else {
            if (titulo1Uri != null) { totalDocuments++; certCount++; }
            if (titulo2Uri != null) { totalDocuments++; certCount++; }
            if (titulo3Uri != null) { totalDocuments++; certCount++; }
        }

        if (totalDocuments == 0) {
            showMessage("Por favor selecciona al menos un documento");
            return;
        }

        String certType = selectedCertificationType.equals("cartas") ? "cartas de recomendación" : "títulos/certificados";
        String message = "Se subirán " + totalDocuments + " documento(s).\n\n";

        if (selectedCertificationType.equals("cartas")) {
            if (certCount >= 3) {
                message += "¡Excelente! Has completado los 3 " + certType + " requeridos.";
            } else if (certCount > 0) {
                message += "Has seleccionado " + certCount + " " + certType + ". Recuerda que se requieren al menos 3.";
            }
        } else {
            if (certCount >= 1) {
                message += "Has seleccionado " + certCount + " " + certType + ".";
            } else {
                message += "Debes seleccionar al menos 1 " + certType + ".";
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar registro")
                .setMessage(message)
                .setPositiveButton("Registrar y subir", (dialog, which) -> startRegistrationProcess())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private Worker newWorker() {
        Worker worker = new Worker(workerId, name, lastName, email, work, phone, null);
        worker.setCategories(categories);
        if (tasks != null && !tasks.isEmpty()) worker.setTasks(tasks);
        if (description != null && !description.isEmpty()) worker.setDescription(description);
        return worker;
    }

    private void startRegistrationProcess() {
        if (isUploading) return;

        // Si viene de Google Sign-In, NO crear cuenta nueva
        if (isFromGoogleSignIn) {
            workerId = googleUserId;
            Worker worker = newWorker();
            createWorkerInDatabase(worker);
        } else {
            // Registro normal con email y contraseña
            mProgressDialog.setMessage("Creando cuenta...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();

            mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        Worker worker = newWorker();
                        createWorkerInDatabase(worker);
                    } else {
                        mProgressDialog.dismiss();
                        showMessage("Error al crear cuenta: " + task.getException().getMessage());
                    }
                }
            });
        }
    }

    private void createWorkerInDatabase(Worker worker) {
        if (isFromGoogleSignIn) {
            mProgressDialog.setMessage("Guardando información...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        } else {
            mProgressDialog.setMessage("Guardando información...");
        }

        // ===== SI HAY IMAGEN, SUBIRLA PRIMERO =====
        if (hasProfileImage && profileImageUri != null) {
            uploadProfileImageAndCreateWorker(worker);
        } else {
            // Si no hay imagen, crear worker sin imagen
            saveWorkerToDatabase(worker, null);
        }
    }

    private void uploadProfileImageAndCreateWorker(Worker worker) {
        mProgressDialog.setMessage("Subiendo foto de perfil...");

        // Crear referencia única para la imagen
        String imageName = "profile_" + workerId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = mStorageReference.child("worker_profiles").child(imageName);

        // Subir imagen
        UploadTask uploadTask = imageRef.putFile(profileImageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Obtener URL de descarga
            imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String imageUrl = downloadUri.toString();
                Log.d(TAG, "Imagen subida exitosamente: " + imageUrl);

                // Guardar worker con URL de imagen
                saveWorkerToDatabase(worker, imageUrl);

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error al obtener URL de imagen", e);
                mProgressDialog.dismiss();
                showMessage("Error al obtener URL de imagen: " + e.getMessage());
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error al subir imagen", e);
            mProgressDialog.dismiss();
            showMessage("Error al subir imagen. Se creará el perfil sin foto.");

            // Crear worker sin imagen si falla la subida
            saveWorkerToDatabase(worker, null);
        }).addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            mProgressDialog.setMessage("Subiendo foto de perfil... " + (int) progress + "%");
        });
    }

    private void saveWorkerToDatabase(Worker worker, String imageUrl) {
        mProgressDialog.setMessage("Guardando información...");

        // Si hay URL de imagen, agregarla al worker
        if (imageUrl != null && !imageUrl.isEmpty()) {
            worker.setImage(imageUrl);
            Log.d(TAG, "Worker con imagen: " + imageUrl);
        } else {
            worker.setImage("default");
            Log.d(TAG, "Worker sin imagen");
        }

        // ⭐ ASEGURAR QUE EL TELÉFONO SE GUARDE CORRECTAMENTE
        if (phone != null && !phone.isEmpty()) {
            worker.setPhone(phone);
            Log.d(TAG, "Worker con teléfono registrado");
        }

        worker.setConsentAcceptedAt(getIntent().getLongExtra(ConsentHelper.EXTRA_ACCEPTED_AT, 0));

        mWorkerProvider.create(worker).addOnCompleteListener(task -> {
            mProgressDialog.dismiss();

            if (task.isSuccessful()) {
                isRegistered = true;
                Toast.makeText(SecondRegisterWorkerActivity.this,
                        "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show();

                if (hasDocumentsToUpload()) {
                    startUploadProcess();
                } else {
                    finishRegistration();
                }
            } else {
                showMessage("Error al guardar datos: " + task.getException().getMessage());
                // Solo eliminar cuenta de Auth si NO viene de Google
                if (!isFromGoogleSignIn && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    FirebaseAuth.getInstance().getCurrentUser().delete();
                }
            }
        });
    }

    private boolean hasDocumentsToUpload() {
        return hojaVidaUri != null || antecedentesUri != null ||
                carta1Uri != null || carta2Uri != null || carta3Uri != null ||
                titulo1Uri != null || titulo2Uri != null || titulo3Uri != null;
    }

    private void startUploadProcess() {
        if (isUploading) return;

        isUploading = true;
        uploadedCount = 0;

        setUIUploading(true);
        updateProgress(0);

        // Upload obligatorios
        if (hojaVidaUri != null) {
            uploadDocument(WorkerDocument.TYPE_HOJA_VIDA, WorkerDocument.CATEGORY_HOJA_VIDA,
                    null, hojaVidaUri, hojaVidaFileName, 0, "Hoja de Vida");
        }

        if (antecedentesUri != null) {
            uploadDocument(WorkerDocument.TYPE_ANTECEDENTES, WorkerDocument.CATEGORY_ANTECEDENTES,
                    null, antecedentesUri, antecedentesFileName, 0, "Antecedentes Judiciales");
        }

        // Upload según tipo seleccionado
        if (selectedCertificationType.equals("cartas")) {
            if (carta1Uri != null) {
                uploadDocument(WorkerDocument.TYPE_CARTA_RECOMENDACION, WorkerDocument.CATEGORY_CERTIFICACIONES,
                        WorkerDocument.SUBCATEGORY_CARTAS, carta1Uri, carta1FileName, 1, "Carta de Recomendación 1");
            }
            if (carta2Uri != null) {
                uploadDocument(WorkerDocument.TYPE_CARTA_RECOMENDACION, WorkerDocument.CATEGORY_CERTIFICACIONES,
                        WorkerDocument.SUBCATEGORY_CARTAS, carta2Uri, carta2FileName, 2, "Carta de Recomendación 2");
            }
            if (carta3Uri != null) {
                uploadDocument(WorkerDocument.TYPE_CARTA_RECOMENDACION, WorkerDocument.CATEGORY_CERTIFICACIONES,
                        WorkerDocument.SUBCATEGORY_CARTAS, carta3Uri, carta3FileName, 3, "Carta de Recomendación 3");
            }
        } else {
            if (titulo1Uri != null) {
                uploadDocument(WorkerDocument.TYPE_TITULO, WorkerDocument.CATEGORY_CERTIFICACIONES,
                        WorkerDocument.SUBCATEGORY_TITULOS, titulo1Uri, titulo1FileName, 1, "Título/Certificado 1");
            }
            if (titulo2Uri != null) {
                uploadDocument(WorkerDocument.TYPE_TITULO, WorkerDocument.CATEGORY_CERTIFICACIONES,
                        WorkerDocument.SUBCATEGORY_TITULOS, titulo2Uri, titulo2FileName, 2, "Título/Certificado 2");
            }
            if (titulo3Uri != null) {
                uploadDocument(WorkerDocument.TYPE_TITULO, WorkerDocument.CATEGORY_CERTIFICACIONES,
                        WorkerDocument.SUBCATEGORY_TITULOS, titulo3Uri, titulo3FileName, 3, "Título/Certificado 3");
            }
        }
    }

    private void uploadDocument(String documentType, String category, String subcategory,
                                Uri fileUri, String fileName, int orden, String description) {
        WorkerDocument document = new WorkerDocument();
        document.setId(documentProvider.generateDocumentId());
        document.setWorkerId(workerId);
        document.setDocumentType(documentType);
        document.setCategory(category);
        if (subcategory != null) {
            document.setSubcategory(subcategory);
        }
        document.setFileName(fileName);
        document.setFileType("application/pdf");
        document.setOrden(orden);
        document.setFileHash(FileHashHelper.sha256(getContentResolver(), fileUri));
        document.setOriginalFileName(FileHashHelper.displayName(getContentResolver(), fileUri));
        document.setDescription(description);

        documentProvider.uploadDocument(document, fileUri)
                .addOnSuccessListener(aVoid -> {
                    uploadedCount++;
                    updateProgress(uploadedCount);

                    if (uploadedCount == totalDocuments) {
                        onAllDocumentsUploaded();
                    }
                })
                .addOnFailureListener(e -> {
                    setUIUploading(false);
                    isUploading = false;
                    showMessage("Error al subir " + description + ": " + e.getMessage());
                });
    }

    private void onAllDocumentsUploaded() {
        isUploading = false;

        documentProvider.updateWorkerVerificationStatus(workerId, "documents_submitted")
                .addOnSuccessListener(aVoid -> {
                    setUIUploading(false);

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("¡Registro completado!")
                            .setMessage("Tu cuenta ha sido creada y tus documentos han sido enviados exitosamente. Serán revisados por nuestro equipo.")
                            .setPositiveButton("Continuar", (dialog, which) -> finishRegistration())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    setUIUploading(false);
                    showMessage("Documentos subidos pero hubo un error actualizando el estado");
                    finishRegistration();
                });
    }

    private void registerUserAndFinish() {
        if (isRegistered) {
            finishRegistration();
            return;
        }

        if (isFromGoogleSignIn) {
            workerId = googleUserId;
            Worker worker = newWorker();

            mProgressDialog.setMessage("Guardando información...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();

            // ===== CAMBIAR PARA MANEJAR IMAGEN =====
            if (hasProfileImage && profileImageUri != null) {
                uploadProfileImageAndCreateWorker(worker);
            } else {
                saveWorkerToDatabase(worker, null);
            }

        } else {
            // Registro normal
            mProgressDialog.setMessage("Creando cuenta...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();

            mAuthProvider.register(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Worker worker = newWorker();

                    // ===== CAMBIAR PARA MANEJAR IMAGEN =====
                    if (hasProfileImage && profileImageUri != null) {
                        uploadProfileImageAndCreateWorker(worker);
                    } else {
                        saveWorkerToDatabase(worker, null);
                    }
                } else {
                    mProgressDialog.dismiss();
                    showMessage("Error al crear cuenta: " + task.getException().getMessage());
                }
            });
        }
    }

    private void finishRegistration() {
        // Mockups 34-36: estado de verificación; desde ahí sigue al inicio
        Intent intent = new Intent(this, VerificationStatusActivity.class);
        intent.putExtra(VerificationStatusActivity.EXTRA_FROM_REGISTRATION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateProgress(int uploaded) {
        int percentage = (int) ((uploaded / (float) totalDocuments) * 100);
        progressBar.setProgress(percentage);
        tvProgress.setText(uploaded + " de " + totalDocuments + " documentos subidos");
    }

    private void updateDocumentStatus(TextView statusView, ImageView checkView, String status, boolean selected) {
        statusView.setText(status);
        checkView.setVisibility(selected ? View.VISIBLE : View.GONE);
        // Fila con archivo: borde naranja oscuro y ficha naranja clara (mockup 33)
        View parent = (View) statusView.getParent();
        while (parent != null && !(parent instanceof com.google.android.material.card.MaterialCardView)) {
            parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
        }
        if (parent != null) {
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) parent;
            card.setStrokeColor(ContextCompat.getColor(this, selected ? R.color.ads_brand_text : R.color.ads_line));
            card.setStrokeWidth(getResources().getDimensionPixelSize(selected ? R.dimen.stroke_focus : R.dimen.stroke_control));
            View tile = ((android.view.ViewGroup) ((android.view.ViewGroup) card.getChildAt(0))).getChildAt(0);
            androidx.core.view.ViewCompat.setBackgroundTintList(tile, selected
                    ? android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ads_chip_bg)) : null);
        }
    }

    private void checkSubmitButton() {
        boolean hasObligatorios = hojaVidaUri != null || antecedentesUri != null;
        boolean hasCertificaciones = false;

        if (selectedCertificationType.equals("cartas")) {
            hasCertificaciones = carta1Uri != null || carta2Uri != null || carta3Uri != null;
        } else {
            hasCertificaciones = titulo1Uri != null || titulo2Uri != null || titulo3Uri != null;
        }

        btnSubmitDocuments.setEnabled((hasObligatorios || hasCertificaciones) && !isUploading);
    }

    private void setUIUploading(boolean uploading) {
        btnSelectHojaVida.setEnabled(!uploading);
        btnSelectAntecedentes.setEnabled(!uploading);
        btnSelectCarta1.setEnabled(!uploading);
        btnSelectCarta2.setEnabled(!uploading);
        btnSelectCarta3.setEnabled(!uploading);
        btnSelectTitulo1.setEnabled(!uploading);
        btnSelectTitulo2.setEnabled(!uploading);
        btnSelectTitulo3.setEnabled(!uploading);
        btnSubmitDocuments.setEnabled(!uploading);
        btnSkip.setEnabled(!uploading);
        rgCertificationType.setEnabled(!uploading);

        progressBar.setVisibility(uploading ? View.VISIBLE : View.GONE);
        tvProgress.setVisibility(uploading ? View.VISIBLE : View.GONE);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (isUploading) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Subida en progreso")
                    .setMessage("¿Deseas cancelar el registro?")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                        super.onBackPressed();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else if (isRegistered) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Registro completado")
                    .setMessage("Tu cuenta ya ha sido creada. Por favor continúa al inicio.")
                    .setPositiveButton("Ir al inicio", (dialog, which) -> finishRegistration())
                    .setCancelable(false)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        super.onDestroy();
    }
}