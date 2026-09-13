package com.ecolocal.app.data

import android.content.Context
import android.util.Log
import com.ecolocal.app.R
import com.ecolocal.app.model.CommunityService
import com.ecolocal.app.model.CommunityServiceItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton repository acting as the online source of truth for Community Services & Help Requests
 * backed by Cloud Firestore.
 */
object ServiceRepository {

    private const val TAG = "ServiceRepository"
    private const val COLLECTION_SERVICES = "services"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var snapshotListener: ListenerRegistration? = null
    private val dataChangeListeners = mutableListOf<() -> Unit>()

    private val initialSeed = listOf(
        CommunityService(
            serviceId = "seed_srv_1",
            ownerId = "user_kamal",
            ownerName = "Kamal Fernando",
            title = "Science & Maths Tutor",
            description = "Experienced tutor offering primary and middle school coaching.",
            category = "Tutoring",
            serviceType = "SERVICE_OFFER",
            locationName = "Malabe",
            price = "Rs. 1,500/session",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_tutor
        ),
        CommunityService(
            serviceId = "seed_srv_2",
            ownerId = "user_saman",
            ownerName = "Saman Kumara",
            title = "Need help fixing a leaking tap",
            description = "Looking for a neighbor with plumbing tools to help with an indoor tap leak.",
            category = "Repairs",
            serviceType = "HELP_REQUEST",
            locationName = "Kaduwela",
            timeText = "Needed this week",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_lawn
        ),
        CommunityService(
            serviceId = "seed_srv_3",
            ownerId = "user_nadeesha",
            ownerName = "Nadeesha Silva",
            title = "Weekend Lawn Mowing Service",
            description = "Eco-friendly garden trimming and lawn mowing in suburban neighborhoods.",
            category = "Gardening",
            serviceType = "SERVICE_OFFER",
            locationName = "Rajagiriya",
            price = "Rs. 800/hr",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_lawn
        ),
        CommunityService(
            serviceId = "seed_srv_4",
            ownerId = "user_chamara",
            ownerName = "Chamara Perera",
            title = "Physics A/L revision guidance",
            description = "Need past paper walkthroughs and physics problem guidance.",
            category = "Tutoring",
            serviceType = "HELP_REQUEST",
            locationName = "Malabe",
            timeText = "Needed urgently",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_tutor
        ),
        CommunityService(
            serviceId = "seed_srv_5",
            ownerId = "user_sunil",
            ownerName = "Sunil Wickrama",
            title = "Electric Fan & Appliance Repairs",
            description = "Small appliance troubleshooting and rewiring service.",
            category = "Repairs",
            serviceType = "SERVICE_OFFER",
            locationName = "Battaramulla",
            price = "Rs. 1,000/job",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_tutor
        ),
        CommunityService(
            serviceId = "seed_srv_6",
            ownerId = "user_priya",
            ownerName = "Priya Jayasuriya",
            title = "Garden clearing & pruning help",
            description = "Need a helping hand to prune overgrown hedges and clear yard waste.",
            category = "Gardening",
            serviceType = "HELP_REQUEST",
            locationName = "Galle",
            timeText = "This Saturday",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_lawn
        ),
        CommunityService(
            serviceId = "seed_srv_7",
            ownerId = "user_sanduni",
            ownerName = "Sanduni Alwis",
            title = "English Conversational Coaching",
            description = "Free language practice for community students preparing for job interviews.",
            category = "Tutoring",
            serviceType = "SERVICE_OFFER",
            locationName = "Matara",
            price = "FREE",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_tutor
        ),
        CommunityService(
            serviceId = "seed_srv_8",
            ownerId = "user_ranjith",
            ownerName = "Ranjith Silva",
            title = "Ancient Grain & Home Seedling Exchange",
            description = "Offering free heirloom vegetable seedlings to local gardening hobbyists.",
            category = "Gardening",
            serviceType = "SERVICE_OFFER",
            locationName = "Anuradhapura",
            price = "FREE",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_lawn
        ),
        CommunityService(
            serviceId = "seed_srv_9",
            ownerId = "user_dinesh",
            ownerName = "Dinesh Bandara",
            title = "Bicycle brake repair assistance",
            description = "Seeking a local mechanic or cyclist for quick help aligning disc brakes.",
            category = "Repairs",
            serviceType = "HELP_REQUEST",
            locationName = "Kaduwela",
            timeText = "Flexible",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_lawn
        ),
        CommunityService(
            serviceId = "seed_srv_10",
            ownerId = "user_kasun",
            ownerName = "Kasun Fernando",
            title = "Elderly grocery delivery assistance",
            description = "Volunteer requested to assist picking up heavy groceries from local market.",
            category = "Volunteering",
            serviceType = "HELP_REQUEST",
            locationName = "Rajagiriya",
            timeText = "Tomorrow evening",
            status = "ACTIVE",
            imageRes = R.drawable.img_service_lawn
        )
    )

