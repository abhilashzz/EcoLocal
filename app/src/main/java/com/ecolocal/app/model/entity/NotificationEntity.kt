package com.ecolocal.app.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "SYSTEM", // "MARKETPLACE", "COMMUNITY", "SYSTEM", "CHAT", "SAVED"
    val targetListingId: String? = null,
    val targetConversationId: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "title" to title,
            "message" to message,
            "timestamp" to timestamp,
            "isRead" to isRead,
            "type" to type,
            "targetListingId" to targetListingId,
            "targetConversationId" to targetConversationId
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): NotificationEntity? {
            if (!doc.exists()) return null
            return try {
                NotificationEntity(
                    id = doc.getString("id") ?: doc.id,
                    userId = doc.getString("userId") ?: "",
                    title = doc.getString("title") ?: "",
                    message = doc.getString("message") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    isRead = doc.getBoolean("isRead") ?: false,
                    type = doc.getString("type") ?: "SYSTEM",
                    targetListingId = doc.getString("targetListingId"),
                    targetConversationId = doc.getString("targetConversationId")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
