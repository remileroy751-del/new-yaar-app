# Yaar-App 1.1.3 — correction de compilation

Correction du build GitHub Actions signalé dans `FirestoreSync.kt`.

Le type retourné par Firebase `DocumentSnapshot.data` est une map Java nullable (`MutableMap<String!, Any!>?`) et ne correspond pas directement au type Kotlin `Map<String, Any?>` utilisé par les fonctions de mapping.

La version 1.1.3 normalise explicitement les données Firestore avant `shopFromDocument()` et `productFromDocument()`.

Version Android : versionCode 5, versionName 1.1.3.

Le message `Unable to strip libdatastore_shared_counter.so` reste un avertissement et n'est pas une cause d'échec.
