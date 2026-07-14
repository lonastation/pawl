package com.linn.pawl.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

fun openManageAllFilesAccessSettings(context: Context) {
    val appIntent = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        "package:${context.packageName}".toUri()
    )
    try {
        context.startActivity(appIntent)
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
}
