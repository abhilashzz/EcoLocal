package com.ecolocal.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ecolocal.app.model.entity.SavedListingEntity

@Dao
interface SavedDao {
    @Query("SELECT * FROM saved_listings ORDER BY savedAt DESC")
    suspend fun getAll(): List<SavedListingEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_listings WHERE listingId = :listingId)")
    suspend fun isSaved(listingId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(saved: SavedListingEntity)

    @Query("DELETE FROM saved_listings WHERE listingId = :listingId")
    suspend fun delete(listingId: String)
}
