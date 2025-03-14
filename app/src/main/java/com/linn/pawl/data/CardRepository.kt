package com.linn.pawl.data

import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getAllCards(): Flow<List<NfcCardEntity>>

    fun getDefaultCard(): Flow<NfcCardEntity?>

    suspend fun insertCard(card: NfcCardEntity)

    suspend fun clearDefaultCard()

    suspend fun setDefaultCard(cardId: String)

    suspend fun updateDefaultCard(cardId: String, isDefault: Boolean)

    fun hasDefaultCard(): Flow<Boolean>

    fun getAllLogs(): Flow<List<NfcLogEntity>>

    suspend fun insertLog(log: NfcLogEntity)
}