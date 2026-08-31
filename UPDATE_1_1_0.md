# Yaar-App 1.1.0 — comptes Firebase et ciblage par ville

## Ce qui change

- Inscription en 3 étapes : pays/ville → nom complet + WhatsApp → mot de passe + confirmation.
- Mot de passe exactement 6 caractères alphanumériques, avec afficher/masquer.
- Connexion par numéro WhatsApp + mot de passe.
- Firebase Authentication Email/Password devient l'identité permanente du compte.
- Le mot de passe n'est pas enregistré dans Firestore ou Room.
- Le profil est enregistré dans `users/{firebaseUid}`.
- Les boutiques portent `ownerUid`.
- Les produits portent `ownerUid`, `shopRemoteId` et `availableCities`.
- Un produit est visible dans la ville principale et au maximum 5 villes supplémentaires.
- Marketplace : produits du pays de l'acheteur et de sa ville priorisés.
- Recherche : résultats de la ville de l'acheteur d'abord, puis autres villes du même pays.
- Migration Room 6 → 7 sans suppression des données locales.
- Les anciens comptes anonymes sont invités à définir un mot de passe une seule fois.

## Action Firebase obligatoire

Dans Firebase Console → Authentication → Sign-in method : activer **Email/Password**.

Garder **Anonymous** activé pendant la migration des anciens comptes.

Après avoir mis à jour `firestore.rules`, déployer les règles :

```text
firebase deploy --only firestore:rules
```

## Migration des utilisateurs existants

Ne pas désinstaller l'ancienne application avant que l'utilisateur existant ait ouvert cette
version et terminé l'écran **Sécurisez votre compte**. Cette étape lie l'ancien compte
Firebase anonyme à un compte permanent avec le même UID et rattache les boutiques et produits
existants à cet UID.

Une fois cette étape terminée, l'utilisateur peut installer l'application sur un autre
appareil et se connecter avec son numéro WhatsApp + son mot de passe.
