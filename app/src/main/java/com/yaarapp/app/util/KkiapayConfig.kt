package com.yaarapp.app.util

/**
 * Configuration du widget de paiement Kkiapay (https://kkiapay.me).
 *
 * ⚠️ À FAIRE avant la mise en production :
 * 1. Créez un compte marchand sur https://app.kkiapay.me (ou https://sandbox.kkiapay.me
 *    pour tester sans argent réel).
 * 2. Dans le tableau de bord Kkiapay → "Clés API", copiez votre clé PUBLIQUE
 *    (jamais la clé privée dans l'application mobile !) et collez-la ci-dessous.
 * 3. Passez [SANDBOX] à false une fois vos tests terminés et votre compte marchand validé.
 *
 * Le paiement se fait via le widget web officiel de Kkiapay (cdn.kkiapay.me/k.js), chargé
 * dans une WebView — c'est la méthode recommandée par Kkiapay pour les apps Android qui
 * n'utilisent pas leur SDK natif.
 */
object KkiapayConfig {
    const val PUBLIC_API_KEY = "VOTRE_CLE_PUBLIQUE_KKIAPAY"
    const val SANDBOX = true
}
