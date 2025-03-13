package com.linn.pawl.data

import kotlinx.coroutines.flow.Flow

interface PawlRepository {

    suspend fun updateCard(card: Card)

    suspend fun insertLog(log: Log)

    fun getDefaultCard(): Flow<Card>

    fun getLogs(): Flow<List<Log>>
}