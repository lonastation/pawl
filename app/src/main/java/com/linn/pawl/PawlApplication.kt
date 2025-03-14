package com.linn.pawl

import android.app.Activity
import android.app.Application
import com.linn.pawl.data.AppContainer
import com.linn.pawl.data.AppDataContainer

class PawlApplication : Application() {
    lateinit var container: AppContainer
    var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}