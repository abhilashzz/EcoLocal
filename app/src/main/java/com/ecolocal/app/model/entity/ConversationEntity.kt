package com.ecolocal.app.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val conversationId: String,
    val participantName: String,
    val participantAvatarRes: Int = 0,
    val participantRole: String = "Community Member",
    val listingId: String? = null,
    val listingTitle: String? = null,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)
