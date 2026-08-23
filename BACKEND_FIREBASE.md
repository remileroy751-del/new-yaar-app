# Base de données en ligne gratuite — pourquoi Firebase, et comment la brancher

## Pourquoi Firebase (Firestore) et pas autre chose

Aujourd'hui, Yaar-App stocke tout **uniquement sur le téléphone** (base Room/SQLite
locale). C'est parfait pour tester, mais ça veut dire que **chaque vendeur et chaque
acheteur voit une base différente** — un acheteur au Togo ne peut pas voir les
produits publiés depuis un téléphone au Bénin.

Pour que tout le monde voie la même chose (les mêmes boutiques, les mêmes produits,
en temps réel), il faut une base de données **en ligne**, partagée par tous les
téléphones. Recommandation : **Firebase Firestore** (Google), pour ces raisons :

- **Gratuit pour démarrer** (offre "Spark") : env. 1 Go de stockage, ~50 000
  lectures/jour et ~20 000 écritures/jour gratuits — largement suffisant pour
  lancer l'app dans les 4 pays et voir venir.
- **Fait pour le mobile** : SDK Android officiel, synchronisation en temps réel,
  fonctionne même avec une connexion instable (les écritures se mettent en attente
  puis se synchronisent).
- **Aucun serveur à gérer** : pas de VPS, pas de code backend à écrire/héberger.
- **Tri et filtres intégrés** (ex : tous les produits d'une ville, triés par date)
  — exactement ce qu'il faut pour "Disponible à Lomé", recherche par ville, etc.
- Alternative valable si vous préférez du SQL : **Supabase** (PostgreSQL, offre
  gratuite généreuse aussi). La logique ci-dessous s'adapterait facilement.

## Ce qui est déjà prêt côté code

Le code actuel (Room) est organisé pour que la bascule soit simple :
- Toute la logique métier passe par `YaarRepository` — c'est le SEUL endroit à
  modifier pour brancher Firestore, les écrans (`MarketplaceScreen`, `MyShopScreen`,
  etc.) n'ont pas à changer.
- Les entités `User`, `Shop`, `Product` sont déjà des `data class` simples,
  directement sérialisables en documents Firestore.
- Pays/ville sont déjà des champs structurés (`Country`, `city: String`), prêts pour
  des requêtes `whereEqualTo("country", ...)`, `whereEqualTo("city", ...)`.

## Étapes pour créer le projet Firebase (à faire vous-même, ~15 min)

Je ne peux pas créer le projet à votre place (il faut votre propre compte Google),
mais voici exactement la marche à suivre :

1. Allez sur **https://console.firebase.google.com** et connectez-vous avec un
   compte Google.
2. **Ajouter un projet** → nommez-le par ex. "Yaar-App" → décochez Google
   Analytics si vous ne voulez pas vous en occuper tout de suite → Créer.
3. Dans le projet, cliquez sur l'icône **Android** pour ajouter une app :
   - Nom du package : `com.yaarapp.app` (doit correspondre exactement à
     `applicationId` dans `app/build.gradle.kts`).
   - Téléchargez le fichier **`google-services.json`** généré et placez-le dans
     `YaarApp/app/google-services.json` (à la racine du dossier `app`).
4. Dans le menu de gauche → **Build → Firestore Database** → **Créer une base de
   données** → choisissez une région proche (ex. `europe-west1`) → démarrez en
   **mode production** (les règles de sécurité ci-dessous protègent les données).
5. Onglet **Règles**, remplacez par :

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Tout le monde peut LIRE les boutiques et produits (marketplace public)
    match /shops/{shopId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == resource.data.ownerId;
    }
    match /products/{productId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    // Un utilisateur ne peut lire/modifier que son propre profil
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

   (Ces règles supposent que vous activez aussi **Firebase Authentication**
   — voir note plus bas — pour que `request.auth` existe.)

## Étapes pour brancher le code Android

1. Dans `YaarApp/build.gradle.kts` (racine), ajoutez le plugin Google Services :

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false   // AJOUT
}
```

2. Dans `YaarApp/app/build.gradle.kts` :

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")   // AJOUT
}

dependencies {
    // ... dépendances existantes ...

    // Firebase (BOM = gère les versions compatibles entre elles automatiquement)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
}
```

3. Structure des collections Firestore à créer (elles se créent toutes seules à la
   première écriture, rien à faire manuellement) — voir le schéma complet et à jour
   dans la section **"Mise à jour du schéma"** plus bas (`users`, `shops`, `products`,
   `ad_campaigns`, `interests`).

4. Exemple de requête avec **tri** (le "tri des résultats" que vous avez demandé) —
   récupérer les produits actifs d'une ville, du plus récent au plus ancien :

```kotlin
firestore.collection("products")
    .whereEqualTo("isActive", true)
    .whereEqualTo("city", "Lomé")
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .get()
```

5. Stratégie recommandée pour la migration : garder Room comme **cache local**
   (l'app reste rapide et fonctionne hors-ligne) et ajouter dans `YaarRepository`
   un "write-through" vers Firestore à chaque `addProduct`, `createShop`,
   `signUp`, etc., + un `addSnapshotListener` Firestore qui met à jour Room quand
   un autre téléphone publie un nouveau produit. C'est le sujet du prochain lot de
   travail une fois le projet Firebase créé de votre côté — dites-moi quand
   `google-services.json` est en place et je branche le code.

## Authentification

Pour que chaque vendeur ne modifie que SA boutique, il faut relier vos comptes
"WhatsApp + mot de passe" actuels à **Firebase Authentication**. Le plus simple :
Firebase Auth par **numéro de téléphone (OTP SMS)**, en réutilisant directement le
numéro WhatsApp déjà collecté au format `00<indicatif><numéro>` (il suffit de le
convertir en `+<indicatif><numéro>` pour Firebase, qui utilise le format E.164).

## Mise à jour du schéma (lot "capacité produits payante" + campagnes publicitaires + certification)

Depuis la dernière version de ce document, le modèle de tarification a changé (plus
de `Plan` à paliers — juste une capacité 5→20 achetée une fois) et deux nouvelles
fonctionnalités sont apparues : les campagnes publicitaires calculées, et la
certification de boutique. Schéma Firestore à jour :

```
users/{uid}          → firstName, sex, country, city, whatsappNumber,
                        notificationsEnabled (Boolean), createdAt

shops/{shopId}        → ownerId, name, whatsappNumber, country, city,
                        logoUrl (String?), activityDescription, categories (List<String>, max 3),
                        extraProductSlots (Int, 0 ou 15 — capacité 5→20 produits),
                        certificationStatus ("NONE"|"PENDING"|"CERTIFIED"),
                        idCardFrontUrl (String?), idCardBackUrl (String?),
                        certificationRequestedAt (Long?), createdAt

products/{productId}  → shopId, shopName, name, description, price, imageUrl, category,
                        country, city, isActive, isPromoted (Boolean),
                        createdAt, activatedAt

ad_campaigns/{id}     → productId, productName, shopId, totalExpositions,
                        remainingExpositions, durationDays, startedAt, endsAt,
                        priceFcfa, isActive (Boolean)

interests/{id}         → productId, productName, productImageUrl, shopId, shopOwnerId,
                        buyerId, buyerFirstName, buyerWhatsappNumber,
                        status ("PENDING"|"AVAILABLE"|"UNAVAILABLE"), isRead, createdAt
```

### Le moteur d'exposition publicitaire doit devenir une Cloud Function

Actuellement (`YaarRepository.recordAppOpenExposure`), chaque campagne perd une
exposition à chaque ouverture de l'app **sur ce même téléphone** — ça ne marche que
pour tester en solo. Une fois Firestore branché, remplacez cet appel local par une
**Cloud Function déclenchée par une requête HTTPS** (appelée une fois au démarrage de
l'app, comme aujourd'hui, mais côté serveur cette fois) :

```javascript
// functions/index.js (Cloud Functions for Firebase, Node.js)
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.recordAppOpenExposure = functions.https.onCall(async () => {
  const db = admin.firestore();
  const now = Date.now();
  const snapshot = await db.collection("ad_campaigns").where("isActive", "==", true).get();

  const batch = db.batch();
  for (const doc of snapshot.docs) {
    const campaign = doc.data();
    const newRemaining = Math.max(0, campaign.remainingExpositions - 1);
    const stillRunning = newRemaining > 0 && now < campaign.endsAt;
    batch.update(doc.ref, { remainingExpositions: newRemaining, isActive: stillRunning });
    if (!stillRunning) {
      batch.update(db.collection("products").doc(campaign.productId), { isPromoted: false });
    }
  }
  await batch.commit();
});
```

Cette fonction s'exécute pour **tous les utilisateurs de tous les téléphones** —
c'est ce qui rend le comptage des expositions réellement partagé, comme demandé
("apparaît sur 100 profils dès l'ouverture de l'application").

### Vérification manuelle de la certification

`certificationStatus` passe à `"PENDING"` automatiquement après le paiement Kkiapay
(côté app). Il n'existe pas encore d'interface pour l'étudier — le plus simple pour
démarrer est de consulter directement la collection `shops` dans la **console
Firebase → Firestore Database**, d'ouvrir le document, de regarder les photos
(`idCardFrontUrl` / `idCardBackUrl`, stockées sur **Firebase Storage**, voir
ci-dessous) et de changer manuellement `certificationStatus` à `"CERTIFIED"` une
fois la pièce vérifiée. Une mini-interface d'administration (page web réservée à
vous) pourra être ajoutée plus tard pour éviter de le faire à la main.

## Photos (logos, produits, pièces d'identité) : Firebase Storage

Les URLs `logoUrl`, `imageUrl`, `idCardFrontUrl`, `idCardBackUrl` sont aujourd'hui
des chemins de fichiers **locaux au téléphone** (dossier interne de l'app). Pour
qu'elles soient visibles depuis n'importe quel appareil, il faut les héberger sur
**Firebase Storage** (gratuit jusqu'à 5 Go / 1 Go de téléchargement par jour) :

1. Dans la console Firebase → **Build → Storage** → Commencer → mode production.
2. Règles à définir (Storage → Règles) — photos de produits/logos publiques en
   lecture, pièces d'identité strictement privées :

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /products/{allPaths=**} { allow read: if true; allow write: if request.auth != null; }
    match /shops/{allPaths=**}    { allow read: if true; allow write: if request.auth != null; }
    match /id_cards/{allPaths=**} { allow read, write: if false; } // jamais côté client — via Cloud Function uniquement
  }
}
```

3. Ajout de dépendance : `implementation("com.google.firebase:firebase-storage-ktx")`
   (déjà couverte par le firebase-bom ajouté plus haut).
4. Dans `YaarRepository`, après avoir sauvegardé une image en local
   (`ImageStorage.saveToInternalStorage`), l'uploader aussi vers
   `storage.reference.child("products/$productId.jpg").putFile(uri)` et stocker
   l'URL de téléchargement obtenue (`getDownloadUrl()`) à la place du chemin local.

## Notifications push : Firebase Cloud Messaging (FCM)

Pour que "Je suis intéressé" déclenche une vraie notification sur le téléphone du
vendeur (et pas seulement une ligne dans la liste "Notifications" de l'app) :

1. Console Firebase → **Build → Cloud Messaging** (rien à activer, c'est inclus).
2. Ajouter la dépendance `implementation("com.google.firebase:firebase-messaging-ktx")`.
3. Au premier lancement après connexion, récupérer le token FCM du téléphone
   (`FirebaseMessaging.getInstance().token.await()`) et le stocker sur le document
   `users/{uid}` (`fcmToken`).
4. Une Cloud Function déclenchée à la création d'un document `interests/{id}` lit
   `users/{shopOwnerId}.fcmToken` — et n'envoie la notification que si
   `notificationsEnabled == true` — puis appelle `admin.messaging().send(...)`.
