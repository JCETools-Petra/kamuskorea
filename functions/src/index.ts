import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { google } from "googleapis";

admin.initializeApp();
const firestore = admin.firestore();

const packageName = "com.webtech.kamuskorea";

export const verifyPurchase = functions.https.onCall(
  async (
    data: { purchaseToken: string; productId: string },
    context
  ): Promise<{ status: string }> => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication is required."
      );
    }

    const { purchaseToken, productId } = data;
    const uid = context.auth.uid;

    try {
      // Gunakan GoogleAuth dan oper langsung ke androidpublisher (hindari AnyAuthClient typing error)
      const googleAuth = new google.auth.GoogleAuth({
        scopes: ["https://www.googleapis.com/auth/androidpublisher"],
      });

      const publisher = google.androidpublisher({
        version: "v3",
        auth: googleAuth,
      });

      const response = await publisher.purchases.subscriptions.get({
        packageName,
        subscriptionId: productId,
        token: purchaseToken,
      });

      const expiryTimeMillis = response.data.expiryTimeMillis;
      const expiryMs = expiryTimeMillis ? Number(expiryTimeMillis) : 0;
      const isActive = expiryMs > Date.now();

      await firestore.collection("users").doc(uid).set(
        { isPremium: isActive },
        { merge: true }
      );

      return { status: isActive ? "success" : "expired" };
    } catch (error) {
      functions.logger.error("Purchase verification failed:", error as any);
      throw new functions.https.HttpsError("internal", "Verification failed.");
    }
  }
);