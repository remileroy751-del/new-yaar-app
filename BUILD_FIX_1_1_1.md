# Yaar-App 1.1.1 — correction de compilation

Cette version corrige les deux erreurs Kotlin remontées par GitHub Actions dans la version 1.1.0.

## Corrections

1. `FirebaseModule.kt`
   - `linkAnonymousAccount()` utilisait par erreur un opérateur Elvis entre `FirebaseUser` et `String` (l'UID retourné par `signInAnonymouslyUser()`).
   - La variable `current` est maintenant toujours un `FirebaseUser`, puis `linkWithCredential()` est appelé correctement.

2. `AddProductScreen.kt`
   - `PickVisualMediaRequest` était importé depuis `androidx.activity.compose`, où il n'existe pas.
   - Import corrigé vers `androidx.activity.result.PickVisualMediaRequest`.

3. Version Android
   - `versionCode = 3`
   - `versionName = 1.1.1`

Le reste de l'architecture Firebase, des comptes, de la récupération des boutiques/produits et de la sélection géographique est conservé.
