package com.linn.pawl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoSignatureDao {

    @Query("SELECT * FROM video_signatures WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): VideoSignatureEntity?

    @Query("SELECT * FROM video_signatures WHERE path IN (:paths)")
    suspend fun getByPaths(paths: List<String>): List<VideoSignatureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VideoSignatureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<VideoSignatureEntity>)

    @Query("SELECT path FROM video_signatures")
    suspend fun getAllPaths(): List<String>

    @Query("DELETE FROM video_signatures WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("SELECT COUNT(*) FROM video_signatures")
    suspend fun count(): Int

    @Query("DELETE FROM video_signatures")
    suspend fun deleteAll()
}
