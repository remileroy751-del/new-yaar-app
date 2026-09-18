# Yaar-App V1.5.1

## Immobilier
- Limite gratuite de 5 annonces immobilières actives par boutique.
- Extension préparée de 5 à 20 annonces immobilières pour 5 000 FCFA.
- Contrôle de la limite côté Android et côté Supabase.
- Les annonces immobilières restent visibles dans tout le pays de publication, avec priorité visuelle à la ville de l’utilisateur.
- Maximum de 2 photos par annonce conservé.

## Paiements
- Aucun paiement réel n’est lancé pour le moment.
- L’écran de paiement affiche : « Les paiements seront activés le 01/11/2026 ».
- L’intégration PayDunya reste dans le projet pour une activation ultérieure.

## Super Admin
- `index.html` à la racine du dépôt, prêt pour GitHub Pages.
- Connexion Supabase sécurisée sans clé `service_role`.
- Statistiques utilisateurs et répartition par pays.
- Liste et suppression des produits/annonces non conformes.
- Vérification/révocation du badge des boutiques.
- Suppression de comptes par administrateur.
- Recherche, filtres, actualisation et interface responsive inspirée du logo Yaar-App.

## Base de données
- Nouvelle colonne `shops.extra_immo_slots`.
- Table `yaar_admins` et fonction `is_yaar_admin()`.
- Policies d’administration et RPC de modération.
- Trigger serveur de limite des annonces immobilières.
