package com.linn.pawl.data.model

import android.net.Uri

data class VideoMedia(
    val mediaId: Long,
    val contentUri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val duration: Long,
    val width: Int = 0,
    val height: Int = 0,
    val dateCreated: Long = 0L,
    val lastModified: Long = 0L
)

data class VideoDuplicateGroup(
    val videos: List<VideoMedia>
)
