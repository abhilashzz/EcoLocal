package com.ecolocal.app.data

import android.content.Context
import com.ecolocal.app.R
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.MarketProduct
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.model.entity.ListingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Singleton repository acting as the single source of truth for Marketplace listings
 * backed by Room database for offline persistence.
 */
object ListingRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val initialSeed = listOf(
        MarketplaceListing(
            id = "desk_1",
            ownerId = "user_nadeesha",
            listingType = "For Sale",
            title = "Wooden Study Desk",
            price = "Rs. 8,500",
            category = "Furniture",
            condition = "Good",
            description = "Used wooden study desk with three drawers. Minor marks from normal use. Perfect for a student or home office setup. Sturdy build and easy to clean.",
            location = "Malabe",
            imageRes = R.drawable.img_mkt_desk,
            secondaryImageRes = R.drawable.img_home_desk,
            isAvailable = true,
            sellerName = "Nadeesha Silva",
            sellerAvatarRes = R.drawable.img_avatar_woman,
            memberSince = "Member since 2026"
        ),
        MarketplaceListing(
            id = "books_2",
            ownerId = "user_kamal",
            listingType = "GIVE AWAY",
            title = "Textbooks Bundle",
            price = "FREE",
            category = "Books",
            condition = "Used",
            description = "Giving away a collection of O/L and A/L textbooks and reference books. All in usable condition. Pickup from Kaduwela.",
            location = "Kaduwela",
            imageRes = R.drawable.img_mkt_textbooks,
            isAvailable = true,
            sellerName = "Kamal Fernando",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026"
        ),
        MarketplaceListing(
            id = "cooker_3",
            ownerId = "user_saman",
            listingType = "FOR SALE",
            title = "Rice Cooker",
            price = "Rs. 4,500",
            category = "Electronics",
            condition = "Good",
            description = "Reliable 1.8L rice cooker, lightly used for 6 months. Works perfectly with original measuring cup and steam tray.",
            location = "Battaramulla",
            imageRes = R.drawable.img_mkt_ricecooker,
            isAvailable = true,
            sellerName = "Saman Kumara",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026"
        ),
        MarketplaceListing(
            id = "fan_4",
            ownerId = "user_nimal",
            listingType = "FOR SALE",
            title = "Ceiling Fan 56-inch",
            price = "Rs. 5,200",
            category = "Home",
            condition = "Like New",
            description = "High-speed 3-blade ceiling fan with regulator. Installed for 2 months only. Perfect condition.",
            location = "Kaduwela",
            imageRes = R.drawable.img_mkt_desk,
            isAvailable = true,
            sellerName = "Nimal Perera",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026"
        ),
        MarketplaceListing(
            id = "kettle_5",
            ownerId = "user_priya",
            listingType = "FOR SALE",
            title = "Electric Kettle 1.7L",
            price = "Rs. 2,800",
            category = "Electronics",
            condition = "Good",
            description = "Stainless steel electric kettle with auto shut-off. Boiling fast and clean.",
            location = "Malabe",
            imageRes = R.drawable.img_mkt_ricecooker,
            isAvailable = true,
            sellerName = "Priya Jayasuriya",
            sellerAvatarRes = R.drawable.img_avatar_woman,
            memberSince = "Member since 2026"
        ),
        MarketplaceListing(
            id = "helmet_6",
            ownerId = "user_nadeesha",
            listingType = "DONATION",
            title = "Bicycle Safety Helmet",
            price = "FREE",
            category = "Clothing",
            condition = "Fair",
            description = "Lightweight bike helmet, medium size. Free donation to any student who commutes by bicycle.",
            location = "Athurugiriya",
            imageRes = R.drawable.img_mkt_textbooks,
            isAvailable = true,
            sellerName = "Nadeesha Silva",
            sellerAvatarRes = R.drawable.img_avatar_woman,
            memberSince = "Member since 2026"
        ),
        MarketplaceListing(
            id = "blender_7",
            ownerId = "user_kamal",
            listingType = "FOR SALE",
            title = "Food Blender & Grinder",
            price = "Rs. 6,000",
            category = "Electronics",
            condition = "Good",
            description = "Panasonic blender with dry mill attachment. Ideal for daily cooking needs.",
            location = "Malabe",
            imageRes = R.drawable.img_mkt_ricecooker,
            isAvailable = true,
            sellerName = "Kamal Fernando",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026"
        ),
        MarketplaceListing(
            id = "pots_8",
            ownerId = "user_saman",
            listingType = "GIVE AWAY",
            title = "Ceramic Plant Pots Set",
            price = "FREE",
            category = "Home",
            condition = "Good",
            description = "Set of 4 terracotta ceramic pots suitable for indoor succulents and herbs. Free for pickup.",
            location = "Kaduwela",
            imageRes = R.drawable.img_mkt_desk,
            isAvailable = true,
            sellerName = "Saman Kumara",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026"
        )
    )

    private val listings = mutableListOf<MarketplaceListing>().apply {
        addAll(initialSeed)
    }

    fun init(context: Context) {
        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        scope.launch {
            val dao = database?.listingDao() ?: return@launch
            val count = dao.count()
            if (count == 0) {
                // Seed initial listings into Room
                val entities = initialSeed.map { ListingEntity.fromModel(it) }
                dao.insertAll(entities)
            } else {
                // Load persisted listings from Room into memory cache
                val persisted = dao.getAll().map { it.toModel() }
                withContext(Dispatchers.Main) {
                    listings.clear()
                    listings.addAll(persisted)
                }
            }
        }
    }

    fun getAll(): List<MarketplaceListing> {
        return listings.toList()
    }

    fun getById(id: String): MarketplaceListing? {
        return listings.find { it.id == id }
    }

    fun add(listing: MarketplaceListing) {
        // Prepend so newly published listings appear first in marketplace
        listings.add(0, listing)
        database?.let { db ->
            scope.launch {
                db.listingDao().insert(ListingEntity.fromModel(listing))
            }
        }
    }

    fun update(listing: MarketplaceListing) {
        val index = listings.indexOfFirst { it.id == listing.id }
        if (index != -1) {
            listings[index] = listing
        }
        database?.let { db ->
            scope.launch {
                db.listingDao().update(ListingEntity.fromModel(listing))
            }
        }
    }

    fun delete(id: String) {
        listings.removeAll { it.id == id }
        database?.let { db ->
            scope.launch {
                db.listingDao().deleteById(id)
            }
        }
    }

    /**
     * Converts current listings into MarketProduct models for MarketplaceActivity grid.
     */
    fun asMarketProducts(): List<MarketProduct> {
        return listings.map { item ->
            MarketProduct(
                id = item.id,
                title = item.title,
                price = item.price,
                condition = "${item.condition} Condition",
                location = item.location,
                imageRes = item.imageRes,
                imageUri = item.imageUri,
                isGiveaway = item.listingType.equals("GIVE AWAY", ignoreCase = true) || item.price.equals("FREE", ignoreCase = true),
                isFavorite = false
            )
        }
    }

    /**
     * Search and filter listings combined.
     */
    fun searchAndFilter(query: String, filterChip: String): List<MarketProduct> {
        val trimmedQuery = query.trim()
        val filtered = listings.filter { item ->
            // Filter chip matching
            val matchesFilter = when (filterChip.lowercase()) {
                "for sale" -> item.listingType.equals("FOR SALE", ignoreCase = true) || item.listingType.equals("For Sale", ignoreCase = true)
                "free" -> item.listingType.equals("GIVE AWAY", ignoreCase = true) || item.price.equals("FREE", ignoreCase = true)
                "donation" -> item.listingType.contains("DONAT", ignoreCase = true)
                else -> true // "All" or any other
            }

            // Query matching
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                item.title.contains(trimmedQuery, ignoreCase = true) ||
                item.category.contains(trimmedQuery, ignoreCase = true) ||
                item.description.contains(trimmedQuery, ignoreCase = true) ||
                item.location.contains(trimmedQuery, ignoreCase = true) ||
                item.listingType.contains(trimmedQuery, ignoreCase = true)
            }

            matchesFilter && matchesQuery
        }

        return filtered.map { item ->
            MarketProduct(
                id = item.id,
                title = item.title,
                price = item.price,
                condition = "${item.condition} Condition",
                location = item.location,
                imageRes = item.imageRes,
                imageUri = item.imageUri,
                isGiveaway = item.listingType.equals("GIVE AWAY", ignoreCase = true) || item.price.equals("FREE", ignoreCase = true),
                isFavorite = false
            )
        }
    }

    /**
     * Retrieves user posts for My Posts section in Activity tab.
     */
    fun getUserPosts(ownerId: String = "user_nimal"): List<MarketplaceListing> {
        return listings.filter { it.ownerId == ownerId }
    }
}
