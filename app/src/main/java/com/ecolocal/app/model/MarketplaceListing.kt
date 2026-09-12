package com.ecolocal.app.model

import androidx.annotation.DrawableRes

/**
 * Shared data model representing a Marketplace listing across Create, Preview, Details, and Edit flows.
 */
data class MarketplaceListing(
    val id: String,
    val ownerId: String = "user_nimal",
    val listingType: String = "FOR SALE", // "FOR SALE", "GIVE AWAY", "DONATION"
    val title: String,
    val price: String, // e.g. "Rs. 15,000" or "FREE"
    val category: String, // e.g. "Furniture", "Electronics", "Books", "Clothing", "Home", "Other"
    val condition: String, // "Like New", "Good", "Fair"
    val description: String,
    val location: String,
    @DrawableRes val imageRes: Int = 0,
    val imageUri: String? = null,
    @DrawableRes val secondaryImageRes: Int? = null,
    val isAvailable: Boolean = true,
    val sellerName: String = "Nimal Perera",
    @DrawableRes val sellerAvatarRes: Int = 0,
    val memberSince: String = "Member since 2026",
    val createdAt: Long = System.currentTimeMillis()
)
