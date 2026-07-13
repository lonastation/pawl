package com.linn.pawl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IgnoredDuplicateGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: IgnoredDuplicateGroupEntity)

    @Query(
        "SELECT groupKey FROM ignored_duplicate_groups WHERE mediaType = :mediaType"
    )
    suspend fun getKeys(mediaType: String): List<String>

    @Query(
        "SELECT * FROM ignored_duplicate_groups WHERE mediaType = :mediaType"
    )
    suspend fun getAll(mediaType: String): List<IgnoredDuplicateGroupEntity>

    @Query(
        "DELETE FROM ignored_duplicate_groups WHERE mediaType = :mediaType AND groupKey IN (:groupKeys)"
    )
    suspend fun deleteByKeys(mediaType: String, groupKeys: List<String>)

    @Query("DELETE FROM ignored_duplicate_groups WHERE mediaType = :mediaType")
    suspend fun deleteAll(mediaType: String)

    @Query(
        "SELECT COUNT(*) FROM ignored_duplicate_groups WHERE mediaType = :mediaType"
    )
    suspend fun count(mediaType: String): Int
}
