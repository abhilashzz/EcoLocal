package com.ecolocal.app.data

import android.content.Context
import android.util.Log
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.entity.NotificationEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Repository managing in-app notifications with Firestore collection "notifications"
 * and local Room fallback.
 */
object NotificationRepository {

    private const val TAG = "NotificationRepository"
    private const val COLLECTION_NOTIFICATIONS = "notifications"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var snapshotListener: ListenerRegistration? = null
    private val changeListeners = mutableListOf<() -> Unit>()

    private val initialNotifications = listOf(
        NotificationEntity(
            id = "notif_welcome",
            title = "Welcome to EcoLocal!",
            message = "Explore nearby sustainable goods and community help services in your area.",
            timestamp = System.currentTimeMillis() - 86400000L,
            isRead = false,
            type = "SYSTEM"
        ),
        NotificationEntity(
            id = "notif_community",
            title = "Community Cleanup Drive",
            message = "Join the suburban green initiative this Saturday at 8:00 AM.",
            timestamp = System.currentTimeMillis() - 43200000L,
            isRead = false,
            type = "COMMUNITY"
        )
    )

    private val notifications = mutableListOf<NotificationEntity>().apply {
        addAll(initialNotifications)
    }

    private var appContext: Context? = null
    private var isInitialSnapshot = true
    private val presentedNotificationIds = mutableSetOf<String>()

    fun init(context: Context) {
        appContext = context.applicationContext
        com.ecolocal.app.util.NotificationHelper.createNotificationChannel(context)

        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        scope.launch {
            val dao = database?.notificationDao()
            if (dao != null) {
                val persisted = dao.getAll()
                if (persisted.isNotEmpty()) {
                    synchronized(notifications) {
                        notifications.clear()
                        notifications.addAll(persisted)
                    }
                    notifyListeners()
                }
            }
        }

        startFirestoreListener()
    }

    fun startFirestoreListener() {
        snapshotListener?.remove()
        isInitialSnapshot = true

        val currentUser = auth.currentUser
        val currentUid = currentUser?.uid ?: ""

        val query = if (currentUid.isNotEmpty()) {
            firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", currentUid)
        } else {
            firestore.collection(COLLECTION_NOTIFICATIONS)
        }

        snapshotListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Firestore notifications listener error: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot == null) return@addSnapshotListener

            val remoteNotifs = snapshot.documents.mapNotNull { doc ->
                NotificationEntity.fromDocument(doc)
            }.sortedByDescending { it.timestamp }

            synchronized(notifications) {
                notifications.clear()
                if (remoteNotifs.isNotEmpty()) {
                    notifications.addAll(remoteNotifs)
                } else {
                    notifications.addAll(initialNotifications)
                }
            }

            notifyListeners()

            // Prevent re-notifying for old existing notifications on startup
            if (isInitialSnapshot) {
                isInitialSnapshot = false
                snapshot.documents.forEach { doc ->
                    presentedNotificationIds.add(doc.id)
                }
            } else {
                for (change in snapshot.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val notif = NotificationEntity.fromDocument(doc)
                        if (notif != null && !presentedNotificationIds.contains(notif.id)) {
                            presentedNotificationIds.add(notif.id)
                            appContext?.let { ctx ->
                                com.ecolocal.app.util.NotificationHelper.showSystemNotification(
                                    context = ctx,
                                    id = notif.id,
                                    title = notif.title,
                                    message = notif.message,
                                    targetListingId = notif.targetListingId,
                                    targetConversationId = notif.targetConversationId
                                )
                            }
                        }
                    }
                }
            }

            scope.launch {
                try {
                    val dao = database?.notificationDao()
                    if (dao != null && remoteNotifs.isNotEmpty()) {
                        remoteNotifs.forEach { dao.insert(it) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error caching notifications in Room", e)
                }
            }
        }
    }

    fun addChangeListener(listener: () -> Unit) {
        synchronized(changeListeners) {
            if (!changeListeners.contains(listener)) {
                changeListeners.add(listener)
            }
        }
    }

    fun removeChangeListener(listener: () -> Unit) {
        synchronized(changeListeners) {
            changeListeners.remove(listener)
        }
    }

    private fun notifyListeners() {
        scope.launch(Dispatchers.Main) {
            val callbacks = synchronized(changeListeners) { changeListeners.toList() }
            callbacks.forEach { it.invoke() }
        }
    }

    fun getAll(): List<NotificationEntity> {
        return synchronized(notifications) { notifications.toList() }
    }

    fun getUnreadCount(): Int {
        return synchronized(notifications) { notifications.count { !it.isRead } }
    }

    fun addNotification(
        title: String,
        message: String,
        type: String = "SYSTEM",
        targetListingId: String? = null,
        targetConversationId: String? = null
    ): NotificationEntity {
        val currentUid = auth.currentUser?.uid ?: ""
        val notif = NotificationEntity(
            id = "notif_${UUID.randomUUID()}",
            userId = currentUid,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            type = type,
            targetListingId = targetListingId,
            targetConversationId = targetConversationId
        )

        presentedNotificationIds.add(notif.id)

        synchronized(notifications) {
            notifications.add(0, notif)
        }
        notifyListeners()

        appContext?.let { ctx ->
            com.ecolocal.app.util.NotificationHelper.showSystemNotification(
                context = ctx,
                id = notif.id,
                title = notif.title,
                message = notif.message,
                targetListingId = notif.targetListingId,
                targetConversationId = notif.targetConversationId
            )
        }

        if (currentUid.isNotEmpty()) {
            firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(notif.id)
                .set(notif.toMap())
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing notification to Firestore", e)
                }
        }

        database?.let { db ->
            scope.launch {
                try {
                    db.notificationDao().insert(notif)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting notification into Room", e)
                }
            }
        }

        return notif
    }

    fun markAsRead(id: String) {
        synchronized(notifications) {
            val index = notifications.indexOfFirst { it.id == id }
            if (index != -1) {
                notifications[index] = notifications[index].copy(isRead = true)
            }
        }
        notifyListeners()

        firestore.collection(COLLECTION_NOTIFICATIONS)
            .document(id)
            .update("isRead", true)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed updating notification isRead in Firestore", e)
            }

        database?.let { db ->
            scope.launch {
                try {
                    db.notificationDao().markAsRead(id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating notification in Room", e)
                }
            }
        }
    }

    fun markAllAsRead() {
        synchronized(notifications) {
            for (i in notifications.indices) {
                notifications[i] = notifications[i].copy(isRead = true)
            }
        }
        notifyListeners()

        database?.let { db ->
            scope.launch {
                try {
                    db.notificationDao().markAllAsRead()
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking all as read in Room", e)
                }
            }
        }
    }

    fun deleteNotification(id: String) {
        synchronized(notifications) {
            notifications.removeAll { it.id == id }
        }
        notifyListeners()

        firestore.collection(COLLECTION_NOTIFICATIONS)
            .document(id)
            .delete()
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed deleting notification in Firestore", e)
            }

        database?.let { db ->
            scope.launch {
                try {
                    db.notificationDao().delete(id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting notification in Room", e)
                }
            }
        }
    }
}
