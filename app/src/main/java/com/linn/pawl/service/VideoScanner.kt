package com.linn.pawl.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.core.graphics.scale
import com.linn.pawl.R
import com.linn.pawl.data.model.ScanLogLevel
import com.linn.pawl.data.model.VideoDuplicateGroup
import com.linn.pawl.data.model.VideoMedia
import com.linn.pawl.data.model.VideoSignature
import com.linn.pawl.data.repository.ScanLogRepository
import com.linn.pawl.data.repository.VideoSignatureRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject

class VideoScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val signatureRepository: VideoSignatureRepository,
    private val scanLogRepository: ScanLogRepository
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
        videos: List<VideoMedia>,
        onProgress: (Int) -> Unit
    ): List<VideoDuplicateGroup> = withContext(Dispatchers.IO) {
        if (videos.isEmpty()) return@withContext emptyList()

        signatureRepository.deleteStale(videos.map { it.path })

        // Phase 1: generate or load signatures for all videos
        val signatureCache = loadOrComputeSignatures(videos, onProgress)

        if (videos.size < 2) return@withContext emptyList()

        // Phase 2: compare within candidate groups
        val candidateGroups = buildCandidateGroups(videos)
        if (candidateGroups.isEmpty()) return@withContext emptyList()

        compareCandidateGroups(candidateGroups, signatureCache)
    }

    private suspend fun loadOrComputeSignatures(
        videos: List<VideoMedia>,
        onProgress: (Int) -> Unit
    ): Map<String, VideoSignature> {
        val signatures = signatureRepository.getCachedBatch(videos).toMutableMap()
        var processedCount = 0

        for (video in videos) {
            if (video.path !in signatures) {
                val signature = extractSignature(video)
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

    private fun buildCandidateGroups(videos: List<VideoMedia>): List<List<VideoMedia>> {
        val candidateGroups = mutableListOf<List<VideoMedia>>()
        groupByDuration(videos).forEach { group ->
            candidateGroups.addAll(groupBySize(group))
        }
        return candidateGroups.filter { it.size >= 2 }
    }

    private fun compareCandidateGroups(
        candidateGroups: List<List<VideoMedia>>,
        signatureCache: Map<String, VideoSignature>
    ): List<VideoDuplicateGroup> {
        val resultGroups = mutableListOf<VideoDuplicateGroup>()
        candidateGroups.forEach { candidateGroup ->
            val signatures = candidateGroup.mapNotNull { video ->
                signatureCache[video.path]?.let { video.path to it }
            }.toMap()

            val videoByPath = candidateGroup.associateBy { it.path }
            resultGroups.addAll(findDuplicatesInGroup(signatures, videoByPath))
        }
        return resultGroups
    }

    /**
     * 按时长分组，时长相差在容差范围内的归为一组
     */
    private fun groupByDuration(videos: List<VideoMedia>): List<List<VideoMedia>> {
        val groups = mutableListOf<List<VideoMedia>>()
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
    private fun groupBySize(videos: List<VideoMedia>): List<List<VideoMedia>> {
        val groups = mutableListOf<List<VideoMedia>>()
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
        videoByPath: Map<String, VideoMedia>
    ): List<VideoDuplicateGroup> {
        if (signatures.size < 2) return emptyList()

        val groups = mutableListOf<VideoDuplicateGroup>()
        val processed = mutableSetOf<String>()

        // Stage A: cluster by exact MD5 match
        val md5Buckets = signatures.entries
            .filter { it.value.md5.isNotEmpty() }
            .groupBy { it.value.md5 }

        for (entries in md5Buckets.values) {
            if (entries.size < 2) continue

            val duplicates = entries.mapNotNull { (path, _) -> videoByPath[path] }
            if (duplicates.size >= 2) {
                groups.add(VideoDuplicateGroup(duplicates))
                processed.addAll(entries.map { it.key })
            }
        }

        // Stage B: DHash comparison for remaining videos
        val remaining = signatures.filterKeys { it !in processed }
        groups.addAll(findVisualDuplicatesInGroup(remaining, videoByPath))

        return groups
    }

    private fun findVisualDuplicatesInGroup(
        signatures: Map<String, VideoSignature>,
        videoByPath: Map<String, VideoMedia>
    ): List<VideoDuplicateGroup> {
        if (signatures.size < 2) return emptyList()

        val groups = mutableListOf<VideoDuplicateGroup>()
        val processed = mutableSetOf<String>()

        val signatureList = signatures.entries.toList()
        for (i in signatureList.indices) {
            val (path1, sig1) = signatureList[i]
            if (path1 in processed) continue

            val duplicates = mutableListOf<VideoMedia>()
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
                groups.add(VideoDuplicateGroup(duplicates))
            }
            processed.add(path1)
        }

        return groups
    }

    /**
     * 计算 MD5 文件哈希与 dHash 视觉指纹。
     */
    private suspend fun extractSignature(video: VideoMedia): VideoSignature? {
        if (video.duration <= 0) return null

        return try {
            val md5 = computeFileMd5(video.path) ?: return null
            val frameHashes = extractFrameHashes(video) ?: return null

            VideoSignature(
                md5 = md5,
                frameHashes = frameHashes,
                width = video.width,
                height = video.height
            )
        } catch (t: Throwable) {
            scanLogRepository.append(
                mediaType = ScanLogRepository.MEDIA_VIDEO,
                level = ScanLogLevel.ERROR,
                message = context.getString(R.string.scan_log_video_signature_failed),
                path = video.path,
                throwable = t
            )
            null
        }
    }

    private fun computeFileMd5(path: String): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(64 * 1024)
            FileInputStream(path).use { input ->
                var bytesRead = input.read(buffer)
                while (bytesRead >= 0) {
                    if (bytesRead > 0) {
                        digest.update(buffer, 0, bytesRead)
                    }
                    bytesRead = input.read(buffer)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 在多个时间点解码帧，计算 dHash 作为视觉指纹。
     */
    private suspend fun extractFrameHashes(video: VideoMedia): List<Long>? {
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
            return frameHashes
        } catch (t: Exception) {
            scanLogRepository.append(
                mediaType = ScanLogRepository.MEDIA_VIDEO,
                level = ScanLogLevel.WARN,
                message = context.getString(R.string.scan_log_video_frames_failed),
                path = video.path,
                throwable = t
            )
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
