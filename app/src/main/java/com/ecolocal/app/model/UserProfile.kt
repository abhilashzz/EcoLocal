package com.ecolocal.app.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * UserProfile model stored in Cloud Firestore under users/{uid}.
 */
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val country: String = "",
    val locationText: String = "",
    val profileImageUrl: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val ecoPoints: Int = 0,
    val ecoLevel: String = "New Member"
) {
    val fullName: String
        get() = if (name.isNotBlank()) name else "EcoLocal User"

    val location: String
        get() = locationText

    companion object {
        fun fromDocument(doc: DocumentSnapshot): UserProfile? {
            if (!doc.exists()) return null
            return try {
                UserProfile(
                    uid = doc.getString("uid") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    address = doc.getString("address") ?: "",
                    city = doc.getString("city") ?: "",
                    country = doc.getString("country") ?: "",
                    locationText = doc.getString("locationText") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    rating = doc.getDouble("rating") ?: 0.0,
                    reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                    ecoPoints = doc.getLong("ecoPoints")?.toInt() ?: 0,
                    ecoLevel = doc.getString("ecoLevel") ?: "New Member"
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
