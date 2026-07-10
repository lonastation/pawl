package com.linn.pawl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageSignatureDao {

    @Query("SELECT * FROM image_signatures WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): ImageSignatureEntity?

    @Query("SELECT * FROM image_signatures WHERE path IN (:paths)")
    suspend fun getByPaths(paths: List<String>): List<ImageSignatureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ImageSignatureEntity)

    @Query("SELECT path FROM image_signatures")
    suspend fun getAllPaths(): List<String>

    @Query("DELETE FROM image_signatures WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("SELECT COUNT(*) FROM image_signatures")
    suspend fun count(): Int

    @Query("DELETE FROM image_signatures")
    suspend fun deleteAll()
}
