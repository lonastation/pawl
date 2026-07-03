package com.linn.pawl.service

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.core.graphics.scale
import com.linn.pawl.data.model.VideoSignature
import com.linn.pawl.data.repository.VideoSignatureRepository
import com.linn.pawl.ui.DuplicateGroup
import com.linn.pawl.ui.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VideoScanner @Inject constructor(
    private val signatureRepository: VideoSignatureRepository
) {

    // 时长容差（毫秒），考虑到编码精度，允许微小差异
    private val durationToleranceMs = 500L
    // 文件大小容差（字节），允许微小差异
    private val sizeToleranceBytes = 1024L * 10 // 10KB

    // 在视频时长上的采样点（比例），用于提取视觉指纹
    private val sampleFractions = listOf(0.1, 0.25, 0.5, 0.75, 0.9)
    // dHash 为 64 bit；允许的汉明距离（越大越宽松）
    private val maxHammingDistance = 10
    // 至少需要的有效帧采样数
    private val minFrameSamples = 3
    // 匹配帧数占有效比较帧数的最低比例
    private val minMatchingFrameRatio = 0.8

    suspend fun findDuplicateVideos(
        videos: List<VideoFile>,
        onProgress: (Int) -> Unit
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        if (videos.isEmpty()) return@withContext emptyList()

        signatureRepository.deleteStale(videos.map { it.path })

        val signatureCache = loadOrComputeSignatures(videos, onProgress)

        if (videos.size < 2) return@withContext emptyList()

        val durationGroups = groupByDuration(videos)

        val candidateGroups = mutableListOf<List<VideoFile>>()
        durationGroups.forEach { group ->
            candidateGroups.addAll(groupBySize(group))
        }

        if (candidateGroups.isEmpty()) {
            return@withContext emptyList()
        }

        val resultGroups = mutableListOf<DuplicateGroup>()
        candidateGroups.forEach { candidateGroup ->
            if (candidateGroup.size < 2) return@forEach

            val signatures = candidateGroup.mapNotNull { video ->
                signatureCache[video.path]?.let { video.path to it }
            }.toMap()

            val videoByPath = candidateGroup.associateBy { it.path }
            resultGroups.addAll(findDuplicatesInGroup(signatures, videoByPath))
        }

        resultGroups
    }

    private suspend fun loadOrComputeSignatures(
        videos: List<VideoFile>,
        onProgress: (Int) -> Unit
    ): Map<String, VideoSignature> {
        val signatures = signatureRepository.getCachedBatch(videos).toMutableMap()
        var processedCount = 0

        for (video in videos) {
            if (video.path !in signatures) {
                val signature = extractVisualSignature(video)
                if (signature != null) {
                    signatureRepository.save(video, signature)
                    signatures[video.path] = signature
                }
            }
            processedCount++
            onProgress(processedCount)
        }

        return signatures
    }

    /**
     * 按时长分组，时长相差在容差范围内的归为一组
     */
    private fun groupByDuration(videos: List<VideoFile>): List<List<VideoFile>> {
        val groups = mutableListOf<List<VideoFile>>()
        val processed = mutableSetOf<String>()

        for (i in videos.indices) {
            val video1 = videos[i]
            if (video1.path in processed) continue

            val group = mutableListOf(video1)
            for (j in i + 1 until videos.size) {
                val video2 = videos[j]
                if (video2.path in processed) continue

                if (isDurationSimilar(video1.duration, video2.duration)) {
                    group.add(video2)
                    processed.add(video2.path)
                }
            }

            if (group.size > 1) {
                groups.add(group)
            }
            processed.add(video1.path)
        }

        return groups
    }

    /**
     * 在时长相近的组内，按文件大小进一步分组
     */
    private fun groupBySize(videos: List<VideoFile>): List<List<VideoFile>> {
        val groups = mutableListOf<List<VideoFile>>()
        val processed = mutableSetOf<String>()

        for (i in videos.indices) {
            val video1 = videos[i]
            if (video1.path in processed) continue

            val group = mutableListOf(video1)
            for (j in i + 1 until videos.size) {
                val video2 = videos[j]
                if (video2.path in processed) continue

                if (isSizeSimilar(video1.size, video2.size)) {
                    group.add(video2)
                    processed.add(video2.path)
                }
            }

            if (group.size > 1) {
                groups.add(group)
            }
            processed.add(video1.path)
        }

        return groups
    }

    private fun isDurationSimilar(duration1: Long, duration2: Long): Boolean {
        return kotlin.math.abs(duration1 - duration2) <= durationToleranceMs
    }

    private fun isSizeSimilar(size1: Long, size2: Long): Boolean {
        if (size1 == size2) return true
        if (kotlin.math.abs(size1 - size2) <= sizeToleranceBytes) return true

        val diffPercent = kotlin.math.abs(size1 - size2).toDouble() / maxOf(size1, size2)
        return diffPercent <= 0.02
    }

    private fun findDuplicatesInGroup(
        signatures: Map<String, VideoSignature>,
        videoByPath: Map<String, VideoFile>
    ): List<DuplicateGroup> {
        if (signatures.size < 2) return emptyList()

        val groups = mutableListOf<DuplicateGroup>()
        val processed = mutableSetOf<String>()

        val signatureList = signatures.entries.toList()
        for (i in signatureList.indices) {
            val (path1, sig1) = signatureList[i]
            if (path1 in processed) continue

            val duplicates = mutableListOf<VideoFile>()
            videoByPath[path1]?.let { duplicates.add(it) }
            for (j in i + 1 until signatureList.size) {
                val (path2, sig2) = signatureList[j]
                if (path2 in processed) continue

                if (areVisuallySimilar(sig1, sig2)) {
                    videoByPath[path2]?.let { duplicates.add(it) }
                    processed.add(path2)
                }
            }

            if (duplicates.size > 1) {
                groups.add(DuplicateGroup(duplicates))
            }
            processed.add(path1)
        }

        return groups
    }

    /**
     * 在多个时间点解码帧，计算 dHash 作为视觉指纹。
     * 相比关键帧时间戳，dHash 反映实际画面内容，能有效避免 GOP 结构相同导致的误判。
     */
    private fun extractVisualSignature(video: VideoFile): VideoSignature? {
        if (video.duration <= 0) return null

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever().apply {
                setDataSource(video.path)
            }

            val frameHashes = mutableListOf<Long>()
            for (fraction in sampleFractions) {
                val timeUs = (video.duration * 1000L * fraction).toLong()
                val frame = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: continue

                try {
                    frameHashes.add(computeDHash(frame))
                } finally {
                    frame.recycle()
                }
            }

            if (frameHashes.size < minFrameSamples) return null

            return VideoSignature(
                frameHashes = frameHashes,
                width = video.width,
                height = video.height
            )
        } catch (_: Exception) {
            return null
        } finally {
            retriever?.release()
        }
    }

    /**
     * 9×8 差分哈希（dHash）：缩略后比较相邻像素灰度，得到 64 bit 指纹。
     */
    private fun computeDHash(bitmap: Bitmap): Long {
        val hashWidth = 9
        val hashHeight = 8
        val scaled = bitmap.scale(hashWidth, hashHeight)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }

        val pixels = IntArray(hashWidth * hashHeight)
        scaled.getPixels(pixels, 0, hashWidth, 0, 0, hashWidth, hashHeight)
        scaled.recycle()

        var hash = 0L
        var bitIndex = 0
        for (y in 0 until hashHeight) {
            for (x in 0 until hashWidth - 1) {
                val left = grayscale(pixels[y * hashWidth + x])
                val right = grayscale(pixels[y * hashWidth + x + 1])
                if (left > right) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        return hash
    }

    private fun grayscale(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    private fun hammingDistance(a: Long, b: Long): Int {
        return java.lang.Long.bitCount(a xor b)
    }

    private fun areVisuallySimilar(sig1: VideoSignature, sig2: VideoSignature): Boolean {
        if (sig1.width != sig2.width || sig1.height != sig2.height) {
            return false
        }

        val hashes1 = sig1.frameHashes
        val hashes2 = sig2.frameHashes
        if (hashes1.size < minFrameSamples || hashes2.size < minFrameSamples) {
            return false
        }

        val compareCount = minOf(hashes1.size, hashes2.size)
        if (compareCount < minFrameSamples) return false

        var matchingFrames = 0
        for (i in 0 until compareCount) {
            if (hammingDistance(hashes1[i], hashes2[i]) <= maxHammingDistance) {
                matchingFrames++
            }
        }

        val requiredMatches = kotlin.math.ceil(compareCount * minMatchingFrameRatio).toInt()
        return matchingFrames >= requiredMatches
    }
}
