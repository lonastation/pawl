package com.linn.pawl.data.model

import android.net.Uri

data class ImageMedia(
    val mediaId: Long,
    val contentUri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val dateCreated: Long = 0L,
    val lastModified: Long = 0L,
    val mimeType: String = ""
) {
    val isGif: Boolean
        get() = mimeType.equals("image/gif", ignoreCase = true) ||
            name.endsWith(".gif", ignoreCase = true) ||
            path.endsWith(".gif", ignoreCase = true)
}

data class ImageDuplicateGroup(
    val images: List<ImageMedia>
)
