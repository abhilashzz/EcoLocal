package com.ecolocal.app.model

import androidx.annotation.DrawableRes
import com.ecolocal.app.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Cloud Firestore model for community services and help requests.
 */
@IgnoreExtraProperties
data class CommunityService(
    val serviceId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Other",
    val serviceType: String = "SERVICE_OFFER", // "SERVICE_OFFER", "HELP_REQUEST"
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val price: String = "", // e.g. "Rs. 1,500/session" or "FREE"
    val timeText: String = "", // e.g. "Needed this week"
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED"
    @DrawableRes val imageRes: Int = 0,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "serviceId" to serviceId,
            "ownerId" to ownerId,
            "ownerName" to ownerName,
            "title" to title,
            "description" to description,
            "category" to category,
            "serviceType" to serviceType,
            "locationName" to locationName,
            "latitude" to latitude,
            "longitude" to longitude,
            "price" to price,
            "timeText" to timeText,
            "status" to status,
            "imageRes" to imageRes,
            "imageUrl" to imageUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    fun toUiItem(): CommunityServiceItem {
        val fallbackImage = when (category.lowercase()) {
            "tutoring" -> R.drawable.img_service_tutor
            "gardening" -> R.drawable.img_service_lawn
            "repairs" -> R.drawable.img_service_lawn
            else -> R.drawable.img_service_tutor
        }
        val finalImage = if (imageRes != 0) imageRes else fallbackImage

        return if (serviceType.equals("HELP_REQUEST", ignoreCase = true) ||
            serviceType.equals("REQUEST", ignoreCase = true)
        ) {
            CommunityServiceItem.Request(
                id = serviceId,
                title = title,
                location = locationName,
                timeText = if (timeText.isNotEmpty()) timeText else "Flexible",
                requesterName = if (ownerName.isNotEmpty()) ownerName else "Community Member",
                category = category,
                imageRes = finalImage
            )
        } else {
            CommunityServiceItem.Offer(
                id = serviceId,
                title = title,
                location = locationName,
                price = if (price.isNotEmpty()) price else "FREE",
                status = if (status == "ACTIVE") "Available" else status,
                imageRes = finalImage,
                providerName = if (ownerName.isNotEmpty()) ownerName else "Community Member",
                category = category
            )
        }
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): CommunityService? {
            if (!doc.exists()) return null
            return try {
                CommunityService(
                    serviceId = doc.getString("serviceId") ?: doc.id,
                    ownerId = doc.getString("ownerId") ?: "",
                    ownerName = doc.getString("ownerName") ?: "Community Member",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    category = doc.getString("category") ?: "Other",
                    serviceType = doc.getString("serviceType") ?: "SERVICE_OFFER",
                    locationName = doc.getString("locationName") ?: "",
                    latitude = doc.getDouble("latitude"),
                    longitude = doc.getDouble("longitude"),
                    price = doc.getString("price") ?: "",
                    timeText = doc.getString("timeText") ?: "",
                    status = doc.getString("status") ?: "ACTIVE",
                    imageRes = doc.getLong("imageRes")?.toInt() ?: 0,
                    imageUrl = doc.getString("imageUrl"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
