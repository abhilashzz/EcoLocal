package com.ecolocal.app.data

import android.content.Context
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.MarketplaceListing
import com.ecolocal.app.model.entity.SavedListingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository for bookmarked / saved listings, backed by Room database.
 */
object SavedRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false
    private val savedListingIds = mutableSetOf<String>()

    fun init(context: Context) {
        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        scope.launch {
            val dao = database?.savedDao() ?: return@launch
            val list = dao.getAll()
            withContext(Dispatchers.Main) {
                savedListingIds.clear()
                savedListingIds.addAll(list.map { it.listingId })
            }
        }
    }

    fun isSaved(listingId: String): Boolean {
        return savedListingIds.contains(listingId)
    }

    /**
     * Toggles bookmark state. Returns true if now saved, false if removed.
     */
    fun toggleSave(listingId: String): Boolean {
        val currentlySaved = isSaved(listingId)
        val nowSaved = !currentlySaved

        if (nowSaved) {
            savedListingIds.add(listingId)
            database?.let { db ->
                scope.launch {
                    db.savedDao().insert(SavedListingEntity(listingId = listingId))
                }
            }
        } else {
            savedListingIds.remove(listingId)
            database?.let { db ->
                scope.launch {
                    db.savedDao().delete(listingId)
                }
            }
        }
        return nowSaved
    }

    fun getSavedListings(): List<MarketplaceListing> {
        val all = ListingRepository.getAll()
        return all.filter { savedListingIds.contains(it.id) }
    }
}
