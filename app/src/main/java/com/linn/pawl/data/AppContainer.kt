package com.linn.pawl.data

import android.content.Context

interface AppContainer {
    val cardRepository: CardRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val cardRepository: CardRepository by lazy {
        OfflineCardRepository(AppDatabase.getDatabase(context).nfcCardDao())
    }
}