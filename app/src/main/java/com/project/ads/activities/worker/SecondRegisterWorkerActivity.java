package com.project.ads.activities.worker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;

import com.project.ads.helpers.RegistrationProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Registro de trabajador, paso 3: documentos agrupados por requisito. Al enviar,
 * una ventana de carga muestra que se crea la cuenta, se suben los documentos y
 * se envían a revisión, y bloquea la pantalla mientras tanto.
 */
public class SecondRegisterWorkerActivity extends AppCompatActivity {

    private static final String TAG = "SecondRegisterWorker";

    // UI Components
    private RadioGroup rgCertificationType;
    private RadioButton rbCartas, rbTitulos;
    private LinearLayout layoutCartasSection, layoutTitulosSection;

    // Cartas
    private Button btnSelectCarta1, btnSelectCarta2, btnSelectCarta3;
    private TextView tvCarta1Status, tvCarta2Status, tvCarta3Status;
    private ImageView ivCarta1Check, ivCarta2Check, ivCarta3Check;

    // Títulos
    private Button btnSelectTitulo1, btnSelectTitulo2, btnSelectTitulo3;
    private TextView tvTitulo1Status, tvTitulo2Status, tvTitulo3Status;
    private ImageView ivTitulo1Check, ivTitulo2Check, ivTitulo3Check;

    private Button btnSelectHojaVida, btnSelectAntecedentes;
    private Button btnSubmitDocuments, btnSkip;
    private TextView tvHojaVidaStatus, tvAntecedentesStatus;
    private ImageView ivHojaVidaCheck, ivAntecedentesCheck;

    // Avance de los requisitos
    private TextView tvRequirements, tvMissing, tvCertLabel, tvCertCount;
    private View segReq1, segReq2, segReq3;
    private TextView tvStepHojaVida, tvStepAntecedentes, tvStepCert;
    private ImageView ivStepHojaVida, ivStepAntecedentes, ivStepCert;
    private final List<View> slots = new ArrayList<>();

    // Providers
    private AuthProvider mAuthProvider;
    private WorkerProvider mWorkerProvider;
    private WorkerDocumentProvider documentProvider;
    private RegistrationProgressDialog mProgressDialog;

    // Datos del usuario
    private String name, lastName, email, password, work, phone, workerId;
    private java.util.ArrayList<String> categories;   // slugs de los oficios (D16)
    private java.util.ArrayList<String> tasks;        // slugs de las tareas que resuelve (mockup 32)
    private String description;                        // descripción opcional (paso 2)

    // Variables para Google Sign-In
    private boolean isFromGoogleSignIn = false;
    private String googleUserId;

    // Documentos obligatorios
    private Uri hojaVidaUri, antecedentesUri;
    private String hojaVidaFileName, antecedentesFileName;

    // Certificaciones - Cartas
    private Uri carta1Uri, carta2Uri, carta3Uri;
    private String carta1FileName, carta2FileName, carta3FileName;

    // Certificaciones - Títulos
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
        updateRequirements();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        lastName = intent.getStringExtra("lastName");
        phone = intent.getStringExtra("phone");
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
        rgCertificationType = findViewById(R.id.rgCertificationType);
        rbCartas = findViewById(R.id.rbCartas);
        rbTitulos = findViewById(R.id.rbTitulos);

        layoutCartasSection = findViewById(R.id.layoutCartasSection);
        layoutTitulosSection = findViewById(R.id.layoutTitulosSection);

        // Cartas
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
        btnSelectTitulo1 = findViewById(R.id.btnSelectTitulo1);
        btnSelectTitulo2 = findViewById(R.id.btnSelectTitulo2);
        btnSelectTitulo3 = findViewById(R.id.btnSelectTitulo3);
        tvTitulo1Status = findViewById(R.id.tvTitulo1Status);
        tvTitulo2Status = findViewById(R.id.tvTitulo2Status);
        tvTitulo3Status = findViewById(R.id.tvTitulo3Status);
        ivTitulo1Check = findViewById(R.id.ivTitulo1Check);
        ivTitulo2Check = findViewById(R.id.ivTitulo2Check);
        ivTitulo3Check = findViewById(R.id.ivTitulo3Check);

