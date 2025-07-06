plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.project.ads"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.project.ads"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildToolsVersion = "34.0.0"

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/LGPL2.1",
                "META-INF/AL2.0"
            )
        }
    }

    // SOLUCIÓN PARA CONFLICTOS DE KOTLIN
    configurations.all {
        resolutionStrategy {
            // Forzar una versión específica de Kotlin
            force("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:1.8.10")

            // Alternativa: excluir las versiones problemáticas
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        }
    }
}

dependencies {

    // Firebase BOM - Versión estable y probada
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))

    // AndroidX Core Libraries - Versiones estables
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core:1.10.1")

    // Material Design
    implementation("com.google.android.material:material:1.9.0")

    // Layout Libraries
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // MultiDex Support
    implementation("androidx.multidex:multidex:2.0.1")

    // Firebase Libraries (versiones gestionadas por BOM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-inappmessaging-display")

    // Google Play Services - Versiones estables
    implementation("com.google.android.gms:play-services-location:21.0.1") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("com.google.android.gms:play-services-maps:18.1.0") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("com.google.android.gms:play-services-auth:20.6.0") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("com.google.android.gms:play-services-base:18.2.0") {
        exclude(group = "org.jetbrains.kotlin")
    }

    // Google Places
    implementation("com.google.android.libraries.places:places:3.2.0") {
        exclude(group = "org.jetbrains.kotlin")
    }

    // GeoFire
    implementation("com.firebase:geofire-android:3.2.0")
    implementation("com.firebase:geofire-android-common:3.2.0")

    // UI Libraries - Versiones estables
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.saadahmedev.popup-dialog:popup-dialog:2.0.0")

    // PDF Library
    implementation("com.itextpdf:itextg:5.5.10")

    // Networking Libraries - Versiones estables
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")

    // Google Auth Library
    implementation("com.google.auth:google-auth-library-oauth2-http:1.17.0") {
        exclude(group = "org.jetbrains.kotlin")
    }

    // Forzar versión específica de Kotlin stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")

    // Annotation Processors
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // Testing Libraries
    testImplementation("junit:junit:4.13.2")

    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}