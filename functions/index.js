const {setGlobalOptions} = require("firebase-functions");
const {onRequest, onCall, HttpsError} = require("firebase-functions/v2/https");
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
    if (t.firebase.sign_in_provider === "password" && t.email_verified !== true) {
      throw new HttpsError("permission-denied", "يجب توثيق بريدك الإلكتروني أولاً");
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

exports.claimWeeklyReward = onCall({enforceAppCheck: false}, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "يجب تسجيل الدخول أولاً لاستلام المكافأة.");
  }

  const provider = request.auth.token.firebase && request.auth.token.firebase.sign_in_provider;
  if (provider === "anonymous") {
    throw new HttpsError("permission-denied", "سجّل الدخول بحساب Google أو بريد إلكتروني لاستلام المكافأة.");
  }
  if (provider === "password" && request.auth.token.email_verified !== true) {
    throw new HttpsError("permission-denied", "يجب توثيق بريدك الإلكتروني أولاً.");
  }

  const deviceId = request.data && request.data.deviceId;
  if (typeof deviceId !== "string" || deviceId.length < 8 || deviceId.length > 128) {
    throw new HttpsError("invalid-argument", "تعذّر التعرف على الجهاز.");
  }

  const WEEK = 7 * 24 * 60 * 60 * 1000;
  const REWARD = 0.06;
  const uid = request.auth.uid;
  const deviceKey = crypto.createHash("sha256").update("cashevo-device:" + deviceId).digest("hex");

  const userRef = db.collection("users").doc(uid);
  const deviceRef = db.collection("device_claims").doc(deviceKey);
  const historyRef = userRef.collection("task_history").doc();

  return await db.runTransaction(async (t) => {
    const [userSnap, deviceSnap] = await Promise.all([t.get(userRef), t.get(deviceRef)]);
    if (!userSnap.exists) {
      throw new HttpsError("not-found", "بيانات المستخدم غير موجودة.");
    }

    const now = Date.now();
    const userLast = userSnap.data().lastWeeklyReward || 0;
    if (userLast !== 0 && now - userLast < WEEK) {
      throw new HttpsError("failed-precondition", "لم يحن موعد المكافأة الأسبوعية بعد. يرجى الانتظار حتى تنقضي 7 أيام.");
    }

    const deviceLast = deviceSnap.exists ? (deviceSnap.data().lastClaim || 0) : 0;
    if (deviceLast !== 0 && now - deviceLast < WEEK) {
      throw new HttpsError("already-exists", "تم استلام المكافأة الأسبوعية على هذا الجهاز بالفعل. حاول في الأسبوع القادم.");
    }

    t.update(userRef, {
      balance: admin.firestore.FieldValue.increment(REWARD),
      lastWeeklyReward: now,
    });
    t.set(deviceRef, {
      lastClaim: now,
      lastUid: uid,
      claims: admin.firestore.FieldValue.increment(1),
    }, {merge: true});
    t.set(historyRef, {
      userId: uid,
      title: "المكافأة الأسبوعية",
      reward: REWARD,
      timestamp: now,
      type: "WEEKLY_REWARD",
    });

    return {success: true};
  });
});