// Top-level build file
plugins {
    id("com.android.application") version "8.5.2" apply false
    // Kotlin 2.3.21 : requis pour lire les métadonnées des bibliothèques Firebase
    // récentes (firebase-auth 24.2.0 est compilé avec metadata 2.3.0, illisible par
    // un compilateur Kotlin 1.9.x — voir l'erreur "incompatible version of Kotlin").
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    // Depuis Kotlin 2.0, le compilateur Compose fait partie du dépôt Kotlin et se
    // configure via ce plugin dédié (remplace composeOptions.kotlinCompilerExtensionVersion,
    // retiré de app/build.gradle.kts) — sa version doit toujours être identique à celle
    // du plugin Kotlin ci-dessus.
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    // Google Services (Firebase) : appliqué conditionnellement dans app/build.gradle.kts,
    // uniquement si google-services.json est présent — voir BACKEND_FIREBASE.md.
    id("com.google.gms.google-services") version "4.5.0" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
