package com.linn.pawl

import android.app.Application
import com.linn.pawl.data.repository.RecycledMediaRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PawlApplication : Application() {

    @Inject
    lateinit var recycledMediaRepository: RecycledMediaRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            runCatching { recycledMediaRepository.purgeExpired() }
        }
    }
}
