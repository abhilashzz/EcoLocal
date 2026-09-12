package com.ecolocal.app.data

import android.content.Context
import android.util.Log
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.model.entity.SavedListingEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Repository for bookmarked / saved listings, backed by Cloud Firestore collection "favorites"
 * with local Room persistence.
 */
object SavedRepository {

    private const val TAG = "SavedRepository"
    private const val COLLECTION_FAVORITES = "favorites"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var snapshotListener: ListenerRegistration? = null
    private val changeListeners = mutableListOf<() -> Unit>()

    private val savedListingIds = mutableSetOf<String>()

    fun init(context: Context) {
        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        // Load Room cache first
        scope.launch {
            val dao = database?.savedDao()
            if (dao != null) {
                val list = dao.getAll()
                synchronized(savedListingIds) {
                    savedListingIds.clear()
                    savedListingIds.addAll(list.map { it.listingId })
                }
                notifyListeners()
            }
        }

        startFirestoreListener()
    }

    fun startFirestoreListener() {
        snapshotListener?.remove()

        val currentUser = auth.currentUser
        val currentUid = currentUser?.uid ?: ""
        if (currentUid.isEmpty()) return

        snapshotListener = firestore.collection(COLLECTION_FAVORITES)
            .whereEqualTo("userId", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore favorites listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val remoteIds = snapshot.documents.mapNotNull { it.getString("listingId") }

                synchronized(savedListingIds) {
                    savedListingIds.clear()
                    savedListingIds.addAll(remoteIds)
                }

                notifyListeners()

                // Sync to Room
                scope.launch {
                    try {
                        val dao = database?.savedDao()
                        if (dao != null) {
                            remoteIds.forEach { id ->
                                dao.insert(SavedListingEntity(listingId = id))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing saved listings to Room", e)
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

    fun isSaved(listingId: String): Boolean {
        return synchronized(savedListingIds) {
            savedListingIds.contains(listingId)
        }
    }

    /**
     * Toggles bookmark state online in Firestore favorites and offline in Room.
     * Returns true if now saved, false if removed.
     */
    fun toggleSave(listingId: String): Boolean {
        val currentUid = auth.currentUser?.uid ?: "local_user"
        val favoriteDocId = "${currentUid}_$listingId"
        val currentlySaved = isSaved(listingId)
        val nowSaved = !currentlySaved

        if (nowSaved) {
            synchronized(savedListingIds) {
                savedListingIds.add(listingId)
            }
            notifyListeners()

            val favData = mapOf(
                "favoriteId" to favoriteDocId,
                "userId" to currentUid,
                "listingId" to listingId,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection(COLLECTION_FAVORITES)
                .document(favoriteDocId)
                .set(favData)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed writing favorite to Firestore", e)
                }

            database?.let { db ->
                scope.launch {
                    try {
                        db.savedDao().insert(SavedListingEntity(listingId = listingId))
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed inserting saved into Room", e)
                    }
                }
            }
        } else {
            synchronized(savedListingIds) {
                savedListingIds.remove(listingId)
            }
            notifyListeners()

            firestore.collection(COLLECTION_FAVORITES)
                .document(favoriteDocId)
                .delete()
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed deleting favorite from Firestore", e)
                }

            database?.let { db ->
                scope.launch {
                    try {
                        db.savedDao().delete(listingId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed deleting saved from Room", e)
                    }
                }
            }
        }

        return nowSaved
    }

    fun getSavedListings(): List<MarketplaceListing> {
        val all = ListingRepository.getAll()
        val currentSaved = synchronized(savedListingIds) { savedListingIds.toSet() }
        return all.filter { currentSaved.contains(it.listingId) && it.status != "DELETED" }
    }
}
