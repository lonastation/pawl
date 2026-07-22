package com.linn.pawl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanLogDao {
    @Query("SELECT * FROM scan_logs ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<ScanLogEntity>>

    @Insert
    suspend fun insert(entity: ScanLogEntity): Long

    @Query("DELETE FROM scan_logs")
    suspend fun deleteAll()

    @Query(
        """
        SELECT id FROM scan_logs
        ORDER BY timestamp DESC, id DESC
        LIMIT -1 OFFSET :keep
        """
    )
    suspend fun getIdsBeyondLimit(keep: Int): List<Long>

    @Query("DELETE FROM scan_logs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM scan_logs")
    suspend fun count(): Int
}
