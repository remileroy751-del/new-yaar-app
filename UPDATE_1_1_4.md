# Yaar-App 1.1.4

## Corrections
- Réparation automatique des anciennes photos de produits lorsque Firestore contient encore un chemin local du téléphone du vendeur.
- Les nouvelles photos sont systématiquement envoyées vers Firebase Storage et leur URL de téléchargement est enregistrée dans Firestore.
- Les références `file://` sont également reconnues par l'affichage local.
- Ajout des pays Mali, Niger et Sénégal avec indicatifs 223, 227 et 221 et drapeaux intégrés.
- Ajout des listes de villes pour ces trois pays ; elles sont triées alphabétiquement à l'affichage.
- Version Android : 1.1.4 (versionCode 6).

## Données Firebase
Ne supprimez pas les collections Firestore existantes. Les documents existants sont conservés.

Pour les anciennes photos dont le vendeur ne possède plus le fichier local sur son téléphone, l'image ne peut pas être reconstruite à partir d'un simple chemin local. Le propriétaire doit alors republier/remplacer la photo.
