package com.ecolocal.app.data

import com.ecolocal.app.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object UserRepository {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private const val COLLECTION_USERS = "users"

    private var cachedProfile: UserProfile? = null

    fun clearCache() {
        cachedProfile = null
    }

    /**
     * Returns currently cached UserProfile, or builds a fallback profile from FirebaseUser
     * and schedules background fetch.
     */
    fun getCurrentUser(): UserProfile? {
        if (cachedProfile != null) return cachedProfile

        val firebaseUser = auth.currentUser ?: return null
        val fallback = UserProfile(
            uid = firebaseUser.uid,
            name = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: firebaseUser.email?.substringBefore("@")
                ?: "EcoLocal User",
            email = firebaseUser.email ?: ""
        )
        cachedProfile = fallback

        // Background fetch real profile
        loadUserProfile(firebaseUser.uid)

        return fallback
    }

    fun loadUserProfile(uid: String, onComplete: ((UserProfile?) -> Unit)? = null) {
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val profile = UserProfile.fromDocument(doc)
                if (profile != null) {
                    cachedProfile = profile
                }
                onComplete?.invoke(profile)
            }
            .addOnFailureListener {
                onComplete?.invoke(null)
            }
    }

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

        cachedProfile = UserProfile(
            uid = uid,
            name = name,
            email = email,
            locationText = locationText
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
                    cachedProfile = UserProfile.fromDocument(documentSnapshot)
                    onComplete(true)
                } else {
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

                    cachedProfile = UserProfile(
                        uid = user.uid,
                        name = fallbackName,
                        email = user.email ?: ""
                    )

                    docRef.set(minimalMap)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    private val profileChangeListeners = mutableListOf<() -> Unit>()

    fun addProfileChangeListener(listener: () -> Unit) {
        synchronized(profileChangeListeners) {
            if (!profileChangeListeners.contains(listener)) {
                profileChangeListeners.add(listener)
            }
        }
    }

    fun removeProfileChangeListener(listener: () -> Unit) {
        synchronized(profileChangeListeners) {
            profileChangeListeners.remove(listener)
        }
    }

    fun notifyProfileChanged() {
        val listeners = synchronized(profileChangeListeners) { profileChangeListeners.toList() }
        listeners.forEach { it.invoke() }
    }

    /**
     * Saves user profile updates to users/{uid} and updates Firebase Auth displayName.
     */
    fun updateUserProfile(
        uid: String,
        name: String,
        phone: String,
        address: String,
        city: String,
        country: String,
        locationText: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updates = hashMapOf<String, Any?>(
            "name" to name,
            "phone" to phone,
            "address" to address,
            "city" to city,
            "country" to country,
            "locationText" to locationText,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                val authUser = auth.currentUser
                if (authUser != null && name.isNotBlank() && authUser.displayName != name) {
                    val profileChange = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    authUser.updateProfile(profileChange)
                }

                val prev = cachedProfile
                if (prev != null) {
                    cachedProfile = prev.copy(
                        name = name,
                        phone = phone,
                        address = address,
                        city = city,
                        country = country,
                        locationText = locationText
                    )
                } else {
                    cachedProfile = UserProfile(
                        uid = uid,
                        name = name,
                        phone = phone,
                        address = address,
                        city = city,
                        country = country,
                        locationText = locationText
                    )
                }
                notifyProfileChanged()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateProfilePhoto(
        uid: String,
        profileImageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .set(
                mapOf(
                    "profileImageUrl" to profileImageUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                cachedProfile = cachedProfile?.copy(profileImageUrl = profileImageUrl)
                notifyProfileChanged()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