        // Obligatorios
        btnSelectHojaVida = findViewById(R.id.btnSelectHojaVida);
        btnSelectAntecedentes = findViewById(R.id.btnSelectAntecedentes);
        btnSubmitDocuments = findViewById(R.id.btnSubmitDocuments);
        btnSkip = findViewById(R.id.btnSkip);
        tvHojaVidaStatus = findViewById(R.id.tvHojaVidaStatus);
        tvAntecedentesStatus = findViewById(R.id.tvAntecedentesStatus);
        ivHojaVidaCheck = findViewById(R.id.ivHojaVidaCheck);
        ivAntecedentesCheck = findViewById(R.id.ivAntecedentesCheck);

        // Avance
        tvRequirements = findViewById(R.id.tvRequirements);
        tvMissing = findViewById(R.id.tvMissing);
        tvCertLabel = findViewById(R.id.tvCertLabel);
        tvCertCount = findViewById(R.id.tvCertCount);
        segReq1 = findViewById(R.id.segReq1);
        segReq2 = findViewById(R.id.segReq2);
        segReq3 = findViewById(R.id.segReq3);
        tvStepHojaVida = findViewById(R.id.tvStepHojaVida);
        tvStepAntecedentes = findViewById(R.id.tvStepAntecedentes);
        tvStepCert = findViewById(R.id.tvStepCert);
        ivStepHojaVida = findViewById(R.id.ivStepHojaVida);
        ivStepAntecedentes = findViewById(R.id.ivStepAntecedentes);
        ivStepCert = findViewById(R.id.ivStepCert);

        mProgressDialog = new RegistrationProgressDialog(this);

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
                        markSlotFilled(tvHojaVidaStatus, ivHojaVidaCheck, btnSelectHojaVida, uri);
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
                        markSlotFilled(tvAntecedentesStatus, ivAntecedentesCheck, btnSelectAntecedentes, uri);
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
                        markSlotFilled(tvCarta1Status, ivCarta1Check, btnSelectCarta1, uri);
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
                        markSlotFilled(tvCarta2Status, ivCarta2Check, btnSelectCarta2, uri);
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
                        markSlotFilled(tvCarta3Status, ivCarta3Check, btnSelectCarta3, uri);
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
                        markSlotFilled(tvTitulo1Status, ivTitulo1Check, btnSelectTitulo1, uri);
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
                        markSlotFilled(tvTitulo2Status, ivTitulo2Check, btnSelectTitulo2, uri);
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
                        markSlotFilled(tvTitulo3Status, ivTitulo3Check, btnSelectTitulo3, uri);
                        checkSubmitButton();
                    }
                }
        );
    }

    /** El mismo PDF, o uno con el mismo nombre, ya está en otra casilla: se avisa y no se usa (DEF-23). */
    private boolean isRepeatedFile(Uri uri, Uri currentSlot) {
        String hash = FileHashHelper.sha256(getContentResolver(), uri);
        String name = FileHashHelper.displayName(getContentResolver(), uri);
        Uri[] others = {hojaVidaUri, antecedentesUri, carta1Uri, carta2Uri, carta3Uri,
                titulo1Uri, titulo2Uri, titulo3Uri};
        for (Uri other : others) {
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
        // Cada casilla se toca entera para elegir el PDF; "Cambiar" hace lo mismo
        bindSlot(btnSelectHojaVida, hojaVidaLauncher);
        bindSlot(btnSelectAntecedentes, antecedentesLauncher);
        bindSlot(btnSelectCarta1, carta1Launcher);
        bindSlot(btnSelectCarta2, carta2Launcher);
        bindSlot(btnSelectCarta3, carta3Launcher);
        bindSlot(btnSelectTitulo1, titulo1Launcher);
        bindSlot(btnSelectTitulo2, titulo2Launcher);
        bindSlot(btnSelectTitulo3, titulo3Launcher);

        // Control segmentado: cartas o títulos
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

    private void bindSlot(Button button, ActivityResultLauncher<String> launcher) {
        View slot = (View) button.getParent();
        slots.add(slot);
        View.OnClickListener pick = v -> {
            if (!isUploading) launcher.launch("application/pdf");
        };
        slot.setOnClickListener(pick);
        button.setOnClickListener(pick);
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
                message += "Has completado las 3 " + certType + " requeridas.";
            } else if (certCount > 0) {
                message += "Has seleccionado " + certCount + " " + certType + ". Recuerda que se requieren 3.";
            }
        } else {
            if (certCount >= 1) {
                message += "Has seleccionado " + certCount + " " + certType + ".";
            } else {
                message += "Debes seleccionar al menos 1 " + certType + ".";
            }
        }

        String missing = missingText();
        if (missing != null) {
            message += "\n\n" + getString(R.string.docs_missing, missing) + ".";
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

    /** Abre la ventana de carga desde el primer paso, con o sin documentos. */
    private void showProgress() {
        mProgressDialog.setWithDocuments(hasDocumentsToUpload());
        mProgressDialog.setStep(RegistrationProgressDialog.STEP_ACCOUNT);
        mProgressDialog.show();
    }

    private void startRegistrationProcess() {
        if (isUploading) return;
        setUIUploading(true);
        showProgress();

        // Si viene de Google Sign-In, NO crear cuenta nueva
        if (isFromGoogleSignIn) {
            workerId = googleUserId;
            Worker worker = newWorker();
            createWorkerInDatabase(worker);
        } else {
            mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        Worker worker = newWorker();
                        createWorkerInDatabase(worker);
                    } else {
                        onRegistrationFailed("Error al crear cuenta: " + task.getException().getMessage());
                    }
                }
            });
        }
    }

    private void createWorkerInDatabase(Worker worker) {
        if (hasProfileImage && profileImageUri != null) {
            uploadProfileImageAndCreateWorker(worker);
        } else {
            saveWorkerToDatabase(worker, null);
        }
    }

    private void uploadProfileImageAndCreateWorker(Worker worker) {
        String imageName = "profile_" + workerId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = mStorageReference.child("worker_profiles").child(imageName);

        UploadTask uploadTask = imageRef.putFile(profileImageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                saveWorkerToDatabase(worker, downloadUri.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error al obtener URL de imagen", e);
                onRegistrationFailed("Error al obtener URL de imagen: " + e.getMessage());
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error al subir imagen", e);
            showMessage("Error al subir imagen. Se creará el perfil sin foto.");
            // Crear worker sin imagen si falla la subida
            saveWorkerToDatabase(worker, null);
        }).addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            mProgressDialog.setMessage("Subiendo foto de perfil... " + (int) progress + "%");
        });
    }

    private void saveWorkerToDatabase(Worker worker, String imageUrl) {
        mProgressDialog.setMessage(null);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            worker.setImage(imageUrl);
        } else {
            worker.setImage("default");
        }

        if (phone != null && !phone.isEmpty()) {
            worker.setPhone(phone);
        }

        worker.setConsentAcceptedAt(getIntent().getLongExtra(ConsentHelper.EXTRA_ACCEPTED_AT, 0));

        mWorkerProvider.create(worker).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isRegistered = true;
                if (hasDocumentsToUpload()) {
                    // La ventana sigue abierta: ahora sube los documentos
                    startUploadProcess();
                } else {
                    mProgressDialog.dismiss();
                    setUIUploading(false);
                    Toast.makeText(SecondRegisterWorkerActivity.this,
                            "Cuenta creada", Toast.LENGTH_SHORT).show();
                    finishRegistration();
                }
            } else {
                onRegistrationFailed("Error al guardar datos: " + task.getException().getMessage());
                // Solo eliminar cuenta de Auth si NO viene de Google
                if (!isFromGoogleSignIn && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    FirebaseAuth.getInstance().getCurrentUser().delete();
                }
            }
        });
    }

    private void onRegistrationFailed(String message) {
        mProgressDialog.dismiss();
        isUploading = false;
        setUIUploading(false);
        showMessage(message);
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
        mProgressDialog.setStep(RegistrationProgressDialog.STEP_DOCUMENTS);
        mProgressDialog.setDocuments(0, totalDocuments);

        if (hojaVidaUri != null) {
            uploadDocument(WorkerDocument.TYPE_HOJA_VIDA, WorkerDocument.CATEGORY_HOJA_VIDA,
                    null, hojaVidaUri, hojaVidaFileName, 0, "Hoja de Vida");
        }

        if (antecedentesUri != null) {
            uploadDocument(WorkerDocument.TYPE_ANTECEDENTES, WorkerDocument.CATEGORY_ANTECEDENTES,
                    null, antecedentesUri, antecedentesFileName, 0, "Antecedentes Judiciales");
        }

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
                    if (!isUploading) return; // otra subida ya falló
                    uploadedCount++;
                    mProgressDialog.setDocuments(uploadedCount, totalDocuments);

                    if (uploadedCount == totalDocuments) {
                        onAllDocumentsUploaded();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isUploading) return;
                    onRegistrationFailed("Error al subir " + description + ": " + e.getMessage());
                });
    }

    private void onAllDocumentsUploaded() {
        isUploading = false;
        mProgressDialog.setStep(RegistrationProgressDialog.STEP_REVIEW);

        documentProvider.updateWorkerVerificationStatus(workerId, "documents_submitted")
                .addOnSuccessListener(aVoid -> {
                    mProgressDialog.dismiss();
                    setUIUploading(false);

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Registro completado")
                            .setMessage("Tu cuenta quedó creada y tus documentos se enviaron. Los revisamos en 24 a 48 horas.")
                            .setPositiveButton("Continuar", (dialog, which) -> finishRegistration())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    mProgressDialog.dismiss();
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

        // Sin documentos: la ventana solo muestra "Crear tu cuenta"
        mProgressDialog.setWithDocuments(false);
        mProgressDialog.setStep(RegistrationProgressDialog.STEP_ACCOUNT);
        mProgressDialog.show();
        setUIUploading(true);

        if (isFromGoogleSignIn) {
            workerId = googleUserId;
            Worker worker = newWorker();
            if (hasProfileImage && profileImageUri != null) {
                uploadProfileImageAndCreateWorker(worker);
            } else {
                saveWorkerToDatabase(worker, null);
            }
        } else {
            mAuthProvider.register(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Worker worker = newWorker();
                    if (hasProfileImage && profileImageUri != null) {
                        uploadProfileImageAndCreateWorker(worker);
                    } else {
                        saveWorkerToDatabase(worker, null);
                    }
                } else {
                    onRegistrationFailed("Error al crear cuenta: " + task.getException().getMessage());
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

    /** Casilla con archivo: fondo lleno, nombre del PDF, visto y "Cambiar". */
    private void markSlotFilled(TextView statusView, ImageView checkView, Button changeButton, Uri uri) {
        String fileName = FileHashHelper.displayName(getContentResolver(), uri);
        statusView.setText(fileName != null ? fileName : getString(R.string.docs_selected));
        statusView.setTextAppearance(R.style.TextAppearance_ADS_BodySmall);
        statusView.setTextColor(ContextCompat.getColor(this, R.color.ads_text));
        checkView.setVisibility(View.VISIBLE);
        changeButton.setVisibility(View.VISIBLE);

        View slot = (View) statusView.getParent();
        slot.setBackgroundResource(R.drawable.bg_ads_doc_slot_filled);
        ImageView icon = (ImageView) ((LinearLayout) slot).getChildAt(0);
        icon.setImageResource(R.drawable.ic_document);
        ImageViewCompat.setImageTintList(icon, ContextCompat.getColorStateList(this, R.color.ads_text));
    }

    private void checkSubmitButton() {
        boolean hasObligatorios = hojaVidaUri != null || antecedentesUri != null;
        boolean hasCertificaciones;

        if (selectedCertificationType.equals("cartas")) {
            hasCertificaciones = carta1Uri != null || carta2Uri != null || carta3Uri != null;
        } else {
            hasCertificaciones = titulo1Uri != null || titulo2Uri != null || titulo3Uri != null;
        }

        btnSubmitDocuments.setEnabled((hasObligatorios || hasCertificaciones) && !isUploading);
        updateRequirements();
    }

    private int letterCount() {
        return (carta1Uri != null ? 1 : 0) + (carta2Uri != null ? 1 : 0) + (carta3Uri != null ? 1 : 0);
    }

    private int titleCount() {
        return (titulo1Uri != null ? 1 : 0) + (titulo2Uri != null ? 1 : 0) + (titulo3Uri != null ? 1 : 0);
    }

    private boolean certificationsReady() {
        return selectedCertificationType.equals("cartas") ? letterCount() >= 3 : titleCount() >= 1;
    }

    /** "hoja de vida, antecedentes y 2 cartas", o null si no falta nada. */
    private String missingText() {
        List<String> parts = new ArrayList<>();
        if (hojaVidaUri == null) parts.add(getString(R.string.docs_missing_cv));
        if (antecedentesUri == null) parts.add(getString(R.string.docs_missing_background));
        if (selectedCertificationType.equals("cartas")) {
            int left = 3 - letterCount();
            if (left == 1) parts.add(getString(R.string.docs_missing_one_letter));
            else if (left > 1) parts.add(getString(R.string.docs_missing_letters, left));
        } else if (titleCount() == 0) {
            parts.add(getString(R.string.docs_missing_title));
        }
        if (parts.isEmpty()) return null;
        if (parts.size() == 1) return parts.get(0);
        String head = android.text.TextUtils.join(", ", parts.subList(0, parts.size() - 1));
        return head + getString(R.string.docs_missing_and) + parts.get(parts.size() - 1);
    }

    /** Barra de requisitos, números o vistos de cada tarjeta, conteo y "Te falta". */
    private void updateRequirements() {
        boolean cv = hojaVidaUri != null;
        boolean bg = antecedentesUri != null;
        boolean cert = certificationsReady();
        int ready = (cv ? 1 : 0) + (bg ? 1 : 0) + (cert ? 1 : 0);
        tvRequirements.setText(getString(R.string.docs_requirements, ready));

        boolean letters = selectedCertificationType.equals("cartas");
        int certPartial = letters ? letterCount() : titleCount();
        paintSegment(segReq1, cv ? R.color.ads_brand : R.color.ads_card_edge);
        paintSegment(segReq2, bg ? R.color.ads_brand : R.color.ads_card_edge);
        paintSegment(segReq3, cert ? R.color.ads_brand : certPartial > 0 ? R.color.ads_chip_bg : R.color.ads_card_edge);

        setStepDone(tvStepHojaVida, ivStepHojaVida, cv);
        setStepDone(tvStepAntecedentes, ivStepAntecedentes, bg);
        setStepDone(tvStepCert, ivStepCert, cert);

        tvCertLabel.setText(letters ? R.string.docs_letters_label : R.string.docs_titles_label);
        tvCertCount.setText(getString(R.string.docs_count, certPartial, 3));

        String missing = missingText();
        if (missing == null) {
            tvMissing.setText(R.string.docs_ready);
            tvMissing.setTextColor(ContextCompat.getColor(this, R.color.ads_brand_text));
        } else {
            tvMissing.setText(getString(R.string.docs_missing, missing));
            tvMissing.setTextColor(ContextCompat.getColor(this, R.color.ads_muted));
        }
    }

    private void paintSegment(View segment, int colorRes) {
        ViewCompat.setBackgroundTintList(segment, ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
    }

    private void setStepDone(TextView number, ImageView check, boolean done) {
        number.setVisibility(done ? View.INVISIBLE : View.VISIBLE);
        check.setVisibility(done ? View.VISIBLE : View.GONE);
    }

    /** Mientras se registra y sube, nada de la pantalla responde (la ventana de carga lo explica). */
    private void setUIUploading(boolean uploading) {
        for (View slot : slots) slot.setEnabled(!uploading);
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
        rbCartas.setEnabled(!uploading);
        rbTitulos.setEnabled(!uploading);
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
