# ADSv — App móvil de ADS

App Android nativa en Java que conecta clientes con trabajadores de reparaciones
locativas en Ubaté. Comparte Firebase (Auth, Realtime Database, Storage) con el
backend Django `Panel_admln_ADS_backend`, que además atiende cotización, pagos,
calificación y notificaciones. Convenciones del proyecto en `CLAUDE.md`.

## Requisitos

- Android Studio con su JBR 17 (Gradle 8.0 no corre con JDK 21).
- SDK de Android 34 (`compileSdk 34`, `minSdk 23`).
- El backend corriendo en el puerto 8000 para cotizar, pagar y calificar.

## Archivos locales (no están en el repo)

Pedirlos al equipo o generarlos desde las consolas; están en `.gitignore`:

| Archivo | Contenido |
|---|---|
| `app/google-services.json` | Consola de Firebase > Configuración del proyecto > App Android `com.project.ads` |
| `app/src/main/res/values/secret.xml` | `<string name="google_maps_key">…</string>` (Maps y Places) |
| `local.properties` | `sdk.dir=…` (Android Studio lo crea solo) |

Ejemplo de `secret.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_maps_key" translatable="false">TU_API_KEY</string>
</resources>
```

Las llaves de firma (`*.jks`, `*.keystore`) tampoco se versionan.

## URL del backend

Debug usa `http://10.0.2.2:8000/` (emulador -> localhost del PC). Para un teléfono
físico, poner en `~/.gradle/gradle.properties` una de estas opciones:

```properties
# Teléfono en la misma red Wi-Fi que el PC
ADS_API_BASE_URL_DEBUG=http://192.168.1.50:8000/
# Teléfono por USB con `adb reverse tcp:8000 tcp:8000`
ADS_API_BASE_URL_DEBUG=http://127.0.0.1:8000/
```

El host que use el teléfono debe estar en `ALLOWED_HOSTS` del `.env` del backend.

## Compilar e instalar

```bash
./gradlew assembleDebug
./gradlew installDebug
```
