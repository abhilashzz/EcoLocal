package com.ecolocal.app.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Manages user reviews and star ratings stored under reviews/{reviewId}.
 * Keeps ratings strictly independent from EcoPoints.
 */
object ReviewRepository {

    private const val TAG = "ReviewRepository"
    private const val COLLECTION_REVIEWS = "reviews"
    private const val COLLECTION_USERS = "users"

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    data class UserReview(
        val reviewId: String = "",
        val reviewerId: String = "",
        val reviewerName: String = "",
        val reviewedUserId: String = "",
        val relatedRequestId: String = "",
        val rating: Double = 5.0,
        val comment: String = "",
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun toMap(): Map<String, Any?> {
            return mapOf(
                "reviewId" to reviewId,
                "reviewerId" to reviewerId,
                "reviewerName" to reviewerName,
                "reviewedUserId" to reviewedUserId,
                "relatedRequestId" to relatedRequestId,
                "rating" to rating,
                "comment" to comment,
                "createdAt" to createdAt
            )
        }
    }

    /**
     * Submits a review.
     * Prevents self-review and duplicate review for the same interaction.
     */
    fun submitReview(
        review: UserReview,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (review.reviewerId.isBlank() || review.reviewedUserId.isBlank()) {
            onComplete(false, "Invalid user identifiers")
            return
        }

        // Prevent self review
        if (review.reviewerId == review.reviewedUserId) {
            onComplete(false, "You cannot review your own profile")
            return
        }

        // Deterministic document ID to prevent duplicate reviews for the same request
        val reviewDocId = if (review.relatedRequestId.isNotBlank()) {
            "${review.reviewerId}_${review.reviewedUserId}_${review.relatedRequestId}"
        } else {
            review.reviewId.ifBlank { "rev_${System.currentTimeMillis()}" }
        }

        val reviewDoc = review.copy(reviewId = reviewDocId)

        // Check if duplicate review already exists
        firestore.collection(COLLECTION_REVIEWS)
            .document(reviewDocId)
            .get()
            .addOnSuccessListener { docSnapshot ->
                if (docSnapshot.exists()) {
                    onComplete(false, "You have already submitted a review for this interaction")
                    return@addOnSuccessListener
                }

                // Write review
                firestore.collection(COLLECTION_REVIEWS)
                    .document(reviewDocId)
                    .set(reviewDoc.toMap())
                    .addOnSuccessListener {
                        updateUserAggregateRating(review.reviewedUserId)
                        onComplete(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to submit review", e)
                        onComplete(false, e.message ?: "Failed to save review")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking existing review", e)
                onComplete(false, e.message)
            }
    }

    /**
     * Recomputes average rating and review count from real reviews and persists to users/{uid}.
     */
    private fun updateUserAggregateRating(userId: String) {
        firestore.collection(COLLECTION_REVIEWS)
            .whereEqualTo("reviewedUserId", userId)
            .get()
            .addOnSuccessListener { snapshots ->
                val reviews = snapshots.documents.mapNotNull { doc ->
                    doc.getDouble("rating")
                }
                val count = reviews.size
                val avg = if (count > 0) {
                    Math.round((reviews.sum() / count) * 10.0) / 10.0
                } else {
                    0.0
                }

                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .set(
                        mapOf(
                            "rating" to avg,
                            "reviewCount" to count
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        Log.d(TAG, "Updated user $userId rating: $avg ($count reviews)")
                    }
            }
    }
}
