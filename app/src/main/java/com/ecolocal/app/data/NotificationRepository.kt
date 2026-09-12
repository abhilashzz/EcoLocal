package com.ecolocal.app.data

import android.content.Context
import com.ecolocal.app.data.local.EcoLocalDatabase
import com.ecolocal.app.model.entity.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository managing in-app notifications.
 */
object NotificationRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: EcoLocalDatabase? = null
    private var isInitialized = false

    private val initialNotifications = listOf(
        NotificationEntity(
            id = "notif_welcome",
            title = "Welcome to EcoLocal!",
            message = "Explore nearby sustainable goods and community help services in Malabe.",
            timestamp = System.currentTimeMillis() - 86400000L,
            isRead = false,
            type = "SYSTEM"
        ),
        NotificationEntity(
            id = "notif_community",
            title = "Community Cleanup Drive",
            message = "Join the Athurugiriya green initiative this Saturday at 8:00 AM.",
            timestamp = System.currentTimeMillis() - 43200000L,
            isRead = false,
            type = "COMMUNITY"
        )
    )

    private val notifications = mutableListOf<NotificationEntity>().apply {
        addAll(initialNotifications)
    }

    fun init(context: Context) {
        if (isInitialized) return
        database = EcoLocalDatabase.getDatabase(context)
        isInitialized = true

        scope.launch {
            val dao = database?.notificationDao() ?: return@launch
            val count = dao.getAll().size
            if (count == 0) {
                initialNotifications.forEach { dao.insert(it) }
            } else {
                val persisted = dao.getAll()
                withContext(Dispatchers.Main) {
                    notifications.clear()
                    notifications.addAll(persisted)
                }
            }
        }
    }

    fun getAll(): List<NotificationEntity> {
        return notifications.toList()
    }

    fun getUnreadCount(): Int {
        return notifications.count { !it.isRead }
    }

    fun addNotification(
        title: String,
        message: String,
        type: String = "SYSTEM",
        targetListingId: String? = null,
        targetConversationId: String? = null
    ): NotificationEntity {
        val notif = NotificationEntity(
            id = "notif_${UUID.randomUUID()}",
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            type = type,
            targetListingId = targetListingId,
            targetConversationId = targetConversationId
        )

        notifications.add(0, notif)
        database?.let { db ->
            scope.launch {
                db.notificationDao().insert(notif)
            }
        }
        return notif
    }

    fun markAsRead(id: String) {
        val index = notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
        }
        database?.let { db ->
            scope.launch {
                db.notificationDao().markAsRead(id)
            }
        }
    }

    fun markAllAsRead() {
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(isRead = true)
        }
        database?.let { db ->
            scope.launch {
                db.notificationDao().markAllAsRead()
            }
        }
    }
}
