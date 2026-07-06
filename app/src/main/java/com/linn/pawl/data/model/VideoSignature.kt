package com.linn.pawl.data.model

data class VideoSignature(
    val md5: String,
    val frameHashes: List<Long>,
    val width: Int,
    val height: Int
)
