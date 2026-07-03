package com.linn.pawl.data.model

data class VideoSignature(
    val frameHashes: List<Long>,
    val width: Int,
    val height: Int
)
