package com.ecolocal.app.data

import android.content.Context
import android.util.Log
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.entity.RequestEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Repository managing user requests (Marketplace interest & Service help offers)
 * backed by Cloud Firestore as single source of truth.
 */
object RequestRepository {

    private const val TAG = "RequestRepository"
    private const val COLLECTION_REQUESTS = "requests"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var snapshotListener: ListenerRegistration? = null
    private val changeListeners = mutableListOf<() -> Unit>()

    private val requests = mutableListOf<RequestEntity>()

    fun init(context: Context) {
        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        // Load Room cache first
        scope.launch {
            val dao = database?.requestDao()
            if (dao != null) {
                val persisted = dao.getAll()
                if (persisted.isNotEmpty()) {
                    synchronized(requests) {
                        requests.clear()
                        requests.addAll(persisted)
                    }
                    notifyListeners()
                }
            }
        }

        startFirestoreListener()
    }

    fun startFirestoreListener() {
        snapshotListener?.remove()

        val currentUser = auth.currentUser
        val currentUid = currentUser?.uid ?: ""

        val query = if (currentUid.isNotEmpty()) {
            firestore.collection(COLLECTION_REQUESTS)
                .whereEqualTo("requesterId", currentUid)
        } else {
            firestore.collection(COLLECTION_REQUESTS)
        }

        snapshotListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Firestore requests listener error: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot == null) return@addSnapshotListener

            val remoteRequests = snapshot.documents.mapNotNull { doc ->
                RequestEntity.fromDocument(doc)
            }.sortedByDescending { it.createdAt }

            synchronized(requests) {
                requests.clear()
                requests.addAll(remoteRequests)
            }

            notifyListeners()

            // Update Room
            scope.launch {
                try {
                    val dao = database?.requestDao()
                    if (dao != null) {
                        dao.insertAll(remoteRequests)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error caching requests in Room", e)
                }
            }
        }
    }

    fun addChangeListener(listener: () -> Unit) {
        synchronized(changeListeners) {
            if (!changeListeners.contains(listener)) {
                changeListeners.add(listener)
            }
        }
    }

    fun removeChangeListener(listener: () -> Unit) {
        synchronized(changeListeners) {
            changeListeners.remove(listener)
        }
    }

    private fun notifyListeners() {
        scope.launch(Dispatchers.Main) {
            val callbacks = synchronized(changeListeners) { changeListeners.toList() }
            callbacks.forEach { it.invoke() }
        }
    }

    fun getAll(): List<RequestEntity> {
        return synchronized(requests) { requests.toList() }
    }

    fun hasRequest(listingId: String, requestType: String): Boolean {
        val currentUid = auth.currentUser?.uid
        return synchronized(requests) {
            requests.any {
                it.listingId == listingId &&
                    it.requestType == requestType &&
                    (currentUid == null || it.requesterId == currentUid || it.requesterId.isEmpty()) &&
                    it.status != "CANCELLED"
            }
        }
    }

    fun createMarketplaceInterest(
        listingId: String,
        title: String,
        imageRes: Int,
        imageUri: String?,
        ownerName: String,
        ownerId: String = "",
        location: String,
        price: String
    ): Boolean {
        if (hasRequest(listingId, "MARKETPLACE_INTEREST")) {
            return false
        }

        val user = auth.currentUser
        val requesterId = user?.uid ?: "anonymous"
        val requesterName = UserRepository.getCurrentUser()?.fullName
            ?: user?.displayName
            ?: "EcoLocal User"

        val reqId = "req_${UUID.randomUUID()}"
        val newRequest = RequestEntity(
            requestId = reqId,
            listingId = listingId,
            listingOwnerId = ownerId,
            requesterId = requesterId,
            requesterName = requesterName,
            listingTitle = title,
            listingImageRes = imageRes,
            listingImageUri = imageUri,
            ownerName = ownerName,
            requestType = "MARKETPLACE_INTEREST",
            status = "PENDING",
            location = location,
            priceOrInfo = price,
            createdAt = System.currentTimeMillis()
        )

        synchronized(requests) {
            requests.add(0, newRequest)
        }
        notifyListeners()

        firestore.collection(COLLECTION_REQUESTS)
            .document(reqId)
            .set(newRequest.toMap())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed writing request to Firestore", e)
            }

        database?.let { db ->
            scope.launch {
                try {
                    db.requestDao().insert(newRequest)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting request in Room", e)
                }
            }
        }

        return true
    }

    fun createServiceHelp(
        serviceId: String,
        serviceTitle: String,
        imageRes: Int,
        ownerName: String,
        ownerId: String = "",
        location: String,
        category: String
    ): Boolean {
        if (hasRequest(serviceId, "SERVICE_HELP")) {
            return false
        }

        val user = auth.currentUser
        val requesterId = user?.uid ?: "anonymous"
        val requesterName = UserRepository.getCurrentUser()?.fullName
            ?: user?.displayName
            ?: "EcoLocal User"

        val reqId = "req_${UUID.randomUUID()}"
        val newRequest = RequestEntity(
            requestId = reqId,
            listingId = serviceId,
            listingOwnerId = ownerId,
            requesterId = requesterId,
            requesterName = requesterName,
            listingTitle = serviceTitle,
            listingImageRes = imageRes,
            listingImageUri = null,
            ownerName = ownerName,
            requestType = "SERVICE_HELP",
            status = "PENDING",
            location = location,
            priceOrInfo = category,
            createdAt = System.currentTimeMillis()
        )

        synchronized(requests) {
            requests.add(0, newRequest)
        }
        notifyListeners()

        firestore.collection(COLLECTION_REQUESTS)
            .document(reqId)
            .set(newRequest.toMap())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed writing service request to Firestore", e)
            }

        database?.let { db ->
            scope.launch {
                try {
                    db.requestDao().insert(newRequest)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting service request in Room", e)
                }
            }
        }

        return true
    }
}
