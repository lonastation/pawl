package com.linn.pawl.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcCardDao {
    @Query("SELECT * FROM nfc_cards ORDER BY isDefault DESC, lastReadTime DESC")
    fun getAllCards(): Flow<List<NfcCardEntity>>

    @Query("SELECT * FROM nfc_cards WHERE isDefault = 1 LIMIT 1")
    fun getDefaultCard(): Flow<NfcCardEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: NfcCardEntity)

    @Query("UPDATE nfc_cards SET isDefault = 0")
    suspend fun clearDefaultCard()

    @Transaction
    suspend fun setDefaultCard(cardId: String) {
        clearDefaultCard()
        updateDefaultCard(cardId, true)
    }

    @Query("UPDATE nfc_cards SET isDefault = :isDefault WHERE id = :cardId")
    suspend fun updateDefaultCard(cardId: String, isDefault: Boolean)

    @Query("SELECT EXISTS(SELECT 1 FROM nfc_cards WHERE isDefault = 1)")
    fun hasDefaultCard(): Flow<Boolean>

    @Query("SELECT * FROM nfc_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<NfcLogEntity>>

    @Insert
    suspend fun insertLog(log: NfcLogEntity)
} 