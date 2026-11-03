const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

// تهيئة أدوات إدارة فايربيس وقاعدة البيانات
admin.initializeApp();
const db = admin.firestore();

// إعداد الحد الأقصى للحاويات حسب إعداداتك الافتراضية
setGlobalOptions({ maxInstances: 10 });

// دالة استقبال الـ Postback من CPX Research وتحديث رصيد المستخدم
exports.cpxPostback = onRequest(async (req, res) => {
    try {
        const userId = req.query.uid;       // معرف المستخدم المرسل من تطبيقك (Firebase UID)
        const payout = req.query.amount;    // قيمة الأرباح المرسلة من الشركة

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

        // تحديث الرصيد سحابياً بشكل آمن ودقيق عبر Transaction
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