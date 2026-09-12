package com.ecolocal.app.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "requests")
data class RequestEntity(
    @PrimaryKey
    val requestId: String,
    val listingId: String,
    val listingTitle: String,
    val listingImageRes: Int = 0,
    val listingImageUri: String? = null,
    val ownerName: String,
    val requesterName: String = "Nimal Perera",
    val requestType: String, // "MARKETPLACE_INTEREST" or "SERVICE_HELP"
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "DECLINED", "COMPLETED"
    val location: String = "Malabe",
    val priceOrInfo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
