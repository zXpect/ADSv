# ADS Mobile App — Convenciones

App móvil nativa Android, escrita en Java, desarrollada en Android Studio.
Lee y escribe los datos de dominio directo en Firebase (Realtime Database,
Storage, Auth), compartidos con el backend y el panel admin. Lo que las reglas
de Firebase reservan al servidor (cotización, pagos, calificación y
notificaciones push) lo hace a través de la API del backend Django
(`../ADS_AdminBack/backend`, ver su `README.md`) — ver "Conexión con el backend".

## Stack técnico

- Lenguaje: Java
- IDE: Android Studio
- Versión mínima de SDK / target SDK: `minSdk 23` (Android 6.0) / `targetSdk 33` / `compileSdk 34` (ver `app/build.gradle.kts`)
- Gestor de dependencias: Gradle (Kotlin DSL)
- Librería de networking: Retrofit 2 + OkHttp 3. `retrofit/AdsApiClient.java` + `retrofit/IAdsApi.java` llaman al backend Django (cotización, pagos, calificación, notificaciones); `FirebaseIdTokenInterceptor` adjunta el ID token de Firebase y lo renueva ante un 401, y `ApiCallAdapter` traduce las respuestas `{"success", "data", "error"}` a `providers/ApiCallback`. El resto de los datos de dominio se lee/escribe directo en Firebase con su SDK. La app ya **no** envía FCM directamente.
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
│   ├── worker/         # pantallas del rol trabajador (documentos, servicios activos...)
│   └── adapters/       # adapters usados por activities sueltas (DocumentAdapter)
├── adapters/           # adapters generales (PendingRequestsAdapter, DocumentsAdapter)
├── providers/          # acceso a datos: Firebase (AuthProvider, WorkerProvider, ClientProvider,
│                       #   RequestProvider, GeofireProvider, TokenProvider, WorkerDocumentProvider)
│                       #   y backend (CatalogProvider, QuoteProvider, PaymentProvider, NotificationProvider)
├── models/             # POJOs (Worker, Client, User, ServiceRequest, RequestTaskItem, WorkerDocument, Token...)
│   └── api/            # respuestas y cuerpos de la API del backend (ApiResponse, CatalogData, QuoteReference...)
├── helpers/            # utilidades (DatabaseHelper, LogoutHelper, ServiceRequestHelper,
│                       #   NotificationPermissionHelper, QuoteDialogHelper, CancelDialogHelper,
│                       #   CategoryHelper, ReasonHelper, ScheduleFields)
├── retrofit/           # cliente del backend (AdsApiClient, IAdsApi, FirebaseIdTokenInterceptor,
│                       #   ApiCallAdapter) e IGoogleApi
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

- URL base de la API: `BuildConfig.ADS_API_BASE_URL`, definida en `app/build.gradle.kts` por tipo de build. Debug: `http://10.0.2.2:8000/` (emulador → backend local); se cambia con la propiedad Gradle `ADS_API_BASE_URL_DEBUG` (p. ej. la IP del PC para un teléfono físico). Release: `ADS_API_BASE_URL_RELEASE` (HTTPS obligatorio: `res/xml/network_security_config.xml` bloquea HTTP en release; la versión de `src/debug` lo permite).
- Qué pasa por el backend: catálogo de oficios y tareas y estimado (`CatalogProvider`, con respaldo local de oficios), crear la solicitud (`RequestProvider.createRequest` → `POST /api/requests/`, con `lat`/`lng`), cancelar con motivo y reprogramar (`RequestProvider.cancelRequest`/`reschedule`, `CancelDialogHelper`), cotizar con motivo y agenda/aceptar/rechazar (`QuoteProvider`, `QuoteDialogHelper`), pagar (`PaymentProvider` + `activities/client/PaymentActivity`, checkout en Custom Tab y regreso por `adsapp://payment-result`), calificar (`RequestProvider.rateService`) y notificaciones push (`NotificationProvider` → `POST /api/notifications/`). El estado del pago se lee de `requests/<id>/payment_status` en Firebase, que escribe el backend.
- Manejo de autenticación (dónde se guarda el token, cómo se envía): la sesión la maneja `FirebaseAuth.getInstance()` y el SDK la persiste automáticamente entre reinicios de la app. Hacia el backend se envía el **ID token de Firebase** (`Authorization: Bearer`), que pide `FirebaseIdTokenInterceptor` en cada llamada. El **token FCM** se cachea en `SharedPreferences` (`AuthProvider`) y se sincroniza en Realtime Database vía `TokenProvider` / `WorkerProvider.updateWorkerFCMToken`; lo lee el backend para enviar las notificaciones.
- Manejo de errores de red: no hay una capa central de manejo de errores; cada `Provider`/`Activity` maneja el resultado con los callbacks propios de Firebase (`addOnSuccessListener` / `addOnFailureListener` / `addOnCompleteListener`), logueando con `Log.e`/`Log.d` y mostrando `Toast` al usuario cuando aplica.

