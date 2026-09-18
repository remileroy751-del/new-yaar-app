YAAR-APP — SUPER ADMIN

1. Déployer le dépôt sur GitHub.
2. Dans Supabase > Authentication > Users, créer le compte administrateur (email + mot de passe).
3. Dans le fichier SQL Yaar-App-Supabase-V1.5.1-IMMO-ADMIN.sql, remplacer VOTRE_EMAIL_ADMIN@EXEMPLE.COM par exactement cet email, puis exécuter le SQL dans Supabase.
4. Dans GitHub > Settings > Pages, choisir le déploiement depuis la branche principale et le dossier / (root).
5. Ouvrir l'URL GitHub Pages du dépôt : index.html est à la racine du dépôt.

Sécurité : ne jamais mettre la clé service_role dans index.html. Le fichier utilise uniquement la clé publishable Supabase et les policies/RPC administratives côté base.
