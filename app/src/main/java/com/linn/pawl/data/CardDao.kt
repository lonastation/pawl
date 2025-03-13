package com.linn.pawl.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCard(card: Card);

    @Update
    suspend fun updateCard(card: Card);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: Log);

    @Query("select * from card order by id desc limit 1")
    fun getDefaultCard(): Flow<Card>;
}