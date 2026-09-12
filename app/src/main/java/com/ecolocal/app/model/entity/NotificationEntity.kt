package com.ecolocal.app.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "SYSTEM", // "MARKETPLACE", "COMMUNITY", "SYSTEM", "CHAT", "SAVED"
    val targetListingId: String? = null,
    val targetConversationId: String? = null
)
