package com.ecolocal.app.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Manages EcoPoints logic, transaction history, level tiers, and prevents repeated farming.
 */
object EcoPointsRepository {

    private const val TAG = "EcoPointsRepository"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_TRANSACTIONS = "ecopoints_transactions"

    const val POINTS_CREATE_LISTING = 5
    const val POINTS_COMPLETE_REQUEST = 5
    const val POINTS_COMPLETE_INTERACTION = 10

    fun calculateLevel(points: Int): String {
        return when {
            points >= 200 -> "Community Champion"
            points >= 100 -> "Trusted Local"
            points >= 50 -> "Active Neighbor"
            else -> "New Member"
        }
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Awards EcoPoints for a specific action while ensuring no repeat point farming.
     */
    fun awardPoints(
        userId: String,
        actionType: String,
        referenceId: String,
        points: Int,
        onComplete: ((newTotal: Int) -> Unit)? = null
    ) {
        if (userId.isBlank() || points <= 0) {
            onComplete?.invoke(0)
            return
        }

        // Deduplication ID per user action and reference item
        val txId = "${userId}_${actionType}_${referenceId}"
        val txDocRef = firestore.collection(COLLECTION_TRANSACTIONS).document(txId)

        txDocRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Already awarded points for this action; prevent farming
                    Log.d(TAG, "EcoPoints already awarded for tx $txId")
                    onComplete?.invoke(0)
                    return@addOnSuccessListener
                }

                // Record transaction
                val txMap = hashMapOf<String, Any>(
                    "txId" to txId,
                    "userId" to userId,
                    "actionType" to actionType,
                    "referenceId" to referenceId,
                    "points" to points,
                    "timestamp" to System.currentTimeMillis()
                )

                txDocRef.set(txMap)
                    .addOnSuccessListener {
                        // Increment points on user document
                        val userDocRef = firestore.collection(COLLECTION_USERS).document(userId)
                        userDocRef.get().addOnSuccessListener { userSnap ->
                            val currentPoints = userSnap.getLong("ecoPoints")?.toInt() ?: 0
                            val updatedPoints = currentPoints + points
                            val updatedLevel = calculateLevel(updatedPoints)

                            userDocRef.set(
                                mapOf(
                                    "ecoPoints" to updatedPoints,
                                    "ecoLevel" to updatedLevel,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            ).addOnSuccessListener {
                                Log.d(TAG, "Awarded $points EcoPoints to $userId. New total: $updatedPoints ($updatedLevel)")
                                onComplete?.invoke(updatedPoints)
                            }.addOnFailureListener {
                                onComplete?.invoke(updatedPoints)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed creating EcoPoints transaction", e)
                        onComplete?.invoke(0)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking EcoPoints transaction", e)
                onComplete?.invoke(0)
            }
    }
}
