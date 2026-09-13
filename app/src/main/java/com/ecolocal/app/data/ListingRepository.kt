package com.ecolocal.app.data

import android.content.Context
import android.util.Log
import com.ecolocal.app.R
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.MarketProduct
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.model.entity.ListingEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Singleton repository acting as the single source of truth for Marketplace listings
 * backed by Cloud Firestore with local Room fallback cache.
 */
object ListingRepository {

    private const val TAG = "ListingRepository"
    private const val COLLECTION_LISTINGS = "listings"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var snapshotListener: ListenerRegistration? = null
    private val dataChangeListeners = mutableListOf<() -> Unit>()

    private val initialSeed = listOf(
        MarketplaceListing(
            id = "seed_mkt_1",
            ownerId = "user_nadeesha",
            listingType = "FOR SALE",
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
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_2",
            ownerId = "user_kamal",
            listingType = "GIVE AWAY",
            title = "Textbooks Bundle (O/L & A/L)",
            price = "FREE",
            category = "Books",
            condition = "Used",
            description = "Giving away a collection of O/L and A/L textbooks and reference books. All in usable condition. Pickup from Kaduwela.",
            location = "Kaduwela",
            imageRes = R.drawable.img_mkt_textbooks,
            isAvailable = true,
            sellerName = "Kamal Fernando",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_3",
            ownerId = "user_saman",
            listingType = "FOR SALE",
            title = "1.8L Electric Rice Cooker",
            price = "Rs. 4,500",
            category = "Electronics",
            condition = "Good",
            description = "Reliable 1.8L rice cooker, lightly used for 6 months. Works perfectly with original measuring cup and steam tray.",
            location = "Battaramulla",
            imageRes = R.drawable.img_mkt_ricecooker,
            isAvailable = true,
            sellerName = "Saman Kumara",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_4",
            ownerId = "user_sunil",
            listingType = "FOR SALE",
            title = "High-Speed Ceiling Fan 56-inch",
            price = "Rs. 5,200",
            category = "Home",
            condition = "Like New",
            description = "High-speed 3-blade ceiling fan with regulator. Installed for 2 months only. Perfect condition.",
            location = "Rajagiriya",
            imageRes = R.drawable.img_mkt_desk,
            isAvailable = true,
            sellerName = "Sunil Wickrama",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_5",
            ownerId = "user_priya",
            listingType = "FOR SALE",
            title = "Vintage Teak Coffee Table",
            price = "Rs. 9,000",
            category = "Furniture",
            condition = "Good",
            description = "Handcrafted solid teak coffee table with glass top. Minor wear, excellent vintage character. Pickup in Galle.",
            location = "Galle",
            imageRes = R.drawable.img_mkt_desk,
            isAvailable = true,
            sellerName = "Priya Jayasuriya",
            sellerAvatarRes = R.drawable.img_avatar_woman,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_6",
            ownerId = "user_sanduni",
            listingType = "DONATION",
            title = "School Uniforms & Backpack Bundle",
            price = "FREE",
            category = "Clothing",
            condition = "Good",
            description = "Set of school white shirts and backpacks in clean condition for young students in Matara district.",
            location = "Matara",
            imageRes = R.drawable.img_mkt_textbooks,
            isAvailable = true,
            sellerName = "Sanduni Alwis",
            sellerAvatarRes = R.drawable.img_avatar_woman,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_7",
            ownerId = "user_ranjith",
            listingType = "DONATION",
            title = "Traditional Clay Water Pots Set",
            price = "FREE",
            category = "Home",
            condition = "Good",
            description = "Set of natural terracotta clay water pots and gardening planters. Free for pickup in Anuradhapura.",
            location = "Anuradhapura",
            imageRes = R.drawable.img_mkt_desk,
            isAvailable = true,
            sellerName = "Ranjith Silva",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_8",
            ownerId = "user_nadeesha",
            listingType = "FOR SALE",
            title = "Stainless Steel Electric Kettle 1.7L",
            price = "Rs. 2,800",
            category = "Electronics",
            condition = "Good",
            description = "Stainless steel electric kettle with auto shut-off. Boiling fast and clean.",
            location = "Malabe",
            imageRes = R.drawable.img_mkt_ricecooker,
            isAvailable = true,
            sellerName = "Nadeesha Silva",
            sellerAvatarRes = R.drawable.img_avatar_woman,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_9",
            ownerId = "user_kamal",
            listingType = "DONATION",
            title = "Bicycle Safety Helmet",
            price = "FREE",
            category = "Clothing",
            condition = "Fair",
            description = "Lightweight bike helmet, medium size. Free donation to any student who commutes by bicycle.",
            location = "Kaduwela",
            imageRes = R.drawable.img_mkt_textbooks,
            isAvailable = true,
            sellerName = "Kamal Fernando",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        ),
        MarketplaceListing(
            id = "seed_mkt_10",
            ownerId = "user_saman",
            listingType = "FOR SALE",
            title = "Kitchen Food Blender & Grinder",
            price = "Rs. 6,000",
            category = "Electronics",
            condition = "Good",
            description = "Panasonic blender with dry mill attachment. Ideal for daily cooking needs.",
            location = "Battaramulla",
            imageRes = R.drawable.img_mkt_ricecooker,
            isAvailable = true,
            sellerName = "Saman Kumara",
            sellerAvatarRes = R.drawable.img_avatar_nimal,
            memberSince = "Member since 2026",
            status = "ACTIVE"
        )
    )

