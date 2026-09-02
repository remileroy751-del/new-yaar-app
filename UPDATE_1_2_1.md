# Yaar-App V1.2.1

## Profil
- Hiérarchie visuelle renforcée des trois actions du profil.
- **Certifier ma boutique** : bouton principal orange, plein, avec bordure/élévation.
- **Se déconnecter** : bouton secondaire vert, bordure renforcée.
- **Supprimer mon compte** : action destructive rouge, bordure renforcée.
- Le profil reste défilable sur les petits écrans.

## Photos produits Firebase
- Les photos locales sont maintenant envoyées vers Firebase Storage avec un chemin stable : `products/{uid}/{productFirestoreId}.jpg|png`.
- Le document Firestore conserve à la fois `imageUrl` et `imageStoragePath`.
- Lorsqu'un autre utilisateur reçoit le produit, `imageStoragePath` est prioritaire pour reconstruire une URL Firebase Storage valide.
- Les nouvelles tentatives d'envoi réutilisent le même fichier Storage, évitant les fichiers orphelins.
- Les anciens produits dont la photo est encore locale sur le téléphone du vendeur peuvent être réparés automatiquement au démarrage.

## Suppression de compte
- Suppression des produits et boutiques Firestore du compte.
- Suppression des fichiers Storage référencés par les produits/boutiques.
- Nettoyage de `interests` et `ad_campaigns` lorsqu'ils contiennent un UID associé.
- Suppression des conversations et sous-collections de messages.
- Nettoyage de sécurité des dossiers Storage `products/{uid}` et `shops/{uid}`.
- Suppression du document `users/{uid}`, puis du compte Firebase Authentication.
- Les pièces d'identité de certification restent protégées par les règles Storage et nécessitent une suppression Admin/Cloud Function si elles existent encore.

## Version
- `versionCode`: 13
- `versionName`: `1.2.1`
