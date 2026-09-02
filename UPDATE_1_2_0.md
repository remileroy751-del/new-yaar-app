# Yaar-App 1.2.0 — Actions du compte dans Mon profil

## Changements

- `Mon profil` affiche clairement les actions du compte dans l'ordre :
  1. **Certifier ma boutique** (si la boutique n'est pas encore certifiée)
  2. **Se déconnecter**
  3. **Supprimer mon compte**
- L'écran de profil est désormais **défilable verticalement**, afin que les actions
  restent accessibles et lisibles sur les petits écrans malgré la barre de navigation
  inférieure.
- Le bouton **Se déconnecter** utilise la déconnexion Firebase existante puis revient
  à la page de connexion/inscription.
- Le bouton **Supprimer mon compte** conserve la ré-authentification par mot de passe
  et la suppression des données locales + Firebase déjà implémentées.
- Version Android : `versionCode 12`, `versionName 1.2.0`.

## Compilation GitHub

Le dépôt reste compatible avec le workflow GitHub Actions existant. Le fichier
`app/google-services.json` est conservé comme dans la V1.1.9 et le workflow peut
également le restaurer depuis le secret `GOOGLE_SERVICES_JSON`.
