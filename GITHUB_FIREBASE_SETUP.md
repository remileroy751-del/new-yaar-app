# Yaar-App — compilation GitHub + Firebase

Cette version corrige l’erreur :

`Default FirebaseApp is not initialized in this process`

## 1. Le fichier Firebase

Le projet contient déjà :

`app/google-services.json`

Il correspond à l’application Android :

`com.yaarapp.app`

et au projet Firebase :

`yaarapp-6c4ae`

Le fichier n’est plus exclu par `.gitignore`, ce qui permet de pousser directement
le projet complet sur GitHub et de compiler sans configuration supplémentaire.

## 2. Option recommandée si le dépôt est public

Vous pouvez ne pas versionner le fichier et utiliser un secret GitHub :

1. GitHub → **Settings**
2. **Secrets and variables** → **Actions**
3. **New repository secret**
4. Nom : `GOOGLE_SERVICES_JSON`
5. Valeur : contenu complet de `app/google-services.json`

Le workflow utilise le secret s’il existe.

## 3. Ce que fait maintenant l’application

Au démarrage :

`YaarApplication` → `FirebaseModule.initialize()` → `YaarRepository` → synchronisation Firestore

Firebase est donc initialisé explicitement avant tout appel à Auth, Firestore ou Storage.

Lorsqu’un vendeur publie un produit :

1. Le produit est enregistré dans Room pour que l’application reste utilisable hors ligne.
2. Firebase Auth effectue une connexion anonyme.
3. La photo locale est envoyée dans Firebase Storage.
4. Le produit est envoyé dans `products/{documentId}` sur Firestore.
5. L’URL Storage remplace le chemin local de l’image.
6. L’identifiant Firestore est enregistré dans Room comme `remoteId`.

## 4. Si la compilation GitHub échoue

Le workflow arrête volontairement la compilation si `app/google-services.json` est absent.
C’est préférable à produire un APK qui fonctionne uniquement en local.

Si le fichier est présent mais que l’application affiche une erreur Firebase après
installation, vérifiez dans Firebase Console que :

- **Authentication → Sign-in method → Anonymous** est activé ;
- **Firestore Database** est créé ;
- les règles de `firestore.rules` sont déployées ;
- **Storage** est créé ;
- les règles de `storage.rules` sont déployées.

## Mise à jour comptes utilisateurs — version 2

Cette version utilise Firebase Authentication Email/Password pour les comptes Yaar-App.
Le mot de passe n'est jamais enregistré dans Firestore ni dans Room. Firebase conserve le
mot de passe de manière sécurisée. Yaar-App utilise une adresse technique dérivée du numéro
WhatsApp afin de permettre une connexion avec « numéro WhatsApp + mot de passe » sans demander
une adresse e-mail à l'utilisateur.

### Firebase Console

Dans **Authentication → Sign-in method**, activez **Email/Password**.
L'authentification **Anonymous** doit rester activée pendant la période de migration des
anciens comptes.

### Migration des anciens comptes

Les comptes créés avec l'ancienne version sont conservés dans Room grâce à une migration
Room 6 → 7. Lors de la première ouverture après la mise à jour, un ancien compte est invité
à définir son mot de passe. Le compte anonyme Firebase existant est alors lié au nouveau
compte permanent sans changer son UID, puis la boutique et les produits existants sont
rattachés à cet UID.

**Important :** un ancien utilisateur doit effectuer cette sécurisation sur son ancien
Téléphone avant de désinstaller l'ancienne application ou de perdre les données locales.
Une fois le mot de passe défini et la migration terminée, il peut installer Yaar-App sur
un autre téléphone et se connecter avec son numéro WhatsApp et son mot de passe.

### Structure Firestore ajoutée

- `users/{firebaseUid}` : profil utilisateur (jamais le mot de passe)
- `shops/{shopId}` : `ownerUid` identifie le propriétaire Firebase
- `products/{productId}` : `ownerUid`, `shopRemoteId`, `availableCities`
- `availableCities` contient automatiquement la ville principale + au maximum 5 villes supplémentaires.

Les règles `firestore.rules` ont été renforcées pour que les modifications de boutiques et
produits soient liées au `request.auth.uid` du propriétaire.

Après modification des règles, déployer :
`firebase deploy --only firestore:rules`
