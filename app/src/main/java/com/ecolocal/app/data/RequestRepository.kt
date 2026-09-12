package com.ecolocal.app.data

import android.content.Context
import com.ecolocal.app.R
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.entity.RequestEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository managing user requests (Marketplace interest & Service help offers).
 */
object RequestRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val initialRequests = listOf(
        RequestEntity(
            requestId = "req_initial_1",
            listingId = "books_2",
            listingTitle = "Textbooks Bundle",
            listingImageRes = R.drawable.img_mkt_textbooks,
            ownerName = "Kamal Fernando",
            requesterName = "Nimal Perera",
            requestType = "MARKETPLACE_INTEREST",
            status = "PENDING",
            location = "Kaduwela",
            priceOrInfo = "FREE",
            createdAt = System.currentTimeMillis() - 3600000L
        )
    )

    private val requests = mutableListOf<RequestEntity>().apply {
        addAll(initialRequests)
    }

    fun init(context: Context) {
        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        scope.launch {
            val dao = database?.requestDao() ?: return@launch
            val count = dao.count()
            if (count == 0) {
                dao.insert(initialRequests[0])
            } else {
                val persisted = dao.getAll()
                withContext(Dispatchers.Main) {
                    requests.clear()
                    requests.addAll(persisted)
                }
            }
        }
    }

    fun getAll(): List<RequestEntity> {
        return requests.toList()
    }

    fun hasRequest(listingId: String, requestType: String): Boolean {
        return requests.any { it.listingId == listingId && it.requestType == requestType }
    }

    fun createMarketplaceInterest(
        listingId: String,
        title: String,
        imageRes: Int,
        imageUri: String?,
        ownerName: String,
        location: String,
        price: String
    ): Boolean {
        if (hasRequest(listingId, "MARKETPLACE_INTEREST")) {
            return false
        }

        val newRequest = RequestEntity(
            requestId = "req_${UUID.randomUUID()}",
            listingId = listingId,
            listingTitle = title,
            listingImageRes = imageRes,
            listingImageUri = imageUri,
            ownerName = ownerName,
            requesterName = "Nimal Perera",
            requestType = "MARKETPLACE_INTEREST",
            status = "PENDING",
            location = location,
            priceOrInfo = price,
            createdAt = System.currentTimeMillis()
        )

        requests.add(0, newRequest)
        database?.let { db ->
            scope.launch {
                db.requestDao().insert(newRequest)
            }
        }
        return true
    }

    fun createServiceHelp(
        serviceId: String,
        serviceTitle: String,
        imageRes: Int,
        ownerName: String,
        location: String,
        category: String
    ): Boolean {
        if (hasRequest(serviceId, "SERVICE_HELP")) {
            return false
        }

        val newRequest = RequestEntity(
            requestId = "req_${UUID.randomUUID()}",
            listingId = serviceId,
            listingTitle = serviceTitle,
            listingImageRes = imageRes,
            listingImageUri = null,
            ownerName = ownerName,
            requesterName = "Nimal Perera",
            requestType = "SERVICE_HELP",
            status = "PENDING",
            location = location,
            priceOrInfo = category,
            createdAt = System.currentTimeMillis()
        )

        requests.add(0, newRequest)
        database?.let { db ->
            scope.launch {
                db.requestDao().insert(newRequest)
            }
        }
        return true
    }
}
