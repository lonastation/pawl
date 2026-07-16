package com.linn.pawl.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_media")
data class TrashMediaEntity(
    @PrimaryKey val id: String,
    val mediaType: String,
    val originalMediaId: Long,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val originalPath: String,
    val relativePath: String,
    val trashFileName: String,
    val dateTaken: Long,
    val recycledAt: Long
)
