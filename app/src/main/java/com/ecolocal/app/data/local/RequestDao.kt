package com.ecolocal.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ecolocal.app.model.entity.RequestEntity

@Dao
interface RequestDao {
    @Query("SELECT * FROM requests ORDER BY createdAt DESC")
    suspend fun getAll(): List<RequestEntity>

    @Query("SELECT * FROM requests WHERE requestId = :requestId LIMIT 1")
    suspend fun getById(requestId: String): RequestEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM requests WHERE listingId = :listingId AND requesterName = :requesterName AND requestType = :requestType)")
    suspend fun exists(listingId: String, requesterName: String, requestType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: RequestEntity)

    @Update
    suspend fun update(request: RequestEntity)

    @Query("DELETE FROM requests WHERE requestId = :requestId")
    suspend fun delete(requestId: String)

    @Query("SELECT COUNT(*) FROM requests")
    suspend fun count(): Int
}
