package com.linn.pawl.data.model

data class ImageSignature(
    val md5: String,
    val dHash: Long,
    val pHash: Long,
    val width: Int,
    val height: Int
)
