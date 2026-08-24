# Yaar-App — Application Android (Marketplace multi-boutiques)

Application mobile native (Kotlin + Jetpack Compose, Material 3) pour **Yaar-App** :
une plateforme où chaque inscrit peut ouvrir un compte, créer sa propre boutique et
vendre ses produits (chaussures, vêtements, accessoires, meubles, électronique...),
et où tout le monde peut parcourir les produits publiés par les autres boutiques et
commander directement via WhatsApp.

## ✨ Fonctionnalités

- **Onboarding pays/ville** : à la toute première ouverture, l'utilisateur choisit son
  **pays** (Bénin, Burkina Faso, Côte d'Ivoire, Togo — avec drapeau) puis sa **ville**
  dans une liste alphabétique qui se réactualise automatiquement selon le pays choisi.
  Il clique sur **Continuer** pour passer à l'inscription (Prénom, Sexe F/M, numéro
  WhatsApp avec indicatif au format `0022890000000`, mot de passe).
- **Comptes utilisateurs** : inscription / connexion par **numéro WhatsApp** (identifiant
  unique) et mot de passe, session conservée entre les ouvertures de l'application.
- **Ma boutique** : chaque compte peut créer une boutique (pays/ville repris
  automatiquement du profil) et y publier des produits (photo, nom, description, prix
  en FCFA, catégorie). **Limite de 5 produits actifs** pour le forfait gratuit.
  - Tout produit publié gratuitement se **désactive automatiquement au bout de 14
    jours** ; à l'ouverture de sa boutique, le vendeur voit une **notification**
    l'invitant à vérifier ses produits.
  - Sous un produit **désactivé** : boutons **Remettre en vente** / **Supprimer**.
  - Sous un produit **actif** : boutons **Désactiver le produit** / **Supprimer
    définitivement**. Un produit désactivé reste visible par le vendeur (jamais
    supprimé automatiquement) — utile pour garder trace des articles déjà vendus.
  - Au-delà de la limite, un écran **"Publier plus de produits"** propose une
    capacité supplémentaire : **5 → 20 produits actifs pour 5 000 FCFA**, en un seul
    paiement Kkiapay (pas d'abonnement — offre unique pour le moment).
  - Bouton **"Promouvoir mes produits"** : le vendeur choisit un produit actif, puis
    règle deux curseurs — **nombre d'expositions (50 à 1000)** et **durée de la
    campagne (10 à 30 jours)**. Le prix se calcule automatiquement (**20 FCFA par
    exposition**, ex. 100 expositions = 2 000 FCFA), puis paiement Kkiapay. Le produit
    apparaît alors en tête d'"Acheter" avec un badge **"Sponsorisé"** jusqu'à
    épuisement des expositions ou fin de la période — un moteur d'exposition
    décrémente automatiquement le compteur à chaque ouverture de l'application.
  - Bouton **"Ma Publicité"** (dans Mon profil, visible uniquement pendant qu'une
    campagne est active — disparaît automatiquement à la fin) : jours et expositions
    restants pour chaque campagne en cours.
  - Bouton **"Certifié ma boutique"** (dans Mon profil) : envoi des photos recto/verso
    de la pièce d'identité du propriétaire, puis paiement de **10 000 FCFA** pour
    l'étude du dossier (montant appelé à augmenter avec la croissance de la
    communauté — message affiché à l'utilisateur).
  - **Création de boutique enrichie** : logo facultatif (choisi dans la galerie),
    nom, numéro WhatsApp, description libre de l'activité, et jusqu'à **3 catégories**
    parmi une liste de 10 par défaut (Vêtements, Chaussures, Accessoires, Cosmétiques,
    Électroménager, Électronique, Meubles, Jouets, Alimentation, Divers).
  - **Notifications vendeur** (icône cloche, avec pastille du nombre de nouvelles
    notifications) : chaque clic d'un acheteur sur **"Je suis intéressé"** y apparaît.
    Le vendeur peut répondre **Oui disponible** / **Non indisponible**, ou **écrire
    directement au client sur WhatsApp** (son numéro est déjà connu, tout le monde
    s'inscrivant avec son WhatsApp).
  - Réglage **"Recevoir des notifications"** dans Mon profil (par compte).
- **Acheter** : fil affichant les produits **actifs** de toutes les boutiques
  (produits **sponsorisés** en tête, badge "Sponsorisé"), filtrables par catégorie,
  puis **triés en priorité par la ville de l'acheteur**. Sous chaque produit : le
  **prix en grand**, le **nom du produit**, puis en miniature le **nom de la
  boutique** et sa ville de disponibilité (ex. "Chic & Style · Disponible à Lomé").
- **Fiche produit** : bouton **"Voir la boutique"** (vitrine publique du vendeur avec
  logo, description, catégories et tous ses produits en vente) et bouton **"Je suis
  intéressé"** qui notifie le vendeur.
- **Recherche (loupe)** : recherche par mot-clé. Les résultats de la **même ville** que
  l'acheteur s'affichent en premier ; un bouton **"Afficher les produits disponibles
  dans d'autres villes"** en bas de liste ouvre la liste complète des villes du pays,
  sélectionnables (une ou plusieurs) pour élargir la recherche.
- **Panier** : les articles sont regroupés par boutique (puisque chaque boutique a son
  propre numéro WhatsApp) ; la validation envoie un message WhatsApp récapitulatif à
  chaque vendeur concerné.
- **Mon profil** : informations du compte connecté (prénom, sexe, pays/ville, numéro
  WhatsApp), aperçu de sa boutique et de son forfait, déconnexion.
- **Menu du bas** (dans l'ordre demandé) : **Mon profil**, **Ma boutique**, **Acheter**.
- **Design** : thème Material 3 aux couleurs de votre logo (orange `#F7941D` / vert
  `#1E8E3E`), icône de l'app générée à partir de vos fichiers, cartes produits au
  format carré (1:1) avec coins arrondis.
- **Données de démonstration** : vos 6 photos de produits ont été recadrées au format
  1:1 et publiées dans deux boutiques de test pour que le fil "Acheter" ne soit pas
  vide au premier lancement (voir comptes de test ci-dessous).

## 🔑 Comptes de test (données de démonstration)

| Boutique       | Numéro WhatsApp   | Mot de passe | Ville          |
|----------------|--------------------|--------------|----------------|
| Chic & Style   | 0022890000001      | test1234     | Lomé (Togo)    |
| Yaar Électro   | 0022990000002      | test1234     | Cotonou (Bénin)|

Ces deux boutiques possèdent déjà des produits (sac à main, veste, chaussures pour
Chic & Style ; tondeuse, mixeur, mini-frigo pour Yaar Électro) construits à partir des
photos que vous avez fournies. Vous pouvez aussi créer un **nouveau compte** depuis
l'écran d'inscription : vous verrez alors ces produits de démonstration dans l'onglet
"Acheter", exactement comme le ferait un vrai acheteur découvrant la plateforme.

## 🟢 Synchronisation en ligne (Firebase)

**Les boutiques et les produits sont désormais partagés entre tous les téléphones**
via Firebase Firestore + Storage — un produit publié depuis un appareil apparaît dans
"Acheter" sur tous les autres. Les comptes utilisateurs, le panier, les notifications
"intéressé" et les campagnes publicitaires restent pour l'instant locaux à chaque
appareil (prochain lot). Détails complets, état précis collection par collection, et
marche à suivre pour la config Firebase (Firestore, Storage, Authentication) :
**[`BACKEND_FIREBASE.md`](./BACKEND_FIREBASE.md)**.

## ⚠️ Limitation importante à connaître avant la mise en production

Dans cette version, **les comptes utilisateurs et le panier restent stockés
uniquement en local sur l'appareil** (base de données Room) — voir la section
ci-dessus pour ce qui est déjà synchronisé (boutiques, produits) et ce qui ne l'est
pas encore.


## 💳 Paiement Kkiapay — à configurer avant de facturer réellement

Les boutons **"Publier plus de produits"**, **"Promouvoir mes produits"** et
**"Certifié ma boutique"** ouvrent le widget web officiel de
[Kkiapay](https://kkiapay.me) dans une WebView. Montants actuellement en place
(tous intégrés, calcul automatique où applicable) :

| Fonctionnalité | Montant |
|---|---|
| Capacité +15 produits (5 → 20 produits actifs), paiement unique | **5 000 FCFA** — `ShopLimits.kt` |
| Campagne publicitaire (mise en avant produit) | **20 FCFA / exposition** — de 1 000 FCFA (50 expositions) à 20 000 FCFA (1000 expositions), sur 10 à 30 jours au choix du vendeur — `AdPricing` dans `ShopLimits.kt` |
| Étude de dossier de certification boutique | **10 000 FCFA** — `CertificationConfig.kt` (montant amené à augmenter avec la communauté) |

Pour que de vrais paiements fonctionnent :

1. Créez un compte marchand sur https://app.kkiapay.me (ou https://sandbox.kkiapay.me
   pour tester sans argent réel).
2. Copiez votre **clé publique** (jamais la clé privée dans l'app) depuis le tableau de
   bord Kkiapay → "Clés API".
3. Collez-la dans `app/src/main/java/com/yaarapp/app/util/KkiapayConfig.kt`
   (`PUBLIC_API_KEY`), et passez `SANDBOX` à `false` une fois vos tests terminés.
4. Les montants ci-dessus sont des **exemples** — ajustez-les dans `ShopLimits.kt`
   (capacité produits, prix par exposition, bornes min/max) ou `CertificationConfig.kt`
   (certification) selon votre modèle économique.

Tant que la clé n'est pas renseignée, le widget Kkiapay s'ouvre mais le paiement ne
sera pas fonctionnel (mode démonstration).

Pour une vraie plateforme où les boutiques créées par un vendeur sont visibles par tous
les acheteurs sur tous les téléphones, il faut brancher un **backend partagé** — la
piste la plus rapide est **Firebase** (Firebase Authentication pour les comptes,
Firestore pour les boutiques/produits, Firebase Storage pour les photos). La couche
`data/YaarRepository.kt` a été conçue pour isoler cette logique : c'est le seul fichier
à réécrire pour brancher un vrai backend, sans toucher aux écrans.
**Voir le guide détaillé étape par étape : [`BACKEND_FIREBASE.md`](./BACKEND_FIREBASE.md)**
(création du projet Firebase, dépendances Gradle, règles de sécurité, requêtes triées
par ville...).

## 🗂 Structure du projet

```
app/src/main/java/com/yaarapp/app/
├── MainActivity.kt              # Point d'entrée, héberge le NavHost Compose
├── YaarApplication.kt           # Initialise la base de données et amorce les données de démo
├── data/
│   ├── User.kt, Shop.kt, Product.kt, CartItem.kt, Interest.kt, AdCampaign.kt  # Modèles (entités Room)
│   ├── Location.kt              # Enum Country (pays + drapeau + indicatif) + CityRepository
│   ├── Sex.kt, InterestStatus (dans Interest.kt), CertificationStatus.kt
│   ├── ShopCategories.kt        # Catégories de boutique par défaut (max 3 sélectionnables)
│   ├── ShopLimits.kt            # Capacité produits (5→20, 5 000 FCFA) + AdPricing (campagnes)
│   ├── CertificationConfig.kt   # Prix de certification boutique (10 000 FCFA)
│   ├── UserDao.kt, ShopDao.kt, ProductDao.kt, CartDao.kt, InterestDao.kt, AdCampaignDao.kt
│   ├── YaarDatabase.kt          # Base Room (6 tables, version 5)
│   ├── YaarRepository.kt        # Authentification, boutique, marketplace, panier, paiements,
│   │                              # campagnes publicitaires, certification, notifications
│   ├── FirestoreSync.kt         # Synchronisation Firestore/Storage (boutiques + produits)
│   ├── SessionManager.kt        # Session (DataStore) — utilisateur connecté
│   └── SeedData.kt              # Comptes/boutiques/produits de démonstration
├── firebase/
│   └── FirebaseModule.kt        # Accès Firestore/Storage/Auth + connexion anonyme
├── nav/                         # Routes + NavHost (onboarding → auth → onglets principaux)
├── ui/
│   ├── components/               # ProductCard, barre de navigation du bas, filtres
│   ├── screens/                  # Onboarding (pays/ville), Login, SignUp, Marketplace,
│   │                              # Recherche, Détail produit, Vitrine boutique publique,
│   │                              # Panier, Ma boutique, Ajout produit, Profil, Notifications,
│   │                              # Forfaits (capacité produits), Sélection produit à promouvoir,
│   │                              # Configuration de campagne publicitaire, Ma Publicité,
│   │                              # Certification boutique, Paiement Kkiapay, Splash
│   └── theme/                    # Couleurs, typographie, thème Material 3
└── util/
    ├── WhatsAppHelper.kt         # Construction des liens wa.me (boutique + client intéressé)
    ├── PhoneFormat.kt            # Formatage du numéro WhatsApp (00 + indicatif + numéro)
    ├── ImageStorage.kt           # Copie des photos importées + résolution des images
    ├── PasswordHasher.kt         # Hachage simple des mots de passe (démo locale)
    ├── KkiapayConfig.kt          # Clé publique Kkiapay à renseigner (voir section paiement)
    └── KkiapayHtmlBuilder.kt     # Page HTML chargée dans la WebView de paiement
```

## 🛠 Personnaliser

1. **Données de démonstration** — modifiables ou supprimables dans
   `data/SeedData.kt`. Pour repartir d'une base vide, retirez simplement l'appel à
   `repository.seedIfEmpty()` dans `YaarApplication.kt`.
2. **Capacité produits / prix des campagnes / prix certification** — modifiables
   dans `data/ShopLimits.kt` (`ShopLimits`, `AdPricing`) et `data/CertificationConfig.kt`.
3. **Logo / icône** — déjà intégrés dans `res/mipmap-*` et `res/drawable-nodpi`.
4. **Photos produits de démonstration** — dans `res/drawable-nodpi/product_*.jpg`
   (recadrées au format 1:1 à partir des photos que vous avez fournies).

## ▶️ Compiler le projet

### Option A — Android Studio (recommandé)
1. Ouvrez le dossier du projet dans **Android Studio Koala (2024.1)** ou plus récent.
2. Android Studio régénère automatiquement le wrapper Gradle (`gradlew`) à la première
   synchronisation — aucune action supplémentaire n'est nécessaire.
3. Cliquez sur **Run ▶** ou **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

### Option B — Ligne de commande
Le dépôt ne contient pas le binaire `gradle-wrapper.jar` (il ne peut pas être généré
dans cet environnement sans accès réseau). Deux façons de compiler en ligne de commande :

```bash
# 1) Si Gradle est installé sur votre machine :
gradle wrapper --gradle-version 8.7   # régénère gradlew une seule fois
./gradlew assembleDebug

# 2) Sans installation locale, utilisez directement Gradle :
gradle assembleDebug
```

L'APK généré se trouve dans `app/build/outputs/apk/debug/`.

### Option C — GitHub Actions (CI automatique)
Le workflow `.github/workflows/android-build.yml` est prêt à l'emploi : à chaque
`push`, il installe le JDK 17, installe Gradle, régénère le wrapper, compile l'APK de
debug et le publie en tant qu'artefact téléchargeable.

## 📦 Prérequis techniques

- Android Studio Koala+ / JDK 17
- `compileSdk` / `targetSdk` 34, `minSdk` 24 (Android 7.0+)
- Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00), Material 3, Navigation Compose, Room,
  DataStore Preferences, Coil
- Le choix de photo utilise le **sélecteur de photos système** (Photo Picker), qui ne
  nécessite aucune permission de stockage sur Android récent.

## 🚀 Prochaines étapes suggérées

- Étendre la synchronisation Firebase aux comptes, au panier, aux campagnes
  publicitaires (+ Cloud Function de comptage partagé) et aux notifications
  "intéressé" (+ notification push) — guide complet dans `BACKEND_FIREBASE.md`.
- Passer de l'authentification anonyme à la connexion par numéro de téléphone
  (Firebase Auth) pour des règles de sécurité plus strictes par propriétaire.
- Renseigner la clé publique Kkiapay (`util/KkiapayConfig.kt`) pour activer les
  paiements réels (capacité produits, campagnes publicitaires, certification).
- Ajouter une interface d'administration pour étudier les dossiers de certification
  (aujourd'hui : vérification manuelle dans Firestore, voir `BACKEND_FIREBASE.md`).
- Publier l'application sur le Google Play Store (nécessite un compte développeur
  Google Play).

---
Basé sur le projet **Yaar-App** — identité, logique métier et photos de produits
reprises de vos échanges et des fichiers fournis.
