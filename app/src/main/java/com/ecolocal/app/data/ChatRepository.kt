package com.ecolocal.app.data

import android.content.Context
import android.util.Log
import com.ecolocal.app.R
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.entity.ConversationEntity
import com.ecolocal.app.model.entity.MessageEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository managing 1-on-1 chats and messaging backed by Cloud Firestore
 * with real-time snapshot listeners and local Room persistence.
 */
object ChatRepository {

    private const val TAG = "ChatRepository"
    private const val COLLECTION_CONVERSATIONS = "conversations"
    private const val COLLECTION_MESSAGES = "messages"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var conversationsListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()
    private val activeMessageObservers = mutableMapOf<String, MutableList<() -> Unit>>()
    private val conversationObservers = mutableListOf<() -> Unit>()

    private val initialConversations = listOf(
        ConversationEntity(
            conversationId = "conv_kamal",
            participantName = "Kamal Fernando",
            participantAvatarRes = R.drawable.img_avatar_nimal,
            participantRole = "Community Member",
            listingId = "seed_mkt_2",
            listingTitle = "Textbooks Bundle (O/L & A/L)",
            lastMessage = "Sure, let me know when you're nearby!",
            lastMessageTimestamp = System.currentTimeMillis() - 1800000L,
            unreadCount = 1
        ),
        ConversationEntity(
            conversationId = "conv_nadeesha",
            participantName = "Nadeesha Silva",
            participantAvatarRes = R.drawable.img_avatar_woman,
            participantRole = "Neighbor / Seller",
            listingId = "seed_mkt_1",
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
                text = "Hi! Are you interested in the textbooks?",
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
            if (count > 0) {
                val persistedConvs = convDao.getAll()
                withContext(Dispatchers.Main) {
                    synchronized(conversations) {
                        conversations.clear()
                        conversations.addAll(persistedConvs)
                    }
                }

                persistedConvs.forEach { c ->
                    val persistedMsgs = msgDao.getMessagesForConversation(c.conversationId)
                    withContext(Dispatchers.Main) {
                        synchronized(messages) {
                            messages[c.conversationId] = persistedMsgs.toMutableList()
                        }
                    }
                }
            }
        }

        startConversationsListener()
    }

