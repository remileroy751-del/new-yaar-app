import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Compilateur Compose (Kotlin 2.0+) — remplace composeOptions.kotlinCompilerExtensionVersion.
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Yaar-App est une marketplace en ligne : Firebase est donc obligatoire pour la
// version distribuée. Le plugin Google Services doit traiter app/google-services.json
// pendant chaque compilation afin de générer les ressources Firebase utilisées par
// FirebaseApp.initializeApp(). Si le fichier manque, on préfère faire échouer la
// compilation plutôt que produire un APK qui fonctionne seulement en local.
val firebaseConfig = file("google-services.json")
check(firebaseConfig.exists()) {
    "google-services.json est introuvable dans app/. " +
        "Ajoutez le fichier Firebase ou configurez le secret GitHub Actions GOOGLE_SERVICES_JSON."
}
apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.yaarapp.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yaarapp.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

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
    // NOTE : "kotlinOptions { jvmTarget = ... }" est supprimé depuis Kotlin 2.2 (pas
    // seulement déprécié) — le réglage équivalent se fait maintenant via le bloc
    // kotlin { compilerOptions { ... } } tout en bas de ce fichier.

    buildFeatures {
        compose = true
    }
    // NOTE : plus de bloc composeOptions { kotlinCompilerExtensionVersion = ... } ici —
    // depuis Kotlin 2.0, le compilateur Compose est piloté par le plugin
    // "org.jetbrains.kotlin.plugin.compose" (voir plugins{} ci-dessus et build.gradle.kts
    // racine), dont la version doit toujours suivre celle du plugin Kotlin.

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
    // Room 2.8.4 (et non 2.6.1) : version minimale nécessaire pour que le compilateur
    // Room (via KSP2, Kotlin 2.3.21) traite correctement les fonctions "suspend" des
    // DAO — les versions antérieures à 2.7.0 provoquent une erreur connue
    // "IllegalStateException: unexpected jvm signature V" sur KSP2. Toujours sous le
    // paquet androidx.room (pas androidx.room3, qui est une réécriture majeure séparée
    // avec renommage de tous les imports — non nécessaire ici).
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // DataStore for simple prefs (onboarding flag, delivery zone, etc.)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Firebase — configuration obligatoire pour la version en ligne.
    // Le BOM gère les versions compatibles entre les modules Firebase automatiquement.
    // Depuis le BoM 34.0.0 (juillet 2025), Firebase a retiré les modules "-ktx" séparés :
    // les API Kotlin (ex. Firebase.firestore) sont désormais directement dans les modules
    // principaux ci-dessous — ne PAS ajouter de suffixe "-ktx", ça ne compilerait plus.
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    // Permet d'utiliser .await() sur les Task Firebase depuis des coroutines Kotlin.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Remplace l'ancien "android.kotlinOptions" (supprimé depuis Kotlin 2.2).
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}
