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
- Tests unitarios: `./gradlew testDebugUnitTest` (35 pruebas JUnit al 20/10/2026 en `app/src/test/java/com/project/ads/`; corren en el PC, sin dispositivo, con el JBR 17 de Android Studio). Solo lógica que no depende de Android; si una clase la necesita, separar la lógica en un método estático. Registro en `../contexto/pruebas/`.
- Tests instrumentados: no hay (se borró el de ejemplo); las pantallas se cubren con los casos funcionales de `../contexto/planes/PLAN_PRUEBAS.md`.

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

---

## Rediseño visual y UX (mockups aprobados)

### Estado y objetivo

Los mockups de TODA la app móvil (cliente y trabajador) y del panel web de administración ya están rediseñados y aprobados. **La tarea ahora es adaptar el código existente a esos mockups y crear lo que falte**, sin romper la lógica, Firebase ni el backend Django que ya funcionan.

- Los mockups están en `docs/mockups/` (archivos `.dc.html`, uno por pantalla; se leen como HTML con medidas, colores y textos exactos). Índice más abajo.
- Los tokens del sistema de diseño están en `docs/mockups/ds/ads/tokens.json`. Los logos en `docs/branding/`.
- Los mockups usan datos de ejemplo inventados (nombres, precios, fechas). **No copiar esos datos al código**; vienen del backend. Lo único real es la tarifa mínima de visita de $25.500 COP.

### Reglas de trabajo

1. Antes de tocar una pantalla, **leer su mockup** y el layout/Activity/Fragment actual. Listar qué cambia y qué se conserva.
2. **No cambiar lógica de negocio, modelos, llamadas a la API ni navegación funcional** sin avisar. El cambio visual y de UX va en layouts, estilos y recursos.
3. Una pantalla o módulo por vez, con un commit por cada uno.
4. Después de cada pantalla: compilar (`./gradlew assembleDebug`), correr `./gradlew lint` y, si hay emulador, tomar captura con `adb exec-out screencap -p > captura.png` y compararla con el mockup.
5. Si algo del mockup choca con lo que ya existe en el código (campo que el backend no tiene, flujo distinto), **preguntar o anotarlo en la sección "Decisiones pendientes"**; no inventar.
6. No instalar ni actualizar dependencias, AGP, Gradle o el SDK sin preguntar. Android Studio del equipo: **Jirafa (Giraffe)**; **no aceptar el "Android Gradle Plugin Upgrade Assistant"**.
7. Respetar la arquitectura "Activity + Provider" y los nombres de layouts de este archivo (`activity_*`, `item_*`, `dialog_*`). El rediseño no mueve lógica entre capas.
8. Al tocar una pantalla, **mover sus strings visibles a `strings.xml`** (regla ya acordada en "Convenciones de código"). Verificar en `app/build.gradle.kts` la versión de Material Components antes de usar estilos Material 3.

### Flujo del producto y reglas del MVP (según los mockups)

**Cliente:** busca por oficio → elige tarea → ve técnicos (lista o mapa) → elige técnico → crea solicitud con tareas (la dirección se captura solo al enviar) → el técnico cotiza → el cliente acepta (paga con Wompi o en efectivo) o rechaza (sin contraoferta) → el técnico inicia viaje → inicia trabajo → finaliza → pago → el cliente califica con estrellas.

**Estados del servicio:** pendiente → cotizada → aceptada → en camino → en progreso → completada, o cancelada.

- Tarifa mínima por visita **$25.500 COP**: es un piso del precio, no un cobro aparte.
- Factor regional **-15 %** (lo administra el admin).
- Aviso de precio: "El valor final puede variar según materiales, tiempo, dificultad y condiciones del lugar".
- Algunas tareas (cerrajería, jardinería, carpintería) no tienen rango: "el técnico la cotizará".
- Cancelar: el cliente solo antes de "en camino"; el trabajador solo antes de "Iniciar viaje". Motivo obligatorio, elegido de una lista.
- Una sola re-cotización por servicio.
- PQR: plazo de respuesta de 15 días hábiles.
- Registro de cliente: dos casillas separadas (términos y condiciones; tratamiento de datos personales) más "Soy mayor de edad".
- Registro de trabajador: hasta 3 oficios, tareas, descripción y documentos (identidad, antecedentes, certificaciones). Verificación: pendiente / aprobada / rechazada (con motivo y posibilidad de volver a subir).
- Trabajador: botón conectar/desconectar (disponible); desconectado no aparece en el mapa ni recibe solicitudes nuevas.

**NO implementar ni dibujar:** propinas, horas extra, reembolsos, comisión, contraoferta, penalizaciones, calificación del trabajador al cliente, tienda de materiales, calendario completo, chat interno, cliente iOS/web, cobertura fuera de Ubaté.

### Sistema de diseño → recursos Android

Fuente: `docs/mockups/ds/ads/tokens.json`. Crear/usar estos recursos y **no poner colores ni medidas fijas en los layouts**.

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `brand` | `#E8751A` | `#F28A2E` | Botones principales, acentos, techo del logo |
| `brand_text` | `#B45309` | `#F28A2E` | Texto/enlaces naranja sobre fondo claro (contraste AA) |
| `navy` | `#1B1830` | `#1B1830` | Texto sobre botón naranja, barras, fichas del ícono |
| `charcoal` | `#3A3F47` | `#F6F3EE` | Texto principal, llave del logo |
| `surface` | `#F6F3EE` | `#1B1830` | Fondo de pantallas |
| `surface_alt` | `#E7E3DB` | `#26223F` | Tarjetas y zonas secundarias |
| `muted` | `#5B606B` | `#B9B6C4` | Texto secundario |
| `line` | `#CFCAC0` | `#3A3658` | Bordes y divisores |
| (control) | `#858A93` | — | Borde de campos y botones secundarios |
| (chip) | `#FBE7D3` | — | Fondo de chips naranja claro |
| (chip texto) | `#8F3F06` | — | Texto de chips naranja |

- Colores en `values/colors.xml` y `values-night/colors.xml`; tema en `themes.xml` (`Theme.Material3.*` con los colores ADS). Modo oscuro opcional por ahora, pero dejar los recursos listos.
- **Tipografía: Sora** (400, 600, 800) en `res/font/` y como `fontFamily` del tema. Estilos: wordmark 40/44 (800), title 24/30 (600), body 16/24 (400), label 12/16 (600).
- **Espaciado:** 4, 8, 16 (margen lateral y relleno de tarjetas), 24 (entre secciones) dp. **Radios:** 12 (campos, chips), 16 (botones, tarjetas), 22 (hojas inferiores, ícono).
- Pantallas de 390 x 844 dp como referencia; deben adaptarse a otros anchos sin desplazamiento horizontal.

### Reglas de componentes

- **Botón principal:** fondo `brand`, **texto `navy`** (no blanco), altura 52-56 dp, radio 16. Secundario: borde `#858A93` 1,5 dp, fondo transparente. Oscuro: fondo `navy`, texto `surface`.
- **Zonas táctiles:** mínimo **44 dp** (botones, íconos, filas).
- **Campos:** `label` real arriba (no solo placeholder), borde `#858A93`, radio 12-14, foco con anillo naranja.
- **Estado nunca solo por color:** acompañar con texto o ícono (chips "Pendiente", "Aprobado", etc.).
- **Navegación inferior:** 76 dp. Cliente: Inicio / Historial / PQR / Mi cuenta. Trabajador: Inicio / Solicitudes / Mis trabajos / Mi cuenta. El mapa se abre desde una tarjeta al inicio de cada usuario y tiene flecha de volver.
- **Barra de acción fija** (botón principal abajo): fondo blanco, borde superior fino.
- **Íconos:** trazo (stroke) de 2 dp, estilo uniforme; usar vector drawables. Sin emojis.
- **Accesibilidad:** `contentDescription` en íconos y botones solo-ícono; contraste AA; no depender del color.

### Logo (decisión definitiva)

- **Opción A** del lienzo "Logo ADS v7": techo naranja + llave inglesa gris/charcoal, sin tuerca como pieza aparte; la boca de la llave tiene forma de tuerca; cabeza corta y simétrica con el ángulo del techo. Referencia: https://claude.ai/artifact/Rk3ZW8ZfMP2twMcg39EXPQ
- **Opción D** (la misma marca dentro de una insignia de esquinas redondeadas, fondo navy): candidata para el **ícono de la app** (adaptive icon).
- Archivos: `docs/branding/ads-logo-claro.svg` (para fondos claros) y `ads-logo-oscuro.svg` (para fondos oscuros). Convertir a vector drawable para la app.
- Usar el logo exactamente así; no redibujarlo ni cambiar sus colores.

### Mapa, pagos y PQR en el rediseño

- **Mapa:** la app ya usa Google Maps + Geofire + Places (ver "Qué falta" y `GeofireProvider`). Los mockups de mapa (cliente: "Cerca de ti" con filtros, pines y tarjeta inferior; trabajador: mapa con interruptor Conectado/Desconectado) dibujan un mapa genérico. Implementarlos sobre el mapa real, conservando la lógica actual de ubicación y de disponibilidad del trabajador. No cambiar el proveedor.
- **Pagos:** hoy el backend usa pasarela simulada (`PAGOS_MODO=mock`); Wompi real aún no está conectado. Rediseñar las pantallas de pago (método, pagando, aprobado, rechazado, pendiente, efectivo registrado) sin tocar el flujo del Custom Tab ni `adsapp://payment-result`. El estado se sigue leyendo de `requests/<id>/payment_status`.
- **PQR:** no existe en el código (ver "Qué falta para el MVP"). Las pantallas PQR (lista y crear, cliente y trabajador) son **nuevas**: requieren provider y modelo nuevos y coordinar con el backend (Maikoll) antes de construir.
- Reprogramar y cancelar con motivo ya existen en el código (`CancelDialogHelper`, `RequestProvider.reschedule`); los mockups de cancelación y re-cotización deben apoyarse en eso, no duplicarlo.

### Índice de mockups (`docs/mockups/`)

Todas las pantallas están enlazadas entre sí con `<a href>`; seguir los enlaces da el flujo real.

| Pantalla | Archivo |
|---|---|
| 01 · Inicio de sesión | `Main.dc.html` |
| 02 · Bienvenida | `Bienvenida.dc.html` |
| 03 · Elegir rol | `Rol.dc.html` |
| 04 · Registro cliente | `RegistroCliente.dc.html` |
| 05 · Registro trabajador | `RegistroTrabajador.dc.html` |
| 06 · Inicio cliente | `InicioCliente.dc.html` |
| 07 · Problemas del oficio | `Tareas.dc.html` |
| 08 · Técnicos disponibles | `Trabajadores.dc.html` |
| 09 · Perfil del técnico | `Perfil.dc.html` |
| 10 · Solicitud: tareas | `Solicitud.dc.html` |
| 11 · Solicitud: dirección y precio | `SolicitudResumen.dc.html` |
| 12 · Solicitud enviada | `SolicitudEnviada.dc.html` |
| 13 · Inicio trabajador | `InicioTrabajador.dc.html` |
| 14 · Bandeja de solicitudes | `Bandeja.dc.html` |
| 15 · Detalle de solicitud | `SolicitudDetalle.dc.html` |
| 16 · Cotizar | `Cotizar.dc.html` |
| 17 · Revisar cotización | `RevisarCotizacion.dc.html` |
| 18 · Método de pago | `Pago.dc.html` |
| 19 · Confirmar rechazo | `RechazarCotizacion.dc.html` |
| 20 · Seguimiento (cliente) | `Seguimiento.dc.html` |
| 21 · Cancelar servicio (cliente) | `CancelarServicio.dc.html` |
| 22 · Re-cotización (cliente) | `ReCotizacionCliente.dc.html` |
| 23 · Servicio en curso (trabajador) | `ServicioCurso.dc.html` |
| 24 · Re-cotizar (trabajador) | `ReCotizar.dc.html` |
| 25 · Pagar con Wompi | `PagarWompi.dc.html` |
| 26 · Pago aprobado | `PagoAprobado.dc.html` |
| 27 · Pago rechazado | `PagoRechazado.dc.html` |
| 28 · Pago pendiente | `PagoPendiente.dc.html` |
| 29 · Efectivo registrado (cliente) | `PagoEfectivo.dc.html` |
| 30 · Calificar servicio | `Calificar.dc.html` |
| 31 · Recibí el pago (trabajador) | `RegistrarEfectivo.dc.html` |
| 32 · Registro trabajador: oficios | `OficiosTrabajador.dc.html` |
| 33 · Registro trabajador: documentos | `DocumentosTrabajador.dc.html` |
| 34 · Verificación pendiente | `VerificacionPendiente.dc.html` |
| 35 · Verificación rechazada | `VerificacionRechazada.dc.html` |
| 36 · Verificación aprobada | `VerificacionAprobada.dc.html` |
| 37 · Mi cuenta | `MiCuenta.dc.html` |
| 38 · Mis datos | `MisDatos.dc.html` |
| 39 · Eliminar cuenta | `EliminarCuenta.dc.html` |
| 40 · Notificaciones | `Notificaciones.dc.html` |
| 41 · Historial de solicitudes | `Historial.dc.html` |
| 42 · PQR | `PQRLista.dc.html` |
| 43 · Crear PQR | `PQRNueva.dc.html` |
| 44 · Técnicos en el mapa | `TrabajadoresMapa.dc.html` |
| 45 · Seguimiento: en camino (cliente) | `SeguimientoEnCamino.dc.html` |
| 46 · Seguimiento: en progreso (cliente) | `SeguimientoEnProgreso.dc.html` |
| 47 · Solicitud con tarea sin rango | `SolicitudSinRango.dc.html` |
| 48 · Servicio en camino (trabajador) | `ServicioEnCamino.dc.html` |
| 49 · Servicio en progreso (trabajador) | `ServicioEnProgreso.dc.html` |
| 50 · Cancelar servicio (trabajador) | `CancelarTrabajador.dc.html` |
| 51 · Mi cuenta (trabajador) | `MiCuentaTrab.dc.html` |
| 52 · Mis trabajos | `MisTrabajos.dc.html` |
| 53 · Notificaciones (trabajador) | `NotificacionesTrab.dc.html` |
| 54 · PQR (trabajador) | `PQRListaTrab.dc.html` |
| 55 · Seguimiento: completada (cliente) | `SeguimientoCompletada.dc.html` |
| 56 · Mapa (cliente) | `MapaCliente.dc.html` |
| 57 · Mapa conectado (trabajador) | `MapaTrabajador.dc.html` |
| 58 · Mapa desconectado (trabajador) | `MapaTrabajadorOff.dc.html` |
| A1 · Ingreso admin | `AdminLogin.dc.html` |
| A2 · Verificación de trabajadores | `AdminVerificacion.dc.html` |
| A3 · Revisar trabajador | `AdminVerificacionDetalle.dc.html` |
| A4 · Catálogo y tarifas | `AdminCatalogo.dc.html` |
| A5 · Usuarios y cuentas | `AdminUsuarios.dc.html` |
| A6 · Transacciones | `AdminTransacciones.dc.html` |
| A7 · PQR | `AdminPQR.dc.html` |
| A8 · Detalle de PQR | `AdminPQRDetalle.dc.html` |

La numeración 01-58 es la app móvil; A1-A8 es el panel web de administración (1280 px, adaptable).

### Panel web de administración (A1-A8)

El panel admin vive en el backend Django (`../ADS_AdminBack/backend`), no en esta app. Los mockups A1-A8 sirven de referencia visual para ese panel; **no tocarlos desde este proyecto** salvo que se indique.

### Decisiones pendientes del rediseño

- [ ] Inventario: qué pantallas ya existen y cuánto se alejan del mockup.
- [ ] Versión instalada de Material Components y si permite Material 3.
- [ ] Unidades reales de cada tarea (los mockups usan "Visita" y "Unidad" de ejemplo).
- [ ] Textos finales de las listas de motivos de cancelación.
- [ ] Modo oscuro: ¿se implementa en esta fase?
- [ ] Qué hacer con el `targetSdk 33` frente a `compileSdk 34` (no cambiar sin avisar).
