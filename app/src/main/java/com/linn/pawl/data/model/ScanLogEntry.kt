package com.linn.pawl.data.model

enum class ScanLogLevel {
    INFO,
    WARN,
    ERROR
}

data class ScanLogEntry(
    val id: Long = 0L,
    val timestamp: Long,
    val mediaType: String,
    val level: ScanLogLevel,
    val path: String?,
    val message: String,
    val detail: String?
)
