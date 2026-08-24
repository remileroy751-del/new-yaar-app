plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Le plugin Google Services (Firebase) n'est appliqué QUE si google-services.json est
// présent dans ce dossier (app/). Tant qu'il n'y est pas, le projet compile normalement
// en mode 100% local (Room) — voir BACKEND_FIREBASE.md pour l'ajouter.
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.yaarapp.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yaarapp.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Local persistence for cart & products
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // DataStore for simple prefs (onboarding flag, delivery zone, etc.)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Firebase — uniquement si google-services.json est présent (voir plus haut).
    // Le BOM gère les versions compatibles entre les modules Firebase automatiquement.
    // Depuis le BoM 34.0.0 (juillet 2025), Firebase a retiré les modules "-ktx" séparés :
    // les API Kotlin (ex. Firebase.firestore) sont désormais directement dans les modules
    // principaux ci-dessous — ne PAS ajouter de suffixe "-ktx", ça ne compilerait plus.
    if (hasFirebaseConfig) {
        implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
        implementation("com.google.firebase:firebase-firestore")
        implementation("com.google.firebase:firebase-auth")
        implementation("com.google.firebase:firebase-storage")
        implementation("com.google.firebase:firebase-messaging")
        // Permet d'utiliser .await() sur les Task Firebase depuis des coroutines Kotlin.
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    }

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
