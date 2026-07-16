package com.linn.pawl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrashMediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrashMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TrashMediaEntity>)

    @Query("SELECT * FROM trash_media ORDER BY recycledAt DESC")
    suspend fun getAll(): List<TrashMediaEntity>

    @Query("SELECT * FROM trash_media WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<TrashMediaEntity>

    @Query("SELECT * FROM trash_media WHERE recycledAt < :cutoffMillis")
    suspend fun getOlderThan(cutoffMillis: Long): List<TrashMediaEntity>

    @Query("DELETE FROM trash_media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM trash_media")
    suspend fun count(): Int
}
