# Yaar-App 1.1.5

## Photos Firebase Storage
- Les nouvelles photos de produits sont désormais envoyées vers `products/{uid}/...` avant que la publication soit confirmée.
- Une erreur d'upload bloque la publication au lieu d'enregistrer un chemin local inaccessible aux autres téléphones.
- Lors d'une connexion/restauration, les anciennes photos locales encore présentes sur le téléphone du propriétaire sont automatiquement réenvoyées vers Firebase Storage.
- Les références `gs://` sont converties en URL de téléchargement avant d'être affichées.
- Une photo distante invalide affiche un état explicite au lieu d'un chargement infini.

## Suppression définitive du compte
- Nouveau bouton `Supprimer mon compte` dans Mon profil.
- Ré-authentification obligatoire avec le mot de passe.
- Confirmation : `Votre compte sera supprimé avec toutes vos données.`
- Suppression des documents users, shops, products et conversations/messages associés.
- Suppression des fichiers Storage référencés par les produits et boutiques.
- Nettoyage des données Room locales puis suppression du compte Firebase Authentication.

## Discussion fournisseur
- Nouveau bouton `Discuter avec le fournisseur ici` depuis la fiche produit.
- Discussion temps réel Firestore dans `conversations/{conversationId}/messages`.
- Le contexte du produit est affiché en haut de la discussion.
- Nouveau bouton WhatsApp avec photo du produit et message prérempli. L'API publique Android de WhatsApp ne permet pas de cibler simultanément un numéro précis et d'attacher un média ; le partage média est donc envoyé directement à WhatsApp avec le message prérempli, puis le vendeur est sélectionné.

## Firebase Rules
- Suppression autorisée uniquement au propriétaire pour users/shops/products.
- Conversations accessibles uniquement aux participants.
- Messages accessibles uniquement aux participants de la conversation.

## Version
- versionCode: 7
- versionName: 1.1.5
