package com.linn.pawl.data

import android.content.Context

interface AppContainer {
    val pawlRepository: PawlRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val pawlRepository: PawlRepository by lazy {
        OfflinePawlRepository(
            PawlDatabase.getDatabase(context).cardDao(),
            PawlDatabase.getDatabase(context).logDao()
        )
    }
}