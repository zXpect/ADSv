# ADS - Aplicación Móvil

Aplicación Android nativa del sistema ADS, una plataforma que conecta a
clientes con trabajadores independientes de reparaciones locativas (hogar y
comercio) en Ubaté, Cundinamarca. Permite a los clientes encontrar técnicos
verificados cerca de su ubicación, solicitar un servicio, recibir una
cotización, pagar y calificar; y a los trabajadores registrarse, acreditar su
documentación, recibir solicitudes, cotizar y gestionar sus trabajos.

La aplicación usa Firebase (Authentication, Realtime Database, Storage y Cloud
Messaging) y la API REST del backend de ADS
([Panel_admln_ADS_backend](https://github.com/zXpect/Panel_admln_ADS_backend))
para las operaciones que requieren validación en el servidor: cotización,
pagos, calificación, PQR, notificaciones y eliminación de cuenta.

## Tabla de contenido

1. [Funcionalidades](#funcionalidades)
2. [Flujo del servicio](#flujo-del-servicio)
3. [Arquitectura](#arquitectura)
4. [Tecnologías](#tecnologías)
5. [Estructura del proyecto](#estructura-del-proyecto)
6. [Requisitos](#requisitos)
7. [Instalación](#instalación)
8. [Configuración](#configuración)
9. [Compilación y ejecución](#compilación-y-ejecución)
10. [Pruebas](#pruebas)
11. [Seguridad](#seguridad)
12. [Contribución](#contribución)
13. [Licencia](#licencia)

## Funcionalidades

### Cliente

- Registro e inicio de sesión con correo y contraseña, con aceptación de
  términos y condiciones y de la política de tratamiento de datos personales.
- Búsqueda de técnicos por oficio y tarea, en lista o en mapa, con distancia,
  calificación y estado de verificación.
- Perfil del técnico con oficios, trabajos realizados y calificación.
- Creación de solicitudes con tareas, descripción y dirección, y precio de
  referencia estimado para Ubaté.
- Revisión de la cotización: aceptar (eligiendo pago en línea o en efectivo) o
  rechazar.
- Seguimiento del servicio en tiempo real, re-cotización y cancelación con motivo.
- Pago en línea mediante Wompi desde una pestaña segura del navegador.
- Calificación del servicio de 1 a 5 estrellas.
- Historial de solicitudes, bandeja de notificaciones y PQR.

### Trabajador

- Registro en varios pasos con hasta tres oficios, tareas que realiza y
  descripción.
- Carga de documentos de verificación (hoja de vida, antecedentes judiciales,
  títulos y cartas de recomendación) y consulta del estado de verificación.
- Interruptor de conexión: desconectado no aparece en el mapa ni recibe
  solicitudes nuevas.
- Bandeja de solicitudes, detalle y cotización con agenda y motivo de ajuste.
- Gestión del servicio: iniciar viaje, iniciar trabajo, finalizar, re-cotizar,
  reprogramar y cancelar con motivo.
- Registro de pagos recibidos en efectivo.
- Historial de trabajos, estadísticas del perfil y PQR.

### Comunes

- Notificaciones push de nuevas solicitudes, cotizaciones, cambios de estado,
  pagos, recordatorios y respuestas a PQR.
- Consulta y edición de datos personales, documentos legales y eliminación de
  la cuenta.
- Modo claro y oscuro.

## Flujo del servicio

```
Pendiente -> Cotizada -> Aceptada -> En camino -> En progreso -> Completada -> Pagada -> Calificada
     \            \           \
      +------------+-----------+---> Cancelada (con motivo)
```

- El precio de referencia se calcula a partir del catálogo de tareas, con una
  tarifa mínima por visita como piso.
- El trabajador puede re-cotizar una sola vez, con el servicio en camino o en
  progreso; el cliente acepta o rechaza el nuevo precio.
- El cliente puede cancelar antes de que el trabajador inicie el viaje.

## Arquitectura

La aplicación sigue una arquitectura por capas del tipo "Activity + Provider":

| Capa | Responsabilidad |
|---|---|
| Activities | Interfaz y lógica de cada pantalla, organizadas por rol (`client`, `worker`) |
| Providers | Acceso a datos: un proveedor por entidad sobre Firebase o sobre la API del backend |
| Models | Objetos que representan los nodos de Firebase y las respuestas de la API |
| Adapters | Adaptadores de `RecyclerView` |
| Helpers | Utilidades transversales: diálogos, validaciones, motivos, notificaciones |
| Retrofit | Cliente HTTP del backend con autenticación por ID token de Firebase |

```
+--------------------+       SDK de Firebase       +----------------------------+
|                    | --------------------------> | Firebase Auth, Realtime DB,|
|  Aplicación ADS    |                             | Storage, Cloud Messaging   |
|  (Android)         |    HTTPS + ID token         +----------------------------+
|                    | --------------------------> |  API Django de ADS         |
+--------------------+                             +----------------------------+
```

Las reglas de seguridad de Firebase (mantenidas en el repositorio del backend)
limitan lo que la aplicación puede leer y escribir; los campos de cotización y
pago solo los modifica el servidor.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java |
| Plataforma | Android (minSdk 23, targetSdk 33, compileSdk 34) |
| Construcción | Gradle 8.0 con Kotlin DSL, Android Gradle Plugin 8.1 |
| Interfaz | Material Components, ConstraintLayout, RecyclerView, tipografía Sora |
| Backend como servicio | Firebase Authentication, Realtime Database, Storage, Cloud Messaging, Crashlytics |
| Mapas y ubicación | Google Maps SDK, Places SDK, Fused Location Provider, GeoFire |
| Red | Retrofit 2, OkHttp 4, Gson |
| Imágenes | Glide, CircleImageView |
| Pagos | Wompi Web Checkout mediante Custom Tabs (`androidx.browser`) |
| Pruebas | JUnit 4 |

## Estructura del proyecto

```
app/src/main/java/com/project/ads/
├── activities/
│   ├── client/         Pantallas del cliente (búsqueda, solicitud, pago, calificación)
│   ├── worker/         Pantallas del trabajador (registro, documentos, bandeja, servicio)
│   └── ...             Pantallas comunes (inicio de sesión, rol, PQR, notificaciones)
├── adapters/           Adaptadores de listas
├── providers/          Acceso a Firebase y a la API del backend
├── models/             Modelos de datos
│   └── api/            Cuerpos y respuestas de la API
├── helpers/            Utilidades transversales
├── retrofit/           Cliente del backend e interceptor del ID token
├── services/           Servicio de mensajería de Firebase (notificaciones push)
├── channel/            Canales de notificación de Android
└── includes/           Componentes de interfaz reutilizables
app/src/main/res/       Layouts, recursos gráficos, colores (claro y oscuro) y textos
app/src/test/           Pruebas unitarias
web/                    Página pública y términos y condiciones
```

## Requisitos

- Android Studio (Giraffe o posterior) con su JDK incluido (JBR 17). Gradle 8.0
  no se ejecuta con JDK 21.
- Android SDK 34.
- Un dispositivo o emulador con Android 6.0 (API 23) o superior y Google Play Services.
- El backend de ADS en ejecución, local o desplegado.
- Acceso al proyecto de Firebase y una clave de Google Maps con Maps SDK y
  Places API habilitados.

## Instalación

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/zXpect/ADSv.git
   ```

2. Abrir la carpeta en Android Studio y esperar la sincronización de Gradle.
   No aceptar el asistente de actualización del Android Gradle Plugin.

3. Agregar los archivos locales descritos en [Configuración](#configuración).

## Configuración

Los siguientes archivos no se versionan y deben crearse localmente:

| Archivo | Contenido | Origen |
|---|---|---|
| `app/google-services.json` | Configuración de Firebase de la aplicación `com.project.ads` | Consola de Firebase > Configuración del proyecto > Aplicación Android |
| `app/src/main/res/values/secret.xml` | Clave de Google Maps y Places | Google Cloud Console |
| `local.properties` | Ruta del Android SDK | Android Studio lo genera automáticamente |

Ejemplo de `secret.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_maps_key" translatable="false">TU_API_KEY</string>
</resources>
```

### URL del backend

La URL se define por tipo de compilación en `app/build.gradle.kts` y puede
sobrescribirse en `~/.gradle/gradle.properties`:

| Propiedad | Uso | Valor por defecto |
|---|---|---|
| `ADS_API_BASE_URL_DEBUG` | Compilación de depuración | `http://10.0.2.2:8000/` (el equipo local visto desde el emulador) |
| `ADS_API_BASE_URL_RELEASE` | Compilación de publicación (solo HTTPS) | URL del backend desplegado |

Para un teléfono físico:

```properties
# Teléfono en la misma red Wi-Fi que el equipo
ADS_API_BASE_URL_DEBUG=http://192.168.1.50:8000/
# Teléfono por USB, después de ejecutar: adb reverse tcp:8000 tcp:8000
ADS_API_BASE_URL_DEBUG=http://127.0.0.1:8000/
```

El host que use el teléfono debe estar incluido en `ALLOWED_HOSTS` del backend.

### Firma de publicación

La compilación de publicación se firma con una llave propia. Sus datos se
configuran únicamente en `~/.gradle/gradle.properties`:

```properties
ADS_KEYSTORE_FILE=C:/ruta/app_key.jks
ADS_KEYSTORE_PASSWORD=...
ADS_KEY_ALIAS=...
ADS_KEY_PASSWORD=...
```

Sin estas propiedades, la compilación de publicación se firma con la llave de
depuración y solo sirve para pruebas.

## Compilación y ejecución

En Windows, usar `gradlew.bat` en lugar de `./gradlew`.

| Comando | Descripción |
|---|---|
| `./gradlew assembleDebug` | Compila el APK de depuración |
| `./gradlew installDebug` | Compila e instala en el dispositivo o emulador conectado |
| `./gradlew assembleRelease` | Compila el APK de publicación, ofuscado con R8, en `app/build/outputs/apk/release/` |
| `./gradlew testDebugUnitTest` | Ejecuta las pruebas unitarias |
| `./gradlew lint` | Análisis estático de Android |

También es posible ejecutar la aplicación directamente desde Android Studio con
el botón **Run**.

Uso básico:

1. Iniciar el backend y abrir la aplicación.
2. Registrarse como cliente o como trabajador.
3. Un trabajador debe cargar sus documentos y ser aprobado desde el panel de
   administración antes de aparecer en las búsquedas.
4. Con un cliente y un trabajador aprobado y conectado, crear una solicitud y
   recorrer el flujo de cotización, servicio, pago y calificación.

## Pruebas

Las pruebas unitarias (JUnit) se ubican en `app/src/test/java/com/project/ads/`
y cubren la lógica independiente de Android: cotización, motivos, agenda,
estados de la solicitud, PQR, reglas de distintivos, ordenamiento y enrutamiento
de notificaciones. Se ejecutan en el equipo, sin dispositivo:

```bash
./gradlew testDebugUnitTest
```

Las pantallas se validan mediante casos de prueba funcionales y pruebas de
aceptación con usuarios.

## Seguridad

La aplicación aplica recomendaciones de OWASP MASVS:

- Autenticación con Firebase Authentication; las peticiones al backend envían el
  ID token de Firebase, que se renueva automáticamente ante un `401`.
- Ninguna credencial de servicio se incluye en la aplicación; las claves de
  Firebase y Google Maps se configuran en archivos locales excluidos del control
  de versiones.
- Reglas de seguridad de Firebase que restringen cada usuario a sus propios datos
  y reservan al servidor los campos de cotización y pago.
- Tráfico solo por HTTPS en la compilación de publicación (`network_security_config`).
- Copia de seguridad del sistema deshabilitada (`allowBackup="false"`).
- Sin datos personales en los registros; R8 elimina los registros en la
  compilación de publicación y ofusca el código.
- Pago realizado en la página de Wompi dentro de una pestaña del navegador; la
  aplicación nunca recibe datos de tarjetas.

## Contribución

1. Crear una rama a partir de `develop`: `feature/<nombre-corto>` para
   funcionalidades o `fix/<nombre-corto>` para correcciones.
2. Hacer commits con el formato [Conventional Commits](https://www.conventionalcommits.org/es/),
   por ejemplo `feat(ui): add worker availability toggle`.
3. Compilar, ejecutar las pruebas unitarias y `lint` antes de abrir el Pull
   Request hacia `develop`.
4. Después de modificar las reglas de R8 (`app/proguard-rules.pro`), probar el APK
   de publicación en un dispositivo real.
5. La rama `main` se reserva para versiones publicadas.

## Licencia

Proyecto privado desarrollado como trabajo de grado en la Universidad de
Cundinamarca. Todos los derechos reservados.
