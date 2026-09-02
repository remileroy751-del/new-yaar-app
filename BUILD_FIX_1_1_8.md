# Yaar-App 1.1.8 — corrections de compilation

Corrections appliquées à partir du rapport GitHub du 2 septembre 2026 :

- `YaarNavHost.kt` : remplacement du delegate `var ... by remember` problématique par un accès explicite à `MutableState.value`.
- `ProductCard.kt` : même correction pour l'état `imageFailed`, afin d'éviter les erreurs de delegate Kotlin.
- `ProfileScreen.kt` : import explicite de `IconButton`.
- `ChatScreen.kt` : opt-in explicite à `ExperimentalMaterial3Api`.
- `TermsAndConditionsScreen.kt` : opt-in explicite à `ExperimentalMaterial3Api`.
- Version Android : 1.1.8 / versionCode 10.

Le message `libdatastore_shared_counter.so` est un avertissement de packaging et n'est pas la cause de l'échec Kotlin.
