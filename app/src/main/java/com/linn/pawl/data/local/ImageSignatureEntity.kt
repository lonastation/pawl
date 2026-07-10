package com.linn.pawl.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_signatures")
data class ImageSignatureEntity(
    @PrimaryKey val path: String,
    val fileName: String,
    val lastModified: Long,
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val md5: String,
    val dHash: Long,
    val computedAt: Long
)
