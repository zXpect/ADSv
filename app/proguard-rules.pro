# Reglas de R8 para el APK de release (MASVS M7: ofuscación; M4: sin logs).
# R8 corre en modo compatible (android.enableR8.fullMode=false en gradle.properties):
# el modo completo de AGP 8 quitaba las firmas genéricas que leen GeoFire, Firebase,
# Retrofit y Gson, y el mapa se cerraba. Después de cambiar estas reglas, probar el
# APK de release en un teléfono: lo que se lee por reflexión falla solo en ejecución.

# Trazas de Crashlytics legibles (el mapping se sube con el build)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# ---------- Modelos ----------
# Firebase (getValue(Worker.class)...) y Gson (respuestas de la API, task_items)
# los llenan por reflexión con el nombre de cada campo, getter y setter, y
# necesitan el constructor vacío: no se renombran ni se quitan.
-keep class com.project.ads.models.** { *; }

# ---------- Firebase Realtime Database y GeoFire ----------
# GenericTypeIndicator<Map<...>> se lee por la firma genérica de la subclase anónima;
# en modo completo R8 la quita y GeoFire falla con «Got Datasnapshot without location»
# (mapa y búsqueda de técnicos cercanos se cerraban en release).
-keep class com.google.firebase.database.GenericTypeIndicator { *; }
-keep class * extends com.google.firebase.database.GenericTypeIndicator { *; }
-keep class com.firebase.geofire.** { *; }

# ---------- Retrofit ----------
# Las interfaces de la API se leen por reflexión (anotaciones y tipos genéricos
# como Call<ApiResponse<List<Pqr>>>); en modo completo R8 quita la firma genérica
# si no se conservan.
-keep interface com.project.ads.retrofit.IAdsApi { *; }
-keep interface com.project.ads.retrofit.IGoogleApi { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# ---------- Gson ----------
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---------- Glide (generado por el compilador de anotaciones) ----------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }

# ---------- MASVS M4: sin logs en release ----------
# R8 elimina las llamadas a android.util.Log (y el armado del texto, que queda sin uso):
# en el APK de release no se escribe nada en logcat, ni uid ni datos de los push.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static boolean isLoggable(java.lang.String, int);
}

# «Deslizar para actualizar» con la tuerca de ADS: RefreshNut lee este campo por reflexión
-keepclassmembers class androidx.swiperefreshlayout.widget.SwipeRefreshLayout {
    android.widget.ImageView mCircleView;
}
