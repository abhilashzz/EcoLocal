package com.ecolocal.app.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_listings")
data class SavedListingEntity(
    @PrimaryKey
    val listingId: String,
    val savedAt: Long = System.currentTimeMillis()
)
