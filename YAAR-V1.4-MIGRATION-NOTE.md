# Yaar-App V1.4 — migration Supabase

Cette version ajoute :
- `shops.name_changed_at` pour le délai de 30 jours entre deux changements de nom ;
- `products.internal_discussion_enabled` ;
- `products.whatsapp_discussion_enabled` ;
- un trigger PostgreSQL qui bloque côté serveur tout changement de nom avant 30 jours.

## À faire avant de tester la nouvelle APK

Dans le SQL Editor de votre projet Supabase, exécutez le fichier `Yaar-App-Supabase-ALL-IN-ONE.sql` fourni dans ce ZIP (ou au minimum les commandes V1.4 qu'il contient).

Les anciennes boutiques et les anciens produits restent compatibles : les nouveaux champs prennent les valeurs par défaut `true`, donc les deux discussions restent activées pour les produits existants.
