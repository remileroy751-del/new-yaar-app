/**
 * Cloud Functions pour Yaar-App.
 *
 * Déploiement : depuis la racine du projet (là où se trouve firebase.json) :
 *   cd functions && npm install && cd ..
 *   firebase deploy --only functions
 *
 * Prérequis : avoir passé le projet Firebase au plan "Blaze" (pay-as-you-go).
 * Les Cloud Functions ne sont PAS disponibles sur le plan gratuit "Spark" — voir la
 * note dans BACKEND_FIREBASE.md. Le plan Blaze reste gratuit tant que vous restez
 * sous les quotas gratuits (2 millions d'appels/mois pour les fonctions HTTPS) ;
 * une carte est demandée mais rien n'est prélevé sauf dépassement de ces quotas.
 */

const { onCall } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Moteur d'exposition des campagnes publicitaires.
 *
 * Appelée UNE FOIS par l'app Android à chaque ouverture (voir
 * YaarRepository.recordAppOpenExposure côté Kotlin — à remplacer par un appel à
 * cette fonction une fois Firestore branché). Contrairement à la version locale
 * (Room), ce compteur est partagé par TOUS les téléphones : chaque campagne perd
 * une exposition par ouverture d'app, tous utilisateurs confondus — c'est ce qui
 * correspond à "le produit apparaît sur 100 profils dès l'ouverture de l'application".
 */
exports.recordAppOpenExposure = onCall(async () => {
  const now = Date.now();
  const snapshot = await db.collection("ad_campaigns").where("isActive", "==", true).get();

  const batch = db.batch();
  for (const doc of snapshot.docs) {
    const campaign = doc.data();
    const newRemaining = Math.max(0, campaign.remainingExpositions - 1);
    const stillRunning = newRemaining > 0 && now < campaign.endsAt;

    batch.update(doc.ref, {
      remainingExpositions: newRemaining,
      isActive: stillRunning,
    });

    if (!stillRunning) {
      batch.update(db.collection("products").doc(campaign.productId), {
        isPromoted: false,
      });
    }
  }
  await batch.commit();

  return { processed: snapshot.size };
});

/**
 * Notification push "Je suis intéressé".
 *
 * Se déclenche automatiquement à chaque création d'un document dans la collection
 * "interests" (quand un acheteur clique "Je suis intéressé" sur un produit). Envoie
 * une notification push au vendeur concerné (shopOwnerId), UNIQUEMENT si son compte
 * a activé les notifications (users/{uid}.notificationsEnabled) et possède un
 * jeton FCM enregistré (users/{uid}.fcmToken).
 */
exports.notifyShopOwnerOfInterest = onDocumentCreated("interests/{interestId}", async (event) => {
  const interest = event.data.data();

  const ownerSnap = await db.collection("users").doc(String(interest.shopOwnerId)).get();
  if (!ownerSnap.exists) return;

  const owner = ownerSnap.data();
  if (owner.notificationsEnabled === false) return;
  if (!owner.fcmToken) return;

  await admin.messaging().send({
    token: owner.fcmToken,
    notification: {
      title: "Nouveau client intéressé !",
      body: `${interest.buyerFirstName} est intéressé(e) par "${interest.productName}".`,
    },
    data: {
      type: "interest",
      interestId: event.params.interestId,
      productId: String(interest.productId),
    },
  });
});