    private val listings = mutableListOf<MarketplaceListing>().apply {
        addAll(initialSeed)
    }

    fun init(context: Context) {
        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        // 1. First load local Room cache quickly
        scope.launch {
            val dao = database?.listingDao()
            if (dao != null) {
                val persisted = dao.getAll().map { it.toModel() }
                if (persisted.isNotEmpty()) {
                    synchronized(listings) {
                        listings.clear()
                        listings.addAll(persisted.filter { it.status != "DELETED" })
                    }
                    notifyListeners()
                }
            }
        }

        // 2. Start Firestore real-time listener as single source of truth
        startFirestoreListener()
        seedSampleDataIfEmpty()
    }

    private fun startFirestoreListener() {
        if (snapshotListener != null) return

        snapshotListener = firestore.collection(COLLECTION_LISTINGS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore listings listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                if (snapshot.isEmpty) {
                    // Seed initial data once if collection is empty
                    seedSampleDataIfEmpty()
                } else {
                    val remoteList = snapshot.documents.mapNotNull { doc ->
                        MarketplaceListing.fromDocument(doc)
                    }.filter { it.status != "DELETED" }
                        .sortedByDescending { it.createdAt }

                    synchronized(listings) {
                        listings.clear()
                        listings.addAll(remoteList)
                    }

                    notifyListeners()

                    // Sync to local Room cache
                    scope.launch {
                        try {
                            val dao = database?.listingDao()
                            if (dao != null) {
                                val entities = remoteList.map { ListingEntity.fromModel(it) }
                                dao.insertAll(entities)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed caching listings in Room", e)
                        }
                    }
                }
            }
    }

    /**
     * Seeds initial sample listings idempotently using stable document IDs.
     */
    fun seedSampleDataIfEmpty(forceSeedMissing: Boolean = false) {
        scope.launch {
            try {
                for (item in initialSeed) {
                    val docRef = firestore.collection(COLLECTION_LISTINGS).document(item.listingId)
                    if (forceSeedMissing) {
                        docRef.set(item.toMap(), SetOptions.merge())
                    } else {
                        docRef.get().addOnSuccessListener { snapshot ->
                            if (!snapshot.exists()) {
                                docRef.set(item.toMap())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding sample listings", e)
            }
        }
    }

    fun addChangeListener(listener: () -> Unit) {
        synchronized(dataChangeListeners) {
            if (!dataChangeListeners.contains(listener)) {
                dataChangeListeners.add(listener)
            }
        }
    }

    fun removeChangeListener(listener: () -> Unit) {
        synchronized(dataChangeListeners) {
            dataChangeListeners.remove(listener)
        }
    }

    private fun notifyListeners() {
        scope.launch(Dispatchers.Main) {
            val callbacks = synchronized(dataChangeListeners) { dataChangeListeners.toList() }
            callbacks.forEach { it.invoke() }
        }
    }

    fun getAll(): List<MarketplaceListing> {
        return synchronized(listings) {
            listings.filter { it.status != "DELETED" }.toList()
        }
    }

    fun getById(id: String): MarketplaceListing? {
        return synchronized(listings) {
            listings.find { it.listingId == id && it.status != "DELETED" }
        }
    }

    fun add(listing: MarketplaceListing, onComplete: ((Boolean) -> Unit)? = null) {
        // Prepend to local memory cache immediately for instant UI feedback
        synchronized(listings) {
            listings.removeAll { it.listingId == listing.listingId }
            listings.add(0, listing)
        }
        notifyListeners()

        // Write online to Firestore
        firestore.collection(COLLECTION_LISTINGS)
            .document(listing.listingId)
            .set(listing.toMap())
            .addOnSuccessListener {
                if (listing.ownerId.isNotBlank() && !listing.listingId.startsWith("seed_")) {
                    EcoPointsRepository.awardPoints(
                        userId = listing.ownerId,
                        actionType = "CREATE_LISTING",
                        referenceId = listing.listingId,
                        points = EcoPointsRepository.POINTS_CREATE_LISTING
                    )
                }
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed adding listing to Firestore", e)
                onComplete?.invoke(false)
            }

        // Cache in Room
        database?.let { db ->
            scope.launch {
                try {
                    db.listingDao().insert(ListingEntity.fromModel(listing))
                } catch (e: Exception) {
                    Log.e(TAG, "Error caching new listing in Room", e)
                }
            }
        }
    }

    fun update(listing: MarketplaceListing, onComplete: ((Boolean) -> Unit)? = null) {
        val updatedWithTimestamp = listing.copy(updatedAt = System.currentTimeMillis())

        synchronized(listings) {
            val index = listings.indexOfFirst { it.listingId == updatedWithTimestamp.listingId }
            if (index != -1) {
                listings[index] = updatedWithTimestamp
            }
        }
        notifyListeners()

        // Write online to Firestore
        firestore.collection(COLLECTION_LISTINGS)
            .document(updatedWithTimestamp.listingId)
            .set(updatedWithTimestamp.toMap(), SetOptions.merge())
            .addOnSuccessListener {
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed updating listing in Firestore", e)
                onComplete?.invoke(false)
            }

        database?.let { db ->
            scope.launch {
                try {
                    db.listingDao().update(ListingEntity.fromModel(updatedWithTimestamp))
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating listing in Room", e)
                }
            }
        }
    }

    fun delete(id: String, onComplete: ((Boolean) -> Unit)? = null) {
        // Remove locally from memory cache
        synchronized(listings) {
            listings.removeAll { it.listingId == id }
        }
        notifyListeners()

        // Online safe delete in Firestore: mark status = DELETED
        firestore.collection(COLLECTION_LISTINGS)
            .document(id)
            .update(
                mapOf(
                    "status" to "DELETED",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed soft-deleting listing in Firestore", e)
                onComplete?.invoke(false)
            }

        database?.let { db ->
            scope.launch {
                try {
                    db.listingDao().deleteById(id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting listing from Room", e)
                }
            }
        }
    }

    /**
     * Converts current active listings into MarketProduct models for MarketplaceActivity grid.
     */
    fun asMarketProducts(): List<MarketProduct> {
        return synchronized(listings) {
            listings.filter { it.status != "DELETED" }.map { item ->
                MarketProduct(
                    id = item.listingId,
                    title = item.title,
                    price = item.price,
                    condition = "${item.condition} Condition",
                    location = item.locationName,
                    imageRes = item.imageRes,
                    imageUri = item.imageUri,
                    isGiveaway = item.listingType.equals("GIVE AWAY", ignoreCase = true) || item.price.equals("FREE", ignoreCase = true),
                    isFavorite = false
                )
            }
        }
    }

    /**
     * Search and filter listings combined.
     */
    fun searchAndFilter(query: String, filterChip: String): List<MarketProduct> {
        val trimmedQuery = query.trim()
        val currentActive = synchronized(listings) {
            listings.filter { it.status != "DELETED" }
        }

        val filtered = currentActive.filter { item ->
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
                    item.locationName.contains(trimmedQuery, ignoreCase = true) ||
                    item.listingType.contains(trimmedQuery, ignoreCase = true)
            }

            matchesFilter && matchesQuery
        }

        return filtered.map { item ->
            MarketProduct(
                id = item.listingId,
                title = item.title,
                price = item.price,
                condition = "${item.condition} Condition",
                location = item.locationName,
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
    fun getUserPosts(ownerId: String): List<MarketplaceListing> {
        return synchronized(listings) {
            listings.filter { it.ownerId == ownerId && it.status != "DELETED" }
        }
    }
}
