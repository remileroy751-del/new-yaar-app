# Yaar-App — correctif de compilation GitHub (API 36)

## Erreur corrigée

Le build GitHub échouait sur `:app:checkDebugAarMetadata` parce que plusieurs dépendances AndroidX exigeaient un `compileSdk` plus récent alors que l'application était compilée avec `android-34`.

La dépendance la plus contraignante était `androidx.browser:browser:1.10.0`, qui exige API 36. Plusieurs composants Compose/Lifecycle exigeaient au minimum API 35.

## Correction

- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 24` conservé
- Le workflow GitHub installe explicitement `platforms;android-36`.
- JDK 17 et Gradle 8.13 sont conservés.

API 36 est compatible avec AGP 8.13.x.

## Vérifications effectuées avant livraison

- recherche des anciennes configurations Firebase dans les sources Gradle/Kotlin/XML/JSON : aucune dépendance Firebase active ;
- vérification de l'absence de `service_role` et de secrets Supabase dans les sources ;
- vérification des références `compileSdk` / `targetSdk` : API 36 partout dans le module Android ;
- vérification de la structure du workflow GitHub Actions ;
- vérification de l'intégrité de l'archive ZIP avec `unzip -tq`.

## Limite

La compilation Android complète ne peut pas être exécutée dans l'environnement de préparation. Le build réel doit être confirmé par GitHub Actions. Le correctif traite directement l'erreur AAR metadata fournie dans le rapport GitHub.
