package com.ecolocal.app.model

import androidx.annotation.DrawableRes

/**
 * Service category model for Home 3-col grid.
 */
data class ServiceCategory(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int
)

/**
 * Marketplace category model for Home row & Marketplace circle row.
 */
data class MarketCategory(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int
)

/**
 * Full-width nearby listing for Home dashboard.
 */
data class NearbyListing(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val isPriceOrange: Boolean = false,
    val location: String? = null,
    val statusText: String,
    val isStatusAvailable: Boolean = true,
    @DrawableRes val imageRes: Int = 0,
    val imageUri: String? = null
)

/**
 * Product item for Marketplace 2-col grid.
 */
data class MarketProduct(
    val id: String,
    val title: String,
    val price: String,
    val condition: String,
    val location: String,
    @DrawableRes val imageRes: Int = 0,
    val imageUri: String? = null,
    val isGiveaway: Boolean = false,
    var isFavorite: Boolean = false
)

/**
 * Sealed hierarchy for Community Services board supporting distinct card types:
 *  - Service Offer (with photo thumbnail and availability dot)
 *  - Help Request (with REQUEST badge, status, and outlined Offer Help button)
 */
sealed class CommunityServiceItem {
    data class Offer(
        val id: String,
        val title: String,
        val location: String,
        val price: String,
        val status: String = "Available",
        @DrawableRes val imageRes: Int,
        val providerName: String = "Kamal Fernando",
        val category: String = "Tutoring"
    ) : CommunityServiceItem()

    data class Request(
        val id: String,
        val title: String,
        val location: String,
        val timeText: String = "Needed this week",
        val requesterName: String = "Saman Kumara",
        val category: String = "Repairs",
        @DrawableRes val imageRes: Int = 0
    ) : CommunityServiceItem()
}
