package com.ecolocal.app.model

import androidx.annotation.DrawableRes
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Shared data model representing a Marketplace listing across Create, Preview, Details, and Edit flows.
 * Compatible with Cloud Firestore and local persistence.
 */
@IgnoreExtraProperties
data class MarketplaceListing(
    val listingId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val listingType: String = "FOR SALE", // "FOR SALE", "FREE", "DONATION", "GIVE AWAY"
    val price: String = "", // e.g. "Rs. 15,000" or "FREE"
    val condition: String = "Good", // "Like New", "Good", "Fair"
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    @DrawableRes val imageRes: Int = 0,
    val imageUri: String? = null,
    @DrawableRes val secondaryImageRes: Int? = null,
    val isAvailable: Boolean = true,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "DELETED"
    @DrawableRes val sellerAvatarRes: Int = 0,
    val memberSince: String = "Member since 2026",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Backwards compatibility properties
    @get:Exclude
    val id: String
        get() = if (listingId.isNotEmpty()) listingId else ""

    @get:Exclude
    val sellerName: String
        get() = if (ownerName.isNotEmpty()) ownerName else "Community Member"

    @get:Exclude
    val location: String
        get() = locationName

    // Secondary constructor for backwards compatibility with existing call sites
    constructor(
        id: String,
        ownerId: String = "user_nimal",
        listingType: String = "FOR SALE",
        title: String,
        price: String,
        category: String,
        condition: String,
        description: String,
        location: String,
        @DrawableRes imageRes: Int = 0,
        imageUri: String? = null,
        @DrawableRes secondaryImageRes: Int? = null,
        isAvailable: Boolean = true,
        sellerName: String = "Nimal Perera",
        @DrawableRes sellerAvatarRes: Int = 0,
        memberSince: String = "Member since 2026",
        createdAt: Long = System.currentTimeMillis(),
        status: String = "ACTIVE",
        latitude: Double? = null,
        longitude: Double? = null,
        imageUrl: String? = null,
        updatedAt: Long = createdAt
    ) : this(
        listingId = id,
        ownerId = ownerId,
        ownerName = sellerName,
        title = title,
        description = description,
        category = category,
        listingType = listingType,
        price = price,
        condition = condition,
        locationName = location,
        latitude = latitude,
        longitude = longitude,
        imageUrl = imageUrl ?: imageUri,
        imageRes = imageRes,
        imageUri = imageUri,
        secondaryImageRes = secondaryImageRes,
        isAvailable = isAvailable,
        status = status,
        sellerAvatarRes = sellerAvatarRes,
        memberSince = memberSince,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "listingId" to listingId,
            "ownerId" to ownerId,
            "ownerName" to ownerName,
            "title" to title,
            "description" to description,
            "category" to category,
            "listingType" to listingType,
            "price" to price,
            "condition" to condition,
            "locationName" to locationName,
            "latitude" to latitude,
            "longitude" to longitude,
            "imageUrl" to imageUrl,
            "imageRes" to imageRes,
            "imageUri" to imageUri,
            "secondaryImageRes" to secondaryImageRes,
            "isAvailable" to isAvailable,
            "status" to status,
            "sellerAvatarRes" to sellerAvatarRes,
            "memberSince" to memberSince,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    fun toMarketProduct(): MarketProduct {
        return MarketProduct(
            id = listingId,
            title = title,
            price = price,
            condition = if (condition.contains("Condition", ignoreCase = true)) condition else "$condition Condition",
            location = locationName,
            imageRes = imageRes,
            imageUri = imageUrl ?: imageUri,
            isGiveaway = listingType.equals("GIVE AWAY", ignoreCase = true) || price.equals("FREE", ignoreCase = true),
            isFavorite = false
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): MarketplaceListing? {
            if (!doc.exists()) return null
            return try {
                val listingId = doc.getString("listingId") ?: doc.id
                val ownerId = doc.getString("ownerId") ?: ""
                val ownerName = doc.getString("ownerName") ?: doc.getString("sellerName") ?: "Community Member"
                val title = doc.getString("title") ?: ""
                val description = doc.getString("description") ?: ""
                val category = doc.getString("category") ?: ""
                val listingType = doc.getString("listingType") ?: "FOR SALE"
                val price = doc.getString("price") ?: "Rs. 0"
                val condition = doc.getString("condition") ?: "Good"
                val locationName = doc.getString("locationName") ?: doc.getString("location") ?: ""
                val latitude = doc.getDouble("latitude")
                val longitude = doc.getDouble("longitude")
                val rawImageUrl = doc.getString("imageUrl") ?: doc.getString("imageUri")
                val imageUrl = rawImageUrl
                val imageRes = doc.getLong("imageRes")?.toInt() ?: 0
                val imageUri = rawImageUrl
                val secondaryImageRes = doc.getLong("secondaryImageRes")?.toInt()
                val isAvailable = doc.getBoolean("isAvailable") ?: true
                val status = doc.getString("status") ?: "ACTIVE"
                val sellerAvatarRes = doc.getLong("sellerAvatarRes")?.toInt() ?: 0
                val memberSince = doc.getString("memberSince") ?: "Member since 2026"
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: createdAt

                MarketplaceListing(
                    listingId = listingId,
                    ownerId = ownerId,
                    ownerName = ownerName,
                    title = title,
                    description = description,
                    category = category,
                    listingType = listingType,
                    price = price,
                    condition = condition,
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = imageUrl,
                    imageRes = imageRes,
                    imageUri = imageUri,
                    secondaryImageRes = secondaryImageRes,
                    isAvailable = isAvailable,
                    status = status,
                    sellerAvatarRes = sellerAvatarRes,
                    memberSince = memberSince,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
