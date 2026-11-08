const {setGlobalOptions} = require("firebase-functions");
const {onRequest, onCall} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

setGlobalOptions({ maxInstances: 10 });

exports.cpxPostback = onRequest(async (req, res) => {
    try {
        const userId = req.query.uid;
        const payout = req.query.amount;

        if (!userId || !payout) {
            logger.warn("Missing parameters: uid or amount");
            return res.status(400).send("Missing parameters: uid or amount");
        }

        const rewardAmount = parseFloat(payout);
        if (isNaN(rewardAmount)) {
            logger.warn("Invalid amount format");
            return res.status(400).send("Invalid amount format");
        }

        const userRef = db.collection("users").doc(userId);

        await db.runTransaction(async (transaction) => {
            const userDoc = await transaction.get(userRef);
            if (!userDoc.exists) {
                throw new Error("User does not exist in database");
            }

            const currentBalance = userDoc.data().balance || 0.0;
            const newBalance = currentBalance + rewardAmount;

            transaction.update(userRef, { balance: newBalance });
        });

        logger.info(`Successfully rewarded user ${userId} with $${rewardAmount}`);
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