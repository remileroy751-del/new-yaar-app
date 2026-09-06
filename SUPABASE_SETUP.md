# Yaar-App — configuration Supabase finale

## 1. Base de données

Le fichier `Yaar-App-Supabase-Initial-Schema.sql` crée les tables, RLS et Storage buckets.

Le fichier `Yaar-App-Supabase-Final-Setup.sql` complète les permissions Data API et la suppression sécurisée du compte.

**Important : exécuter ces fichiers dans `Supabase Dashboard → SQL Editor`. Ne pas les exécuter dans `Logs`.**

Le message « Logs now run on a ClickHouse-backed engine » signifie que l'éditeur de logs utilise ClickHouse et n'est pas le PostgreSQL de l'application.

## 2. Authentification

Dans `Authentication → Sign In / Providers` :

- Email : **Enabled**
- Confirm email : **Disabled**
- Phone : **Disabled**

L'utilisateur continue de voir uniquement son numéro WhatsApp dans Yaar-App. L'application transforme ce numéro en identifiant email technique interne :
`<numero-normalise>@login.yaar-app.com`.

Aucun mot de passe ou `service_role` n'est embarqué dans l'application.

## 3. Storage

Buckets attendus :

- `products` : public
- `shops` : public
- `id_cards` : privé

Les photos produits sont stockées sous `products/<uid>/<productId>.<extension>` et l'URL publique est enregistrée dans `products.image_url`.

## 4. Compilation GitHub

Le projet compile avec `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`. Le workflow installe explicitement la plateforme Android API 36.

Le workflow `.github/workflows/android-build.yml` installe Gradle 8.13 et exécute :

`gradle testDebugUnitTest assembleDebug --stacktrace --no-daemon`

Puis publie l'APK debug comme artifact `yaar-app-debug`.

## 5. Architecture finale

Firebase n'est plus utilisé par cette version. Supabase fournit :

- Auth
- PostgreSQL / Data API
- Storage
- synchronisation distante par polling

Le polling est volontairement utilisé pour les données métier afin que le fonctionnement ne dépende pas d'une publication Realtime configurée sur les tables.
