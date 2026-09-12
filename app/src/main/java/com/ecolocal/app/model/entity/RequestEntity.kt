package com.ecolocal.app.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "requests")
data class RequestEntity(
    @PrimaryKey
    val requestId: String = "",
    val listingId: String = "",
    val listingOwnerId: String = "",
    val requesterId: String = "",
    val requesterName: String = "Community Member",
    val listingTitle: String = "",
    val listingImageRes: Int = 0,
    val listingImageUri: String? = null,
    val ownerName: String = "",
    val requestType: String = "MARKETPLACE_INTEREST", // "MARKETPLACE_INTEREST" or "SERVICE_HELP"
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "COMPLETED", "CANCELLED"
    val location: String = "",
    val priceOrInfo: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "requestId" to requestId,
            "listingId" to listingId,
            "listingOwnerId" to listingOwnerId,
            "requesterId" to requesterId,
            "requesterName" to requesterName,
            "listingTitle" to listingTitle,
            "listingImageRes" to listingImageRes,
            "listingImageUri" to listingImageUri,
            "ownerName" to ownerName,
            "type" to requestType,
            "requestType" to requestType,
            "status" to status,
            "location" to location,
            "priceOrInfo" to priceOrInfo,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): RequestEntity? {
            if (!doc.exists()) return null
            return try {
                RequestEntity(
                    requestId = doc.getString("requestId") ?: doc.id,
                    listingId = doc.getString("listingId") ?: "",
                    listingOwnerId = doc.getString("listingOwnerId") ?: "",
                    requesterId = doc.getString("requesterId") ?: "",
                    requesterName = doc.getString("requesterName") ?: "Community Member",
                    listingTitle = doc.getString("listingTitle") ?: doc.getString("title") ?: "",
                    listingImageRes = doc.getLong("listingImageRes")?.toInt() ?: 0,
                    listingImageUri = doc.getString("listingImageUri"),
                    ownerName = doc.getString("ownerName") ?: "",
                    requestType = doc.getString("requestType") ?: doc.getString("type") ?: "MARKETPLACE_INTEREST",
                    status = doc.getString("status") ?: "PENDING",
                    location = doc.getString("location") ?: "",
                    priceOrInfo = doc.getString("priceOrInfo") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
