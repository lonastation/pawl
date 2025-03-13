package com.linn.pawl.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: Log)

    @Query("select * from punch_in_log order by id asc")
    fun getAllLogs(): Flow<List<Log>>
}