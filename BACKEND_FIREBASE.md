# Base de données en ligne gratuite — Firebase, et son état d'avancement

## 🟢 État actuel : synchronisation active

Le projet Firebase `yaarapp-6c4ae` est créé, `google-services.json` est installé, et
**la synchronisation Firestore + Storage est câblée dans le code** :

- Chaque **boutique créée** et chaque **produit publié/désactivé/remis en vente/promu**
  est automatiquement poussé vers Firestore en tâche de fond (write-through), photos
  comprises (upload vers Firebase Storage). Voir `data/FirestoreSync.kt`.
- L'app **écoute Firestore en temps réel** dès son lancement (`YaarApplication` →
  `repository.startRemoteSync()`) et reflète dans la base locale (Room) les
  boutiques/produits publiés par **les autres téléphones** — donc désormais visibles
  dans "Acheter" sans rien faire de plus.
- Chaque installation se connecte automatiquement en **Firebase Authentication anonyme**
  (aucun écran de connexion supplémentaire), suffisant pour satisfaire les règles de
  sécurité Firestore/Storage déployées.
- Toute cette synchronisation est **best-effort et non bloquante** : sans réseau, l'app
  continue de fonctionner intégralement en local (comme avant), et se resynchronise
  automatiquement dès que la connexion revient.

**Reste à faire vous-même**, dans la console Firebase (voir étapes détaillées plus
bas) : activer Firestore Database, Storage et Authentication (anonyme) si ce n'est pas
déjà fait, puis déployer `firestore.rules` / `storage.rules` / `firestore.indexes.json`
avec la Firebase CLI (`firebase deploy --only firestore:rules,firestore:indexes,storage`).

## ✅ Comment vérifier que ça fonctionne vraiment

Deux pièges fréquents à ce stade :

