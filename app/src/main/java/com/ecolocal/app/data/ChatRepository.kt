package com.ecolocal.app.data

import android.content.Context
import com.ecolocal.app.R
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.entity.ConversationEntity
import com.ecolocal.app.model.entity.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository managing local 1-on-1 chats and messaging.
 */
object ChatRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val initialConversations = listOf(
        ConversationEntity(
            conversationId = "conv_kamal",
            participantName = "Kamal Fernando",
            participantAvatarRes = R.drawable.img_avatar_nimal,
            participantRole = "Community Member",
            listingId = "books_2",
            listingTitle = "Textbooks Bundle",
            lastMessage = "Sure, let me know when you're nearby!",
            lastMessageTimestamp = System.currentTimeMillis() - 1800000L,
            unreadCount = 1
        ),
        ConversationEntity(
            conversationId = "conv_nadeesha",
            participantName = "Nadeesha Silva",
            participantAvatarRes = R.drawable.img_avatar_woman,
            participantRole = "Neighbor / Seller",
            listingId = "desk_1",
            listingTitle = "Wooden Study Desk",
            lastMessage = "Great! Is the price negotiable?",
            lastMessageTimestamp = System.currentTimeMillis() - 7200000L,
            unreadCount = 0
        )
    )

    private val initialMessages = mapOf(
        "conv_kamal" to listOf(
            MessageEntity(
                messageId = "msg_k1",
                conversationId = "conv_kamal",
                senderId = "kamal",
                senderName = "Kamal Fernando",
                text = "Hi Nimal! Are you interested in the textbooks?",
                timestamp = System.currentTimeMillis() - 3600000L,
                isFromMe = false
            ),
            MessageEntity(
                messageId = "msg_k2",
                conversationId = "conv_kamal",
                senderId = "current_user",
                senderName = "Nimal Perera",
                text = "Yes, can I pick them up tomorrow around 4 PM?",
                timestamp = System.currentTimeMillis() - 2700000L,
                isFromMe = true
            ),
            MessageEntity(
                messageId = "msg_k3",
                conversationId = "conv_kamal",
                senderId = "kamal",
                senderName = "Kamal Fernando",
                text = "Sure, let me know when you're nearby!",
                timestamp = System.currentTimeMillis() - 1800000L,
                isFromMe = false
            )
        ),
        "conv_nadeesha" to listOf(
            MessageEntity(
                messageId = "msg_n1",
                conversationId = "conv_nadeesha",
                senderId = "nadeesha",
                senderName = "Nadeesha Silva",
                text = "Hello! The wooden study desk is still available.",
                timestamp = System.currentTimeMillis() - 9000000L,
                isFromMe = false
            ),
            MessageEntity(
                messageId = "msg_n2",
                conversationId = "conv_nadeesha",
                senderId = "current_user",
                senderName = "Nimal Perera",
                text = "Great! Is the price negotiable?",
                timestamp = System.currentTimeMillis() - 7200000L,
                isFromMe = true
            )
        )
    )

    private val conversations = mutableListOf<ConversationEntity>().apply {
        addAll(initialConversations)
    }

    private val messages = mutableMapOf<String, MutableList<MessageEntity>>().apply {
        initialMessages.forEach { (convId, list) ->
            put(convId, list.toMutableList())
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        scope.launch {
            val convDao = database?.conversationDao() ?: return@launch
            val msgDao = database?.messageDao() ?: return@launch

            val count = convDao.count()
            if (count == 0) {
                initialConversations.forEach { convDao.insert(it) }
                initialMessages.values.forEach { msgDao.insertAll(it) }
            } else {
                val persistedConvs = convDao.getAll()
                withContext(Dispatchers.Main) {
                    conversations.clear()
                    conversations.addAll(persistedConvs)
                }

                persistedConvs.forEach { c ->
                    val persistedMsgs = msgDao.getMessagesForConversation(c.conversationId)
                    withContext(Dispatchers.Main) {
                        messages[c.conversationId] = persistedMsgs.toMutableList()
                    }
                }
            }
        }
    }

    fun getAllConversations(): List<ConversationEntity> {
        return conversations.sortedByDescending { it.lastMessageTimestamp }
    }

    fun getConversationById(id: String): ConversationEntity? {
        return conversations.find { it.conversationId == id }
    }

    fun getOrCreateConversation(
        participantName: String,
        participantAvatarRes: Int,
        participantRole: String = "Community Member",
        listingId: String? = null,
        listingTitle: String? = null
    ): ConversationEntity {
        val existing = conversations.find {
            it.participantName.equals(participantName, ignoreCase = true) ||
            (listingId != null && it.listingId == listingId)
        }

        if (existing != null) {
            return existing
        }

        val newId = "conv_${UUID.randomUUID()}"
        val newConv = ConversationEntity(
            conversationId = newId,
            participantName = participantName,
            participantAvatarRes = if (participantAvatarRes != 0) participantAvatarRes else R.drawable.img_avatar_nimal,
            participantRole = participantRole,
            listingId = listingId,
            listingTitle = listingTitle,
            lastMessage = "Started a conversation",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )

        conversations.add(0, newConv)
        messages[newId] = mutableListOf()

        database?.let { db ->
            scope.launch {
                db.conversationDao().insert(newConv)
            }
        }

        return newConv
    }

    fun getMessages(conversationId: String): List<MessageEntity> {
        return messages[conversationId]?.toList() ?: emptyList()
    }

    fun sendMessage(
        conversationId: String,
        text: String,
        isFromMe: Boolean = true
    ): MessageEntity {
        val newMsg = MessageEntity(
            messageId = "msg_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = if (isFromMe) "current_user" else "other_user",
            senderName = if (isFromMe) "Nimal Perera" else "Neighbor",
            text = text,
            timestamp = System.currentTimeMillis(),
            isFromMe = isFromMe
        )

        val list = messages.getOrPut(conversationId) { mutableListOf() }
        list.add(newMsg)

        // Update conversation lastMessage & timestamp
        val convIndex = conversations.indexOfFirst { it.conversationId == conversationId }
        if (convIndex != -1) {
            val conv = conversations[convIndex]
            val updatedConv = conv.copy(
                lastMessage = text,
                lastMessageTimestamp = newMsg.timestamp,
                unreadCount = if (isFromMe) conv.unreadCount else conv.unreadCount + 1
            )
            conversations.removeAt(convIndex)
            conversations.add(0, updatedConv)
        }

        database?.let { db ->
            scope.launch {
                db.messageDao().insert(newMsg)
                db.conversationDao().updateLastMessage(conversationId, text, newMsg.timestamp)
            }
        }

        return newMsg
    }

    fun markConversationAsRead(conversationId: String) {
        val convIndex = conversations.indexOfFirst { it.conversationId == conversationId }
        if (convIndex != -1) {
            conversations[convIndex] = conversations[convIndex].copy(unreadCount = 0)
        }
        database?.let { db ->
            scope.launch {
                db.conversationDao().markAsRead(conversationId)
            }
        }
    }
}
