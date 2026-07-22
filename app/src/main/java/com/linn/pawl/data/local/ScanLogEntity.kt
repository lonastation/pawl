package com.linn.pawl.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_logs")
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val mediaType: String,
    val level: String,
    val path: String?,
    val message: String,
    val detail: String?
)