1. **"Analytics" (page d'accueil du projet) ≠ Firestore.** Le widget "Données
   analytiques" / "Utilisateurs actifs par jour" que vous voyez sur la page d'accueil
   Firebase appartient à **Google Analytics**, un produit Firebase totalement séparé
   qui n'est PAS intégré dans ce projet (pas de SDK Analytics ajouté). C'est normal
   qu'il affiche "Aucune donnée" — ça ne dit rien sur l'état de Firestore. Pour voir si
   vos boutiques/produits sont bien synchronisés, allez plutôt dans
   **Build → Firestore Database → onglet "Data"** : vous devriez y voir des
   collections `shops` et `products` apparaître après avoir créé une boutique/publié
   un produit dans l'app.
2. **"Anonyme" doit être activé dans Authentication**, pas "Téléphone" (une
   instruction plus bas dans ce document mentionnait par erreur "Téléphone" — c'est
   corrigé). Le code utilise `signInAnonymously()` ; si le fournisseur "Anonyme" n'est
   pas activé (**Build → Authentication → Sign-in method → Anonyme**), chaque tentative
   de connexion échoue silencieusement (rattrapée par un `try/catch`), et donc **rien
   n'est jamais envoyé à Firestore**, sans aucune erreur visible dans l'app elle-même.
   C'est la cause la plus probable si vous ne voyez toujours rien après avoir vérifié
   le point 1.

Pour diagnostiquer précisément : ouvrez **Logcat** dans Android Studio (ou
`adb logcat | grep YaarFirestoreSync` en ligne de commande) pendant que vous utilisez
l'app. Chaque tentative de synchronisation est maintenant journalisée sous le tag
`YaarFirestoreSync` :
- `"Connexion Firebase anonyme OK"` → tout va bien, la connexion Firebase fonctionne.
- `"Connexion Firebase impossible..."` → très probablement "Anonyme" pas encore activé
  (point 2 ci-dessus), ou pas de réseau au moment du test.
- `"Boutique/Produit ... synchronisé(e)"` → l'écriture vers Firestore a réussi ; le
  document devrait être visible dans Firestore Database → Data quelques secondes après.
- `"Échec de synchro pour..."` → le message d'erreur qui suit indique la cause exacte
  (souvent : règles de sécurité Firestore non déployées, ou base Firestore pas encore
  créée dans la console).

**Pas encore branché** (périmètre volontairement limité pour cette première mise en
ligne, afin de limiter les risques) : synchronisation des comptes utilisateurs, du
panier, des notifications "intéressé", des campagnes publicitaires et des photos de
pièce d'identité (certification) — ces éléments restent 100 % locaux pour l'instant.
Le moteur d'exposition des campagnes publicitaires reste également local à chaque
téléphone (la version Cloud Function partagée, déjà écrite dans `functions/index.js`,
n'est pas encore appelée par l'app — nécessite le plan Blaze, voir plus bas).

## Pourquoi Firebase (Firestore) et pas autre chose

Avant cette mise en ligne, Yaar-App stockait tout **uniquement sur le téléphone** (base
Room/SQLite locale) — chaque vendeur et chaque acheteur voyait une base différente.
Désormais, boutiques et produits sont partagés via **Firebase Firestore** (Google),
pour ces raisons :

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

## Comment ça marche côté code (déjà en place)

- Toute la logique métier passe par `YaarRepository`, qui délègue la synchronisation
  à `data/FirestoreSync.kt` — les écrans (`MarketplaceScreen`, `MyShopScreen`, etc.)
  n'ont pas eu besoin de changer.
- Chaque boutique/produit garde son identifiant local Room (généré indépendamment sur
  chaque téléphone) ET reçoit un `remoteId` Firestore (généré par Firestore,
  globalement unique) une fois synchronisé. C'est ce second identifiant qui permet de
  faire correspondre les mises à jour entre téléphones sans jamais faire entrer en
  collision les identifiants locaux de deux appareils différents.
- Pays/ville sont des champs structurés (`Country`, `city: String`), ce qui permettra
  des requêtes filtrées côté serveur (`whereEqualTo("city", ...)`) quand la recherche
  sera, elle aussi, branchée sur Firestore (actuellement, la recherche continue de
  fonctionner sur les données locales déjà synchronisées, ce qui couvre l'usage normal).

## ✅ Déjà fait pour vous dans ce dépôt

Ce projet contient maintenant tous les fichiers de configuration Firebase, prêts à
déployer dès que votre projet Firebase existe :

```
firebase.json              # Config Firebase CLI (pointe vers les fichiers ci-dessous)
.firebaserc                 # Identifiant de votre projet Firebase (à renseigner, voir plus bas)
firestore.rules             # Règles de sécurité Firestore (qui peut lire/écrire quoi)
firestore.indexes.json      # Index composites nécessaires aux requêtes triées par ville/promo
storage.rules                # Règles de sécurité Storage (photos publiques, pièces d'identité privées)
functions/index.js          # Cloud Functions : moteur d'exposition partagé + notifications push
functions/package.json      # Dépendances des Cloud Functions
app/build.gradle.kts         # Plugin Google Services + dépendances Firebase (déjà ajoutés,
                              # activés automatiquement dès que google-services.json existe)
```

Il ne reste que la partie qui nécessite votre propre compte Google : créer le projet
Firebase, puis déployer ces fichiers avec la Firebase CLI.

> **Note versions (24/08/2026)** : les versions du plugin `google-services` (4.5.0)
> et du BoM Firebase (34.18.0) utilisées dans ce dépôt sont celles indiquées par la
> console Firebase au moment de la configuration de `yaarapp-6c4ae`. Depuis le BoM
> 34.0.0 (juillet 2025), Firebase a retiré les modules `-ktx` séparés (ex.
> `firebase-firestore-ktx`) : les API Kotlin sont désormais directement dans les
> modules principaux (`firebase-firestore`, `firebase-auth`, etc.), déjà utilisés
> comme tels dans `app/build.gradle.kts`. Si vous mettez le BoM à jour plus tard,
> vérifiez sur https://firebase.google.com/docs/android/setup que les noms
> d'artefacts n'ont pas encore changé.

> **Note Kotlin 2.3.21 (25/08/2026)** : `firebase-auth` 24.2.0 (tiré par le BoM
> 34.18.0) est compilé avec un format de métadonnées Kotlin que seul un compilateur
> Kotlin 2.3+ peut lire — le projet est donc passé de Kotlin 1.9.24 à **2.3.21**
> (`build.gradle.kts` racine), avec KSP **2.3.11** assorti. Cette bascule impose
> deux changements Gradle obligatoires, déjà faits dans ce dépôt :
> - Le compilateur Compose se configure désormais via le plugin
>   `org.jetbrains.kotlin.plugin.compose` (même version que Kotlin) au lieu de
>   `composeOptions { kotlinCompilerExtensionVersion = ... }`, supprimé.
> - `android.kotlinOptions { jvmTarget = ... }` est supprimé depuis Kotlin 2.2 ;
>   remplacé par le bloc `kotlin { compilerOptions { jvmTarget = ... } }` en bas de
>   `app/build.gradle.kts`.
>
> Le Compose BOM (2024.06.00) et `compileSdk`/`targetSdk` (34) n'ont volontairement
> **pas** été mis à jour pour limiter les risques — Kotlin 2.3.21 reste compatible
> avec ces versions plus anciennes.

> **Note AGP 8.13.2 (25/08/2026)** : KSP 2.3.11 appelle une méthode
> (`AndroidComponentsExtension.addKspConfigurations`) qui n'existe pas dans AGP 8.5.2
> (`NoSuchMethodError` à la compilation). AGP est donc passé de **8.5.2 à 8.13.2**
> (`build.gradle.kts` racine) — c'est aussi la version où Google a officiellement
> ajouté le support de Kotlin 2.3 (R8 8.13.19). **AGP 8.13 exige Gradle 8.13 minimum**
> (mis à jour dans `gradle-wrapper.properties` et le workflow CI :
> `gradle-version: '8.13'`). `compileSdk`/`targetSdk` (34) restent inchangés — AGP
> 8.13 supporte jusqu'à l'API 36, donc aucune contrainte de ce côté.

> **Note Room 2.8.4 (25/08/2026)** : le compilateur Room (via KSP2, avec Kotlin 2.3.21)
> plantait sur les fonctions `suspend` des DAO (`IllegalStateException: unexpected jvm
> signature V`) — bug connu de KSP2, corrigé dans Room à partir de la 2.7.0. Room est
> donc passé de **2.6.1 à 2.8.4** (`app/build.gradle.kts`), toujours sous le paquet
> `androidx.room` (Room 3.0, sorti récemment sous `androidx.room3`, est une réécriture
> majeure à part qui aurait exigé de renommer tous les imports du projet — pas
> nécessaire ici, volontairement évité).

> **Compilation Firebase obligatoire** : la version distribuée de Yaar-App refuse désormais
> de compiler si `app/google-services.json` est absent. Cela évite de générer par erreur
> un APK qui fonctionnerait uniquement en local. Le workflow GitHub peut restaurer ce
> fichier depuis le secret `GOOGLE_SERVICES_JSON` si vous ne souhaitez pas le versionner.
> L'application initialise ensuite explicitement `FirebaseApp` au démarrage avant de
> créer le repository, ce qui corrige l'erreur `Default FirebaseApp is not initialized`.


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
4. **Build → Firestore Database** → Créer une base de données → région proche
   (ex. `europe-west1`) → mode production (les règles sont déjà prêtes, voir
   `firestore.rules` — pas besoin de les retaper à la main, la CLI les déploie).
5. **Build → Storage** → Commencer → mode production (règles déjà prêtes dans
   `storage.rules`).
6. **Build → Authentication** → Commencer → onglet "Sign-in method" → activez
   **Anonyme** ("Anonymous"). ⚠️ C'est bien "Anonyme" qu'il faut activer, pas
   "Téléphone" — c'est ce que le code utilise actuellement
   (`FirebaseModule.ensureSignedIn()`, connexion automatique sans écran de connexion).
   "Téléphone" n'est mentionné ailleurs dans ce document que comme piste
   d'amélioration future, pas encore implémentée.
7. Pour les **Cloud Functions** (`functions/index.js`, moteur d'exposition partagé +
   notifications push) : elles nécessitent de passer le projet au plan **Blaze**
   (pay-as-you-go). Ce n'est pas gratuit "Spark", MAIS le plan Blaze reste gratuit
   tant que vous restez sous les quotas gratuits inclus (2 millions
   d'appels/mois pour les fonctions HTTPS, largement suffisant au démarrage) — une
   carte bancaire est demandée par précaution, mais rien n'est prélevé sauf
   dépassement. Si vous préférez rester 100 % Spark pour l'instant, sautez cette
   étape : tout le reste (Firestore, Storage, Auth) fonctionne sans Cloud Functions,
   simplement le moteur d'exposition restera local à chaque téléphone comme
   aujourd'hui (voir section dédiée plus bas).

### Déployer les règles et fonctions avec la Firebase CLI

Sur votre ordinateur (pas dans cet environnement, qui n'a pas accès à Internet) :

```bash
# 1) Installer la CLI (une seule fois)
npm install -g firebase-tools

# 2) Se connecter avec le même compte Google que la console Firebase
firebase login

# 3) Depuis la racine du projet Yaar-App (là où se trouve firebase.json)
#    Remplacez l'ID dans .firebaserc par l'identifiant exact de votre projet
#    (visible dans Paramètres du projet ⚙️ → Général → "ID du projet")

# 4) Déployer les règles Firestore + Storage + les index
firebase deploy --only firestore:rules,firestore:indexes,storage

# 5) (Si plan Blaze activé) déployer les Cloud Functions
cd functions && npm install && cd ..
firebase deploy --only functions
```

## Ce qui est branché côté code Android (fait)

`google-services.json` est installé dans `app/`, le plugin Google Services et toutes
les dépendances Firebase sont actives. La synchronisation Firestore/Storage fonctionne
pour les boutiques et les produits (voir "État actuel" en haut de ce document).

Fichiers concernés si vous voulez lire ou ajuster le code :
- `firebase/FirebaseModule.kt` — accès Firestore/Storage/Auth, connexion anonyme.
- `data/FirestoreSync.kt` — toute la logique de synchronisation (write-through +
  écoute temps réel). C'est le seul fichier qui parle directement à Firebase.
- `data/YaarRepository.kt` — appelle `FirestoreSync` après chaque écriture locale
  pertinente (création boutique, ajout/désactivation/suppression produit, etc.).
- `YaarApplication.kt` — démarre l'écoute temps réel au lancement de l'app.

Exemple de requête Firestore avec **tri**, pour référence si vous étendez la
synchronisation (ex. recherche par ville directement côté serveur) :

```kotlin
firestore.collection("products")
    .whereEqualTo("isActive", true)
    .whereEqualTo("city", "Lomé")
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .get()
```

## Authentification

**Fait (V1)** : chaque installation se connecte automatiquement en **anonyme**
(`FirebaseModule.ensureSignedIn()`), sans écran de connexion Firebase supplémentaire.
Cela suffit à satisfaire les règles de sécurité actuelles (`request.auth != null`),
mais ne relie pas un compte local précis à une identité Firebase précise — voir
l'avertissement en tête de `firestore.rules`.

**Amélioration recommandée pour la suite** : remplacer l'authentification anonyme par
**Firebase Auth par numéro de téléphone (OTP SMS)**, en réutilisant directement le
numéro WhatsApp déjà collecté (format `00<indicatif><numéro>`, à convertir en
`+<indicatif><numéro>` pour Firebase qui utilise le format E.164). Cela permettra de
resserrer les règles Firestore/Storage à "seul le propriétaire peut modifier SA
boutique" — actuellement, n'importe quel compte connecté à l'app peut modifier
n'importe quelle boutique côté Firestore (mais pas depuis l'app elle-même, qui ne
propose pas cette action dans son interface).

## Mise à jour du schéma (lot "capacité produits payante" + campagnes publicitaires + certification)

Schéma Firestore à jour. **✅ Synchronisées** (voir "État actuel" en haut) : `shops`
et `products`. **⏳ Pas encore synchronisées** (structure prête, mais ces collections
ne sont pas encore écrites/lues par l'app — elles restent 100 % locales pour
l'instant) : `users`, `ad_campaigns`, `interests`.

```
users/{uid}          → firstName, sex, country, city, whatsappNumber,
                        notificationsEnabled (Boolean), createdAt          [PAS SYNCHRONISÉ]

shops/{shopId}        → ownerId, name, whatsappNumber, country, city,
                        logoUrl (String?), activityDescription, categories (List<String>, max 3),
                        extraProductSlots (Int, 0 ou 15 — capacité 5→20 produits),
                        certificationStatus ("NONE"|"PENDING"|"CERTIFIED"),
                        idCardFrontUrl (String?), idCardBackUrl (String?),
                        certificationRequestedAt (Long?), createdAt, remoteId    [SYNCHRONISÉ]

products/{productId}  → shopId, shopName, name, description, price, imageUrl, category,
                        country, city, isActive, isPromoted (Boolean),
                        createdAt, activatedAt, remoteId                    [SYNCHRONISÉ]

ad_campaigns/{id}     → productId, productName, shopId, totalExpositions,
                        remainingExpositions, durationDays, startedAt, endsAt,
                        priceFcfa, isActive (Boolean)                       [PAS SYNCHRONISÉ]

interests/{id}         → productId, productName, productImageUrl, shopId, shopOwnerId,
                        buyerId, buyerFirstName, buyerWhatsappNumber,
                        status ("PENDING"|"AVAILABLE"|"UNAVAILABLE"), isRead, createdAt   [PAS SYNCHRONISÉ]
```

`remoteId` (String, ajouté sur `Shop` et `Product` côté Room) est l'identifiant du
document Firestore correspondant — voir "Comment ça marche côté code" en haut de ce
document pour le principe complet (il évite toute collision entre les identifiants
locaux générés indépendamment sur chaque téléphone).

### Le moteur d'exposition publicitaire doit devenir une Cloud Function

Actuellement (`YaarRepository.recordAppOpenExposure`), chaque campagne perd une
exposition à chaque ouverture de l'app **sur ce même téléphone** — ça ne marche que
pour tester en solo (les campagnes publicitaires ne sont pas encore synchronisées, voir
plus haut). Le code de la vraie version partagée est **déjà écrit** dans
`functions/index.js` (fonction `recordAppOpenExposure`, déployable directement avec
`firebase deploy --only functions` une fois le plan Blaze activé — voir plus haut).
Elle s'exécute pour **tous les utilisateurs de tous les téléphones** — c'est ce qui
rend le comptage des expositions réellement partagé, comme demandé ("apparaît sur
100 profils dès l'ouverture de l'application"). Il restera, côté Android, à
synchroniser `ad_campaigns` vers Firestore (même principe que `products`) puis à
remplacer l'appel à `repository.recordAppOpenExposure()` (local) par un appel à
cette Cloud Function via le SDK Firebase Functions — prochaine étape logique.

De la même façon, `functions/index.js` contient déjà `notifyShopOwnerOfInterest`,
la Cloud Function qui enverra une vraie notification push au vendeur dès qu'un
document sera créé dans `interests` — cette collection n'étant pas encore
synchronisée, la fonction ne se déclenche pas encore en pratique (voir section Cloud
Messaging plus bas).

### Vérification manuelle de la certification

`certificationStatus` passe à `"PENDING"` automatiquement après le paiement Kkiapay
(côté app), et cette information EST déjà synchronisée (fait partie de `shops`). Il
n'existe pas encore d'interface pour l'étudier — le plus simple pour démarrer est de
consulter directement la collection `shops` dans la **console Firebase → Firestore
Database**, d'ouvrir le document, et de changer manuellement `certificationStatus` à
`"CERTIFIED"` une fois la pièce vérifiée. Les photos recto/verso
(`idCardFrontUrl` / `idCardBackUrl`) ne sont pour l'instant PAS uploadées vers Storage
— elles restent des chemins de fichiers locaux au téléphone du vendeur (à faire dans
un prochain lot, avec des règles Storage restreintes déjà prêtes dans `storage.rules`).
Une mini-interface d'administration (page web réservée à vous) pourra être ajoutée
plus tard pour éviter de le faire à la main.

## Photos (logos, produits, pièces d'identité) : Firebase Storage

**✅ Fait pour `logoUrl` (boutique) et `imageUrl` (produit)** : `FirestoreSync.kt`
détecte automatiquement les chemins de fichiers locaux (par opposition aux images
intégrées `"res:..."` ou aux URL déjà distantes), les envoie vers **Firebase Storage**
(gratuit jusqu'à 5 Go / 1 Go de téléchargement par jour), et remplace le chemin local
par l'URL de téléchargement avant l'écriture dans Firestore — donc visibles depuis
n'importe quel appareil.

**⏳ Pas encore fait pour `idCardFrontUrl` / `idCardBackUrl`** (pièces d'identité,
certification) : ces deux champs restent des chemins locaux au téléphone du vendeur
pour l'instant (voir "Vérification manuelle de la certification" plus haut) — les
règles Storage sont déjà prêtes (`storage.rules`, dossier `id_cards/{uid}/...`,
jamais relisible depuis l'app cliente) pour quand cet upload sera ajouté.

1. Dans la console Firebase → **Build → Storage** → Commencer → mode production
   (si ce n'est pas déjà fait).
2. Règles déjà écrites et prêtes à déployer : `storage.rules` à la racine du projet.
   Déploiement : `firebase deploy --only storage` (voir plus haut).
3. La dépendance `firebase-storage` est **déjà ajoutée** dans
   `app/build.gradle.kts` et active.

## Notifications push : Firebase Cloud Messaging (FCM)

Pour que "Je suis intéressé" déclenche une vraie notification sur le téléphone du
vendeur (et pas seulement une ligne dans la liste "Notifications" de l'app) :

1. Console Firebase → **Build → Cloud Messaging** (rien à activer, c'est inclus).
2. La dépendance `firebase-messaging` est **déjà ajoutée** dans
   `app/build.gradle.kts` (activée automatiquement avec `google-services.json`).
3. Au premier lancement après connexion, il faudra récupérer le token FCM du
   téléphone (`FirebaseMessaging.getInstance().token.await()`) et le stocker sur le
   document `users/{uid}` (`fcmToken`) — ce sera fait dans `YaarRepository` lors du
   branchement Firestore.
4. Côté serveur, tout est **déjà écrit** : `functions/index.js` contient
   `notifyShopOwnerOfInterest`, qui se déclenche automatiquement à la création d'un
   document `interests/{id}`, lit `users/{shopOwnerId}.fcmToken`, n'envoie la
   notification que si `notificationsEnabled == true`, puis appelle
   `admin.messaging().send(...)`. Rien à écrire — juste à déployer (voir
   `firebase deploy --only functions` plus haut, nécessite le plan Blaze).

## ❓ Faut-il retoucher Firebase à chaque mise à jour de l'app ?

**La plupart du temps, non.** Une mise à jour qui ne touche pas aux fonctionnalités
listées ci-dessous (nouvel écran, correction de bug, changement visuel, nouvelle
logique 100% locale...) n'a besoin d'aucune action côté Firebase — vous recompilez et
republiez l'app, c'est tout.

**Une action côté Firebase est nécessaire uniquement si vous :**
- **Ajoutez un nouveau champ lu/écrit dans Firestore** sur `shops` ou `products` → rien
  à faire dans la console (Firestore n'a pas de schéma rigide), mais pensez à mettre à
  jour `firestore.rules` si ce champ doit être protégé différemment des autres.
- **Synchronisez une nouvelle collection** (ex. étendre la synchro à `interests` ou
  `ad_campaigns`, comme évoqué plus haut) → ajouter les règles correspondantes dans
  `firestore.rules`, et les redéployer : `firebase deploy --only firestore:rules`.
- **Ajoutez une nouvelle requête filtrée/triée** (ex. `whereEqualTo` + `orderBy`
  combinés sur des champs pas encore indexés ensemble) → Firestore vous demandera de
  créer un index composite (message d'erreur avec un lien direct pour le créer en un
  clic, ou ajout manuel dans `firestore.indexes.json` + redéploiement).
- **Modifiez `functions/index.js`** → redéployer avec
  `firebase deploy --only functions`.
- **Changez le nom de package de l'app** (`applicationId`, actuellement
  `com.yaarapp.app`) → il faudrait ré-enregistrer l'app dans Firebase et récupérer un
  nouveau `google-services.json`. À éviter sauf raison majeure.

En résumé : le code Android et la configuration Firebase (règles, index, fonctions)
évoluent **indépendamment** la plupart du temps ; ils ne doivent être synchronisés
que lorsqu'une mise à jour touche concrètement à la structure des données partagées
ou aux règles de sécurité.


## Correction importante — compilation GitHub Actions

La version actuelle de Yaar-App initialise Firebase explicitement dans `YaarApplication`.
Le build refuse maintenant de produire un APK si `app/google-services.json` est absent,
afin d'éviter de distribuer par erreur une version locale sans connexion Firebase.

Deux méthodes sont possibles :

1. **Méthode simple pour ce dépôt** : conserver `app/google-services.json` dans GitHub.
   Le fichier fourni dans ce projet correspond à `com.yaarapp.app` et au projet Firebase
   `yaarapp-6c4ae`.
2. **Méthode recommandée pour un dépôt public** : créer dans GitHub
   `Settings → Secrets and variables → Actions` un secret nommé
   `GOOGLE_SERVICES_JSON`, contenant le contenu complet du fichier. Le workflow le
   restaurera automatiquement avant la compilation.

Le workflow vérifie également que le JSON contient bien la configuration Android pour
`com.yaarapp.app`. Il génère maintenant un APK debug et un APK release comme artefacts.
