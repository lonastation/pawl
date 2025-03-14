package com.linn.pawl.data

import kotlinx.coroutines.flow.Flow

class OfflineCardRepository(private val nfcCardDao: NfcCardDao) : CardRepository {
    override fun getAllCards(): Flow<List<NfcCardEntity>> = nfcCardDao.getAllCards()

    override fun getDefaultCard(): Flow<NfcCardEntity?> = nfcCardDao.getDefaultCard()

    override suspend fun insertCard(card: NfcCardEntity) = nfcCardDao.insertCard(card)

    override suspend fun clearDefaultCard() = nfcCardDao.clearDefaultCard()

    override suspend fun setDefaultCard(cardId: String) = nfcCardDao.setDefaultCard(cardId)

    override suspend fun updateDefaultCard(cardId: String, isDefault: Boolean) =
        nfcCardDao.updateDefaultCard(cardId, isDefault)

    override fun hasDefaultCard(): Flow<Boolean> = nfcCardDao.hasDefaultCard()

    override fun getAllLogs(): Flow<List<NfcLogEntity>> = nfcCardDao.getAllLogs()

    override suspend fun insertLog(log: NfcLogEntity) = nfcCardDao.insertLog(log)
}