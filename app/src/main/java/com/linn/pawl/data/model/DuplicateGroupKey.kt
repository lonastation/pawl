package com.linn.pawl.data.model

import java.security.MessageDigest

object DuplicateGroupKey {
    const val MEDIA_IMAGE = "image"
    const val MEDIA_VIDEO = "video"

    fun fromPaths(mediaType: String, paths: Collection<String>): String {
        val canonical = paths.sorted().joinToString("\u0000")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        val hash = digest.joinToString("") { "%02x".format(it) }
        return "$mediaType:$hash"
    }

    fun encodeMemberPaths(paths: Collection<String>): String =
        paths.sorted().joinToString("\n")

    fun decodeMemberPaths(encoded: String): List<String> =
        if (encoded.isEmpty()) emptyList() else encoded.split("\n")
}
