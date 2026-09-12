package com.ecolocal.app.model

/**
 * UserProfile model stored in Cloud Firestore under users/{uid}.
 */
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
)
