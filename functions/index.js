const {setGlobalOptions} = require("firebase-functions");
const {onRequest, onCall} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

setGlobalOptions({ maxInstances: 10 });

const CPX_SECRET = defineSecret("CPX_SECRET");

exports.requestWithdrawal = onCall({enforceAppCheck: false}, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "سجّل الدخول أولاً");

  const t = request.auth.token;
  if (t.firebase.sign_in_provider === "anonymous") {
    throw new HttpsError("permission-denied", "يجب استخدام حساب مُوثّق");
  }

  const {amount, fullName, accountNumber, paymentMethod} = request.data || {};
  const METHODS = ["PayPal", "Payeer", "USDT", "BaridiMob"];

  if (!Number.isFinite(amount) || amount < 5 || amount > 500 ||
      !METHODS.includes(paymentMethod) ||
      typeof fullName !== "string" || fullName.length > 80 ||
      typeof accountNumber !== "string" || accountNumber.length > 100) {
    throw new HttpsError("invalid-argument", "بيانات غير صحيحة");
  }

  const userRef = db.collection("users").doc(request.auth.uid);
  const wRef = db.collection("withdrawals").doc();

  await db.runTransaction(async (tx) => {
    const u = await tx.get(userRef);
    if (!u.exists) throw new HttpsError("not-found", "المستخدم غير موجود");

    const bal = u.data()?.balance || 0;
    if (bal < amount) throw new HttpsError("failed-precondition", "الرصيد غير كافٍ");

    tx.update(userRef, {balance: bal - amount});
    tx.set(wRef, {
      userId: request.auth.uid,
      fullName,
      accountNumber,
      amount,
      paymentMethod,
      status: "pending",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });

  return {ok: true};
});

exports.cpxPostback = onRequest({secrets: [CPX_SECRET]}, async (req, res) => {
    try {
        const {ext_user_id: uid, trans_id: txId, amount_usd, status, hash} = req.query;

        if (!uid || !txId || !hash) {
            logger.warn("Missing parameters: uid, trans_id, or hash");
            return res.status(400).send("bad");
        }

        const expected = crypto.createHash("md5").update(`${txId}-${CPX_SECRET.value()}`).digest("hex");
        const ok = hash.length === expected.length &&
                   crypto.timingSafeEqual(Buffer.from(hash), Buffer.from(expected));

        if (!ok) {
            logger.warn("Invalid CPX signature hash");
            return res.status(403).send("forbidden");
        }

        const amount = Number(amount_usd);
        if (String(status) !== "1" || !Number.isFinite(amount) || amount <= 0 || amount > 20) {
            logger.warn("Invalid status or amount");
            return res.status(400).send("bad");
        }

        const txRef = db.collection("cpx_transactions").doc(String(txId));
        const userRef = db.collection("users").doc(String(uid));

        await db.runTransaction(async (t) => {
            const txDoc = await t.get(txRef);
            if (txDoc.exists) {
                return;
            }

            const userDoc = await t.get(userRef);
            if (!userDoc.exists) {
                throw new Error("User does not exist in database");
            }

            t.update(userRef, { balance: admin.firestore.FieldValue.increment(amount) });
            t.create(txRef, { uid, amount, at: Date.now() });
        });

        logger.info(`Successfully rewarded user ${uid} with $${amount} for transaction ${txId}`);
        return res.status(200).send("OK");

    } catch (error) {
        logger.error("Error processing CPX postback:", error);
        return res.status(500).send("Internal Server Error");
    }
});

exports.claimWeeklyReward = onCall(async (request) => {
    if (!request.auth) {
        throw new Error("يجب تسجيل الدخول أولاً لاستلام المكافأة.");
    }

    const userId = request.auth.uid;
    const userRef = db.collection("users").doc(userId);
    const historyRef = userRef.collection("task_history").doc();

    try {
        return await db.runTransaction(async (transaction) => {
            const snapshot = await transaction.get(userRef);
            if (!snapshot.exists) {
                throw new Error("بيانات المستخدم غير موجودة.");
            }

            const userData = snapshot.data();
            const currentBalance = userData.balance || 0.0;
            const lastWeeklyReward = userData.lastWeeklyReward || 0;
            const currentTime = Date.now();

            const sevenDaysMillis = 7 * 24 * 60 * 60 * 1000;

            if (lastWeeklyReward !== 0 && (currentTime - lastWeeklyReward < sevenDaysMillis)) {
                throw new Error("لم يحن موعد المكافأة الأسبوعية بعد. يرجى الانتظار حتى تنقضي 7 أيام.");
            }

            const newBalance = currentBalance + 0.06;

            transaction.update(userRef, {
                balance: newBalance,
                lastWeeklyReward: currentTime
            });

            transaction.set(historyRef, {
                title: "المكافأة الأسبوعية",
                reward: 0.06,
                timestamp: currentTime,
                type: "WEEKLY_REWARD"
            });

            return { success: true, newBalance: newBalance };
        });
    } catch (error) {
        throw new Error(error.message || "فشل استلام المكافأة.");
    }
});