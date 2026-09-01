# Yaar-App 1.1.2

## Corrections
- Correction définitive de la restauration des boutiques et produits depuis Firestore lors d'une nouvelle connexion / changement de téléphone.
- Suppression de la désérialisation Firestore directe des entités Room `Shop` et `Product`.
- Mapping Firestore explicite, compatible avec les documents déjà présents dans la base.
- Écriture Firestore normalisée : les identifiants Room locaux (`id`, `ownerId`) ne sont plus envoyés comme données métier.
- Les produits existants restent dans Firebase : aucune suppression de la base n'est nécessaire.
- Les produits publics de tous les vendeurs restent lisibles par les utilisateurs du même pays, avec priorité à la ville de l'utilisateur dans l'application et la recherche.
- Ajout des catégories `Informatique` et `Matériels de bureau`.

## Version
- versionCode: 4
- versionName: 1.1.2