## Seguridad (app móvil)

- No hardcodear claves ni secretos en el código fuente: `local.properties`, `app/src/main/res/values/secret.xml` y `app/google-services.json` están en `.gitignore` y no se versionan; las API keys de Google/Places/Firebase se inyectan desde esos archivos locales (cada desarrollador debe crear los suyos, no están en el repo).
- **Nunca** incluir una credencial de servicio (`serviceAccountKey.json`) en la app: todo lo que la requiera va al backend. El build la excluye de `assets/` por si quedara una copia local (`androidResources.ignoreAssetsPattern`).
- Las reglas de seguridad de Firebase viven en `../ADS_AdminBack/backend/firebase/` (con pruebas). Un cambio en cómo la app lee o escribe Firebase debe revisarse contra esas reglas.
- Seguir las recomendaciones del OWASP Mobile Security Project relevantes al
  proyecto: almacenamiento seguro de datos sensibles, validación de
  certificados en llamadas HTTPS, no loguear información sensible en
  producción
- Oficios del trabajador: `User/Trabajadores/<uid>/categories` (slugs del catálogo, hasta 3). `work` se sigue escribiendo con el primer oficio como respaldo y `Worker.getCategorySlugs()` lo traduce mientras no corra `migrate_worker_categories` en el backend. Nada de listas fijas de oficios en `res/`.
- Verificación documental de trabajadores (lado móvil): `WorkerDocumentsActivity` permite subir los documentos requeridos (hoja de vida, antecedentes judiciales, títulos, cartas de recomendación) a Firebase Storage a través de `providers/WorkerDocumentProvider`. El estado de verificación (`documents_submitted`, `approved`, `rejected`) se lee desde `verificationStatus` en Realtime Database — es el mismo dato que gestiona el panel admin del backend Django, por lo que un cambio de estado hecho desde el backend se refleja directamente en la app sin necesidad de una llamada API adicional.

## Convenciones de código

- Nomenclatura de clases/paquetes: PascalCase para clases, con sufijo según su rol (`*Activity`, `*Provider`, `*Adapter`, `*Helper`); paquetes en minúscula agrupados por feature/rol (`activities.client`, `activities.worker`).
- Convención de layouts XML (nombres de archivos, IDs): `activity_<pantalla>.xml` para layouts de Activity (ej. `activity_home_worker.xml`), `item_<fila>.xml` para filas de `RecyclerView` (ej. `item_worker_card.xml`), `dialog_<nombre>.xml` para diálogos y `spinner_<nombre>.xml` para spinners.
- Manejo de strings/recursos (evitar strings hardcodeados): **no se sigue de forma estricta hoy** — hay bastantes strings de UI escritos directo en el código Java en vez de `strings.xml`. Al tocar una pantalla, mover a `strings.xml` los strings visibles que se editen, pero no es necesario migrar todo el archivo de una vez.
- Formato de código / linter usado: ninguno configurado (no hay Checkstyle/ktlint/detekt); se sigue el formateo por defecto de Android Studio.

## Qué falta para el MVP (resumen — ver `../docs/09_Cronograma.md` para detalle)

Según el cronograma del proyecto (Sprints 5 y 6) y una revisión del código actual, **no están implementados todavía**:
- Pasarela de pago en línea (Sprint 5) — implementada contra la pasarela simulada del backend (`PAGOS_MODO=mock`); falta conectar Wompi real (Fase 7 del plan de pagos).
- Módulo de reclamaciones (PQR) y reversión de pago (Sprint 6) — no existe pantalla ni provider relacionado.
- Disparador de notificación de finalización de servicio ligado a calificación y PQR (Sprint 6) — existen `ServiceCompletionActivity` y `RatingActivity`, pero falta conectar la reclamación (PQR) a ese flujo.

Ya implementado: registro/login (Firebase Auth), búsqueda geolocalizada de trabajadores (Geofire + Maps), gestión de solicitudes de servicio, subida y verificación de documentos, calificación de trabajadores, notificaciones push (FCM).