    private fun startConversationsListener() {
        conversationsListener?.remove()

        conversationsListener = firestore.collection(COLLECTION_CONVERSATIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore conversations listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                if (snapshot.isEmpty) {
                    seedConversationsIfEmpty()
                } else {
                    val remoteConvs = snapshot.documents.mapNotNull { doc ->
                        try {
                            ConversationEntity(
                                conversationId = doc.getString("conversationId") ?: doc.id,
                                participantName = doc.getString("participantName") ?: "Neighbor",
                                participantAvatarRes = doc.getLong("participantAvatarRes")?.toInt() ?: R.drawable.img_avatar_nimal,
                                participantRole = doc.getString("participantRole") ?: "Community Member",
                                listingId = doc.getString("listingId"),
                                listingTitle = doc.getString("listingTitle"),
                                lastMessage = doc.getString("lastMessage") ?: "",
                                lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: System.currentTimeMillis(),
                                unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.lastMessageTimestamp }

                    synchronized(conversations) {
                        conversations.clear()
                        conversations.addAll(remoteConvs)
                    }

                    notifyConversationObservers()

                    // Cache in Room
                    scope.launch {
                        try {
                            val convDao = database?.conversationDao()
                            if (convDao != null) {
                                remoteConvs.forEach { convDao.insert(it) }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error caching conversations in Room", e)
                        }
                    }
                }
            }
    }

    private fun seedConversationsIfEmpty() {
        scope.launch {
            try {
                for (conv in initialConversations) {
                    val convData = mapOf(
                        "conversationId" to conv.conversationId,
                        "participantName" to conv.participantName,
                        "participantAvatarRes" to conv.participantAvatarRes,
                        "participantRole" to conv.participantRole,
                        "listingId" to conv.listingId,
                        "listingTitle" to conv.listingTitle,
                        "lastMessage" to conv.lastMessage,
                        "lastMessageTimestamp" to conv.lastMessageTimestamp,
                        "unreadCount" to conv.unreadCount
                    )
                    firestore.collection(COLLECTION_CONVERSATIONS)
                        .document(conv.conversationId)
                        .set(convData, SetOptions.merge())

                    val msgs = initialMessages[conv.conversationId] ?: emptyList()
                    for (m in msgs) {
                        val mData = mapOf(
                            "messageId" to m.messageId,
                            "conversationId" to m.conversationId,
                            "senderId" to m.senderId,
                            "senderName" to m.senderName,
                            "text" to m.text,
                            "timestamp" to m.timestamp
                        )
                        firestore.collection(COLLECTION_CONVERSATIONS)
                            .document(conv.conversationId)
                            .collection(COLLECTION_MESSAGES)
                            .document(m.messageId)
                            .set(mData, SetOptions.merge())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding initial chats", e)
            }
        }
    }

    fun addConversationObserver(observer: () -> Unit) {
        synchronized(conversationObservers) {
            if (!conversationObservers.contains(observer)) {
                conversationObservers.add(observer)
            }
        }
    }

    fun removeConversationObserver(observer: () -> Unit) {
        synchronized(conversationObservers) {
            conversationObservers.remove(observer)
        }
    }

    private fun notifyConversationObservers() {
        scope.launch(Dispatchers.Main) {
            val list = synchronized(conversationObservers) { conversationObservers.toList() }
            list.forEach { it.invoke() }
        }
    }

    fun attachMessageListener(conversationId: String, onNewMessages: () -> Unit) {
        synchronized(activeMessageObservers) {
            val list = activeMessageObservers.getOrPut(conversationId) { mutableListOf() }
            if (!list.contains(onNewMessages)) {
                list.add(onNewMessages)
            }
        }

        if (!messageListeners.containsKey(conversationId)) {
            val listener = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Message listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    val currentUid = auth.currentUser?.uid ?: "current_user"
                    val remoteMsgs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val senderId = doc.getString("senderId") ?: ""
                            val isFromMe = (senderId == currentUid || senderId == "current_user")
                            MessageEntity(
                                messageId = doc.getString("messageId") ?: doc.id,
                                conversationId = conversationId,
                                senderId = senderId,
                                senderName = doc.getString("senderName") ?: "Neighbor",
                                text = doc.getString("text") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                isFromMe = isFromMe
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.timestamp }

                    if (remoteMsgs.isNotEmpty()) {
                        synchronized(messages) {
                            messages[conversationId] = remoteMsgs.toMutableList()
                        }
                    }

                    scope.launch(Dispatchers.Main) {
                        val observers = synchronized(activeMessageObservers) {
                            activeMessageObservers[conversationId]?.toList() ?: emptyList()
                        }
                        observers.forEach { it.invoke() }
                    }

                    // Cache in Room
                    scope.launch {
                        try {
                            val msgDao = database?.messageDao()
                            if (msgDao != null && remoteMsgs.isNotEmpty()) {
                                msgDao.insertAll(remoteMsgs)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error caching messages in Room", e)
                        }
                    }
                }

            messageListeners[conversationId] = listener
        }
    }

    fun detachMessageListener(conversationId: String, onNewMessages: () -> Unit) {
        synchronized(activeMessageObservers) {
            activeMessageObservers[conversationId]?.remove(onNewMessages)
            if (activeMessageObservers[conversationId].isNullOrEmpty()) {
                activeMessageObservers.remove(conversationId)
                messageListeners[conversationId]?.remove()
                messageListeners.remove(conversationId)
            }
        }
    }

    fun getAllConversations(): List<ConversationEntity> {
        return synchronized(conversations) {
            conversations.sortedByDescending { it.lastMessageTimestamp }
        }
    }

    fun getConversationById(id: String): ConversationEntity? {
        return synchronized(conversations) {
            conversations.find { it.conversationId == id }
        }
    }

    fun getOrCreateConversation(
        participantName: String,
        participantAvatarRes: Int,
        participantRole: String = "Community Member",
        listingId: String? = null,
        listingTitle: String? = null
    ): ConversationEntity {
        val existing = synchronized(conversations) {
            conversations.find {
                it.participantName.equals(participantName, ignoreCase = true) ||
                    (listingId != null && it.listingId == listingId)
            }
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

        synchronized(conversations) {
            conversations.add(0, newConv)
            messages[newId] = mutableListOf()
        }
        notifyConversationObservers()

        val convData = mapOf(
            "conversationId" to newConv.conversationId,
            "participantName" to newConv.participantName,
            "participantAvatarRes" to newConv.participantAvatarRes,
            "participantRole" to newConv.participantRole,
            "listingId" to newConv.listingId,
            "listingTitle" to newConv.listingTitle,
            "lastMessage" to newConv.lastMessage,
            "lastMessageTimestamp" to newConv.lastMessageTimestamp,
            "unreadCount" to newConv.unreadCount
        )

        firestore.collection(COLLECTION_CONVERSATIONS)
            .document(newId)
            .set(convData)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed creating conversation in Firestore", e)
            }

        database?.let { db ->
            scope.launch {
                try {
                    db.conversationDao().insert(newConv)
                } catch (e: Exception) {
                    Log.e(TAG, "Error caching conversation", e)
                }
            }
        }

        return newConv
    }

    fun getMessages(conversationId: String): List<MessageEntity> {
        return synchronized(messages) {
            messages[conversationId]?.toList() ?: emptyList()
        }
    }

    fun sendMessage(
        conversationId: String,
        text: String,
        isFromMe: Boolean = true
    ): MessageEntity {
        val currentUid = auth.currentUser?.uid ?: "current_user"
        val currentUserName = UserRepository.getCurrentUser()?.fullName
            ?: auth.currentUser?.displayName
            ?: "EcoLocal User"

        val msgId = "msg_${UUID.randomUUID()}"
        val timestamp = System.currentTimeMillis()

        val newMsg = MessageEntity(
            messageId = msgId,
            conversationId = conversationId,
            senderId = if (isFromMe) currentUid else "other_user",
            senderName = if (isFromMe) currentUserName else "Neighbor",
            text = text,
            timestamp = timestamp,
            isFromMe = isFromMe
        )

        synchronized(messages) {
            val list = messages.getOrPut(conversationId) { mutableListOf() }
            list.add(newMsg)
        }

        synchronized(conversations) {
            val convIndex = conversations.indexOfFirst { it.conversationId == conversationId }
            if (convIndex != -1) {
                val conv = conversations[convIndex]
                val updatedConv = conv.copy(
                    lastMessage = text,
                    lastMessageTimestamp = timestamp,
                    unreadCount = if (isFromMe) conv.unreadCount else conv.unreadCount + 1
                )
                conversations.removeAt(convIndex)
                conversations.add(0, updatedConv)
            }
        }
        notifyConversationObservers()

        // Write online to Firestore
        val msgData = mapOf(
            "messageId" to msgId,
            "conversationId" to conversationId,
            "senderId" to newMsg.senderId,
            "senderName" to newMsg.senderName,
            "text" to text,
            "timestamp" to timestamp
        )

        firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .document(msgId)
            .set(msgData)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed sending message to Firestore", e)
            }

        firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTimestamp" to timestamp
                )
            )

        database?.let { db ->
            scope.launch {
                try {
                    db.messageDao().insert(newMsg)
                    db.conversationDao().updateLastMessage(conversationId, text, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating message in Room", e)
                }
            }
        }

        return newMsg
    }

    fun markConversationAsRead(conversationId: String) {
        synchronized(conversations) {
            val convIndex = conversations.indexOfFirst { it.conversationId == conversationId }
            if (convIndex != -1) {
                conversations[convIndex] = conversations[convIndex].copy(unreadCount = 0)
            }
        }
        notifyConversationObservers()

        firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .update("unreadCount", 0)

        database?.let { db ->
            scope.launch {
                try {
                    db.conversationDao().markAsRead(conversationId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking conversation as read", e)
                }
            }
        }
    }
}
