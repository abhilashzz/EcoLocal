package com.ecolocal.app.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object UserRepository {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private const val COLLECTION_USERS = "users"

    /**
     * Creates a new user profile document in Firestore at users/{uid}.
     */
    fun createUserProfile(
        uid: String,
        name: String,
        email: String,
        locationText: String = "",
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userMap = hashMapOf<String, Any?>(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "phone" to "",
            "address" to "",
            "city" to "",
            "country" to "",
            "locationText" to locationText,
            "profileImageUrl" to "",
            "rating" to 0.0,
            "reviewCount" to 0,
            "ecoPoints" to 0,
            "ecoLevel" to "New Member",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .set(userMap)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Checks if users/{uid} exists in Firestore.
     * If missing, creates a minimal fallback profile without overwriting existing data.
     */
    fun ensureUserProfileExists(
        user: FirebaseUser,
        onComplete: (Boolean) -> Unit
    ) {
        val docRef = firestore.collection(COLLECTION_USERS).document(user.uid)
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Profile already exists; do NOT overwrite!
                    onComplete(true)
                } else {
                    // Profile document is missing; create minimal document safely
                    val fallbackName = user.displayName?.takeIf { it.isNotBlank() }
                        ?: user.email?.substringBefore("@")
                        ?: "EcoLocal User"

                    val minimalMap = hashMapOf<String, Any?>(
                        "uid" to user.uid,
                        "name" to fallbackName,
                        "email" to (user.email ?: ""),
                        "phone" to "",
                        "address" to "",
                        "city" to "",
                        "country" to "",
                        "locationText" to "",
                        "profileImageUrl" to "",
                        "rating" to 0.0,
                        "reviewCount" to 0,
                        "ecoPoints" to 0,
                        "ecoLevel" to "New Member",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    docRef.set(minimalMap)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }
            }
            .addOnFailureListener {
                // If offline or failed read, do not block login
                onComplete(false)
            }
    }
}
