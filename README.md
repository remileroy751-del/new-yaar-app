# Yaar-App — version Supabase

Yaar-App est une application Android Kotlin/Jetpack Compose. Cette version utilise **Supabase exclusivement** pour l'authentification, PostgreSQL, Storage et la synchronisation distante. Les anciennes données Firebase ne sont pas migrées.

## Build GitHub

Le workflow `.github/workflows/android-build.yml` :

1. installe JDK 17 ;
2. installe le SDK Android API 36 ;
3. utilise Gradle 8.13 ;
4. lance `testDebugUnitTest assembleDebug` ;
5. publie l'APK debug comme artifact `yaar-app-debug`.

### Versions Android

- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 24`
- AGP 8.13.2
- Gradle 8.13
- Kotlin 2.3.21
- KSP 2.3.11
- JDK 17

Le passage à API 36 corrige l'échec `checkDebugAarMetadata` signalé par GitHub : certaines dépendances AndroidX utilisées par le projet exigent API 35 ou 36.

## Supabase

Voir `SUPABASE_SETUP.md` et `Yaar-App-Supabase-Final-Setup.sql`.

Configuration Auth attendue :

- Email : Enabled
- Confirm email : Disabled
- Phone : Disabled

L'utilisateur saisit toujours son numéro WhatsApp dans l'application. Un email technique interne est construit à partir du numéro normalisé et n'est pas affiché.

Buckets attendus :

- `products` : public
- `shops` : public
- `id_cards` : privé

Les images produits sont envoyées dans Supabase Storage avant la publication du produit. Les chemins utilisent une structure stable par utilisateur et produit.

## Architecture

- `data/YaarRepository.kt` : logique métier et interface du backend
- `data/SupabaseSync.kt` : Auth, PostgreSQL, Storage et synchronisation distante
- `supabase/SupabaseModule.kt` : client Supabase
- `data/YaarDatabase.kt` : persistance locale Room
- `ui/` : écrans Jetpack Compose
- `viewmodel/` : état et actions de l'application

Le champ historique `firebaseUid` dans les modèles Room est conservé volontairement pour compatibilité avec la base locale existante ; dans cette version il contient l'UID Supabase et aucune bibliothèque Firebase n'est utilisée.

## Compilation locale

Une installation Android Studio récente avec JDK 17 et SDK Android 36 est recommandée. Le dépôt est principalement destiné à être compilé par GitHub Actions avec le workflow fourni.

## Kkiapay

La clé publique Kkiapay peut être renseignée dans `app/src/main/java/com/yaarapp/app/util/KkiapayConfig.kt`. Ne jamais placer de clé privée ou de secret serveur dans l'application Android.

## SQL

Exécuter les scripts SQL dans **Supabase → SQL Editor**, jamais dans **Logs** :

1. `Yaar-App-Supabase-Initial-Schema.sql`
2. `Yaar-App-Supabase-Final-Setup.sql`

La partie Logs de Supabase utilise un moteur ClickHouse et n'est pas le SQL PostgreSQL de l'application.

## État de cette livraison

Cette archive contient le correctif API 36 demandé après le rapport de compilation GitHub. La compilation complète doit être confirmée par GitHub Actions ; l'environnement de préparation ne dispose pas d'une installation Android/Gradle permettant d'exécuter `assembleDebug` localement.
