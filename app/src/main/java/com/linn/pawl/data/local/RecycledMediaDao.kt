package com.linn.pawl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecycledMediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecycledMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<RecycledMediaEntity>)

    @Query("SELECT * FROM recycled_media ORDER BY recycledAt DESC")
    suspend fun getAll(): List<RecycledMediaEntity>

    @Query("SELECT * FROM recycled_media WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<RecycledMediaEntity>

    @Query("DELETE FROM recycled_media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM recycled_media")
    suspend fun count(): Int
}
