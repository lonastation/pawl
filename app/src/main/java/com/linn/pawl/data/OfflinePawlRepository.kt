package com.linn.pawl.data

import kotlinx.coroutines.flow.Flow

class OfflinePawlRepository(private val cardDao: CardDao, private val logDao: LogDao) :
    PawlRepository {

    override fun getDefaultCard(): Flow<Card> = cardDao.getDefaultCard()

    override suspend fun updateCard(card: Card) = cardDao.updateCard(card)

    override suspend fun insertLog(log: Log) = logDao.insert(log)

    override fun getLogs(): Flow<List<Log>> = logDao.getAllLogs()
}