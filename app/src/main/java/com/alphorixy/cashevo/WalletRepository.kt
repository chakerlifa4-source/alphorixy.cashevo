package com.alphorixy.cashevo

import com.google.firebase.firestore.FirebaseFirestore

object WalletRepository {
    private val db = FirebaseFirestore.getInstance()

    fun addRewardTransaction(
        userId: String,
        reward: Double,
        taskName: String,
        onSuccess: (newBalance: Double) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userRef = db.collection("users").document(userId)
        val historyRef = db.collection("task_history").document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentBalance = snapshot.getDouble("balance") ?: 0.0

            val newBalance = currentBalance + reward

            transaction.update(userRef, "balance", newBalance)

            val historyData = mapOf(
                "userId" to userId,
                "reward" to reward,
                "taskName" to taskName,
                "timestamp" to System.currentTimeMillis()
            )
            transaction.set(historyRef, historyData)

            newBalance
        }.addOnSuccessListener { newBalance ->
            onSuccess(newBalance)
        }.addOnFailureListener { e ->
            onError(e)
        }
    }
}