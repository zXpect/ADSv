# ADS Mobile App — Convenciones

App móvil nativa Android, escrita en Java, desarrollada en Android Studio.
No consume la API del backend Django directamente: comparte con el panel
admin (`../ADS_AdminBack/backend`, ver su `README.md` para el contrato de
API que sí expone) la misma Firebase Realtime Database/Storage/Auth — ver
la sección "Conexión con el backend" más abajo.

## Stack técnico

- Lenguaje: Java
- IDE: Android Studio
- Versión mínima de SDK / target SDK: `minSdk 23` (Android 6.0) / `targetSdk 33` / `compileSdk 34` (ver `app/build.gradle.kts`)
- Gestor de dependencias: Gradle (Kotlin DSL)
- Librería de networking: Retrofit 2 + OkHttp 3 (`retrofit/RetrofitClient.java`), pero **solo se usa para llamar APIs REST externas de Google** (FCM `https://fcm.googleapis.com` vía `retrofit/IFCMApi.java`, y Google/Places vía `retrofit/IGoogleApi.java`). Los datos de dominio (workers, clients, service requests, documentos) no pasan por Retrofit ni por el backend Django: se leen/escriben directo contra Firebase Realtime Database, Storage y Auth con el SDK de Firebase.
- Persistencia local: no hay Room ni SQLite propio. `SharedPreferences` se usa únicamente en `providers/AuthProvider.java` para cachear el token FCM (`saveFCMToken`/`getSavedFCMToken`); la sesión de usuario la persiste el propio SDK de `FirebaseAuth`.

## Arquitectura

Patrón de arquitectura usado: no es MVVM/MVP formal. Es una arquitectura por capas simple tipo "Activity + Provider" (común en apps basadas en Firebase):
- **Activities**: UI + lógica de pantalla, organizadas por rol de usuario.
- **Providers**: capa de acceso a datos, un provider por entidad/responsabilidad sobre Firebase (Auth, Realtime Database, Storage, Geofire, FCM).
- **Models**: POJOs planos que mapean los nodos de Firebase.
- **Adapters**: adapters de `RecyclerView`.
- **Helpers**: utilidades transversales (interceptores HTTP, logout, notificaciones).

Estructura de paquetes (`com.project.ads`):

```
com.project.ads/
├── activities/
│   ├── client/        # pantallas del rol cliente (registro, mapa, solicitudes, calificación...)
│   ├── worker/         # pantallas del rol trabajador (documentos, servicios activos, fixdepot...)
│   └── adapters/       # adapters usados por activities sueltas (DocumentAdapter, ToolAdapter)
├── adapters/           # adapters generales (PendingRequestsAdapter, DocumentsAdapter)
├── providers/          # acceso a datos Firebase (AuthProvider, WorkerProvider, ClientProvider,
│                       #   RequestProvider, ServiceRequestProvider, GeofireProvider,
│                       #   NotificationProvider, TokenProvider, WorkerDocumentProvider,
│                       #   FirebaseAuthProvider)
├── models/             # POJOs (Worker, Client, User, ServiceRequest, Tool, WorkerDocument, Token...)
├── helpers/            # utilidades (AuthInterceptor, DatabaseHelper, LogoutHelper,
│                       #   ServiceRequestHelper, NotificationPermissionHelper)
├── retrofit/           # clientes HTTP para APIs de Google/FCM (RetrofitClient, IGoogleApi, IFCMApi,
│                       #   FCMAuthInterceptor)
├── services/           # MyFirebaseMessagingService (push notifications)
├── channel/            # NotificationHelper (canales de notificación Android)
└── includes/           # componentes UI reutilizables (MyToolbar)
```

## Cómo correr y compilar

- Build debug: `./gradlew assembleDebug`
- Instalar en emulador/dispositivo: `./gradlew installDebug`
- Tests unitarios: `./gradlew test` (solo existe el boilerplate `ExampleUnitTest`; no hay tests de lógica de negocio todavía)
- Tests instrumentados: `./gradlew connectedAndroidTest` (boilerplate por defecto de Android Studio, sin tests propios escritos aún)

## Conexión con el backend

- URL base de la API: **la app no consume el backend Django directamente.** La app móvil y el panel admin (`../backend`) comparten la misma Firebase Realtime Database / Storage / Auth; la app lee y escribe esos datos usando el SDK de Firebase, no un endpoint REST propio. Si en el futuro se necesita hablar con el backend Django (`/api/...`), definir la URL base como constante de build (por flavor/`BuildConfig`) en vez de hardcodearla.
- Manejo de autenticación (dónde se guarda el token, cómo se envía): la sesión la maneja `FirebaseAuth.getInstance()` y el SDK la persiste automáticamente entre reinicios de la app; no hay JWT propio en el cliente móvil. El único token gestionado manualmente es el **token FCM** (push notifications): se cachea en `SharedPreferences` (`AuthProvider`) y se sincroniza en Realtime Database vía `TokenProvider` / `WorkerProvider.updateWorkerFCMToken`.
- Manejo de errores de red: no hay una capa central de manejo de errores; cada `Provider`/`Activity` maneja el resultado con los callbacks propios de Firebase (`addOnSuccessListener` / `addOnFailureListener` / `addOnCompleteListener`), logueando con `Log.e`/`Log.d` y mostrando `Toast` al usuario cuando aplica.

## Seguridad (app móvil)

- No hardcodear claves ni secretos en el código fuente: `local.properties`, `app/src/main/res/values/secret.xml`, `app/google-services.json` y `app/src/main/assets/serviceAccountKey.json` están en `.gitignore` y no se versionan; las API keys de Google/Places/Firebase se inyectan desde esos archivos locales (cada desarrollador debe crear los suyos, no están en el repo).
- Seguir las recomendaciones del OWASP Mobile Security Project relevantes al
  proyecto: almacenamiento seguro de datos sensibles, validación de
  certificados en llamadas HTTPS, no loguear información sensible en
  producción
- Verificación documental de trabajadores (lado móvil): `WorkerDocumentsActivity` permite subir los documentos requeridos (hoja de vida, antecedentes judiciales, títulos, cartas de recomendación) a Firebase Storage a través de `providers/WorkerDocumentProvider`. El estado de verificación (`documents_submitted`, `approved`, `rejected`) se lee desde `verificationStatus` en Realtime Database — es el mismo dato que gestiona el panel admin del backend Django, por lo que un cambio de estado hecho desde el backend se refleja directamente en la app sin necesidad de una llamada API adicional.

## Convenciones de código

- Nomenclatura de clases/paquetes: PascalCase para clases, con sufijo según su rol (`*Activity`, `*Provider`, `*Adapter`, `*Helper`); paquetes en minúscula agrupados por feature/rol (`activities.client`, `activities.worker`, `activities.worker.fixdepot`).
- Convención de layouts XML (nombres de archivos, IDs): `activity_<pantalla>.xml` para layouts de Activity (ej. `activity_home_worker.xml`), `item_<fila>.xml` para filas de `RecyclerView` (ej. `item_worker_card.xml`), `dialog_<nombre>.xml` para diálogos y `spinner_<nombre>.xml` para spinners.
- Manejo de strings/recursos (evitar strings hardcodeados): **no se sigue de forma estricta hoy** — hay bastantes strings de UI escritos directo en el código Java en vez de `strings.xml`. Al tocar una pantalla, mover a `strings.xml` los strings visibles que se editen, pero no es necesario migrar todo el archivo de una vez.
- Formato de código / linter usado: ninguno configurado (no hay Checkstyle/ktlint/detekt); se sigue el formateo por defecto de Android Studio.

## Qué falta para el MVP (resumen — ver `../docs/09_Cronograma.md` para detalle)

Según el cronograma del proyecto (Sprints 5 y 6) y una revisión del código actual, **no están implementados todavía**:
- Pasarela de pago en línea (Sprint 5) — no hay ningún módulo/provider de pagos en el código.
- Módulo de reclamaciones (PQR) y reversión de pago (Sprint 6) — no existe pantalla ni provider relacionado.
- Disparador de notificación de finalización de servicio ligado a calificación y PQR (Sprint 6) — existen `ServiceCompletionActivity` y `RatingActivity`, pero falta conectar la reclamación (PQR) a ese flujo.

Ya implementado: registro/login (Firebase Auth), búsqueda geolocalizada de trabajadores (Geofire + Maps), gestión de solicitudes de servicio, subida y verificación de documentos, calificación de trabajadores, notificaciones push (FCM).
