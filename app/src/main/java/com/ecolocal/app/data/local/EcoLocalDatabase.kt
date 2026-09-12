package com.ecolocal.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ecolocal.app.model.entity.ConversationEntity
import com.ecolocal.app.model.entity.ListingEntity
import com.ecolocal.app.model.entity.MessageEntity
import com.ecolocal.app.model.entity.NotificationEntity
import com.ecolocal.app.model.entity.RequestEntity
import com.ecolocal.app.model.entity.SavedListingEntity

@Database(
    entities = [
        ListingEntity::class,
        SavedListingEntity::class,
        RequestEntity::class,
        NotificationEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EcoLocalDatabase : RoomDatabase() {

    abstract fun listingDao(): ListingDao
    abstract fun savedDao(): SavedDao
    abstract fun requestDao(): RequestDao
    abstract fun notificationDao(): NotificationDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: EcoLocalDatabase? = null

        fun getDatabase(context: Context): EcoLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EcoLocalDatabase::class.java,
                    "ecolocal_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