    private val services = mutableListOf<CommunityService>().apply {
        addAll(initialSeed)
    }

    fun init(context: Context) {
        startFirestoreListener()
        seedSampleDataIfEmpty()
    }

    private fun startFirestoreListener() {
        if (snapshotListener != null) return

        snapshotListener = firestore.collection(COLLECTION_SERVICES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore services listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                if (snapshot.isEmpty) {
                    seedSampleDataIfEmpty()
                } else {
                    val remoteList = snapshot.documents.mapNotNull { doc ->
                        CommunityService.fromDocument(doc)
                    }.filter { it.status == "ACTIVE" }
                        .sortedByDescending { it.createdAt }

                    synchronized(services) {
                        services.clear()
                        services.addAll(remoteList)
                    }

                    notifyListeners()
                }
            }
    }

    fun seedSampleDataIfEmpty() {
        scope.launch {
            try {
                for (item in initialSeed) {
                    val docRef = firestore.collection(COLLECTION_SERVICES).document(item.serviceId)
                    docRef.get().addOnSuccessListener { snap ->
                        if (!snap.exists()) {
                            docRef.set(item.toMap(), SetOptions.merge())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding sample services", e)
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

    fun getAll(): List<CommunityService> {
        return synchronized(services) { services.toList() }
    }

    fun getAllAsUiItems(): List<CommunityServiceItem> {
        return synchronized(services) {
            services.map { it.toUiItem() }
        }
    }

    fun addService(service: CommunityService, onComplete: ((Boolean) -> Unit)? = null) {
        synchronized(services) {
            services.removeAll { it.serviceId == service.serviceId }
            services.add(0, service)
        }
        notifyListeners()

        firestore.collection(COLLECTION_SERVICES)
            .document(service.serviceId)
            .set(service.toMap())
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create service in Firestore", e)
                onComplete?.invoke(false)
            }
    }

    fun searchAndFilter(
        query: String,
        category: String,
        tab: String // "SERVICE_OFFER" or "HELP_REQUEST"
    ): List<CommunityServiceItem> {
        val trimmedQuery = query.trim()
        val allServices = getAll()

        val filtered = allServices.filter { item ->
            // Tab filter
            val matchesTab = when (tab) {
                "SERVICE_OFFER" -> item.serviceType.equals("SERVICE_OFFER", ignoreCase = true)
                "HELP_REQUEST", "REQUESTS" -> item.serviceType.equals("HELP_REQUEST", ignoreCase = true) || item.serviceType.equals("REQUEST", ignoreCase = true)
                else -> true
            }

            // Category filter
            val matchesCategory = if (category.equals("All", ignoreCase = true)) {
                true
            } else {
                item.category.equals(category, ignoreCase = true)
            }

            // Search query filter: title, description, category, location, provider name
            val matchesSearch = if (trimmedQuery.isEmpty()) {
                true
            } else {
                item.title.contains(trimmedQuery, ignoreCase = true) ||
                    item.description.contains(trimmedQuery, ignoreCase = true) ||
                    item.locationName.contains(trimmedQuery, ignoreCase = true) ||
                    item.category.contains(trimmedQuery, ignoreCase = true) ||
                    item.providerName.contains(trimmedQuery, ignoreCase = true)
            }

            matchesTab && matchesCategory && matchesSearch
        }

        return filtered.map { it.toUiItem() }
    }
}
