package com.linn.pawl.service

import android.media.MediaExtractor
import android.media.MediaFormat
import com.linn.pawl.ui.viewmodels.DuplicateGroup
import com.linn.pawl.ui.viewmodels.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VideoScanner @Inject constructor() {

    // 时长容差（毫秒），考虑到编码精度，允许微小差异
    private val durationToleranceMs = 500L
    // 文件大小容差（字节），允许微小差异
    private val sizeToleranceBytes = 1024L * 10 // 10KB

    suspend fun findSimilarVideos(
        videos: List<VideoFile>,
        onProgress: (Int) -> Unit
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {

        if (videos.size < 2) return@withContext emptyList()

        // === 第一层：按时长分组 ===
        val durationGroups = groupByDuration(videos)

        // === 第二层：在每个时长组内，按文件大小分组 ===
        val candidateGroups = mutableListOf<List<VideoFile>>()
        durationGroups.forEach { group ->
            val sizeGroups = groupBySize(group)
            candidateGroups.addAll(sizeGroups)
        }

        // 如果没有候选组，直接返回空
        if (candidateGroups.isEmpty()) {
            return@withContext emptyList()
        }

        // === 第三层：对候选组进行深度分析 ===
        val resultGroups = mutableListOf<DuplicateGroup>()
        var processedCount = 0

        candidateGroups.forEach { candidateGroup ->
            if (candidateGroup.size < 2) return@forEach

            // 提取候选组内每个视频的签名
            val signatures = mutableMapOf<String, VideoSignature>()
            candidateGroup.forEach { video ->
                try {
                    val signature = extractHeaderSignature(video.path)
                    signature?.let { signatures[video.path] = it }
                } catch (e: Exception) {
                    // 跳过无法分析的视频
                    e.printStackTrace()
                }
                processedCount++
                onProgress(processedCount)
            }

            val videoByPath = candidateGroup.associateBy { it.path }

            // 在候选组内进行比对
            val similarGroups = findSimilarInGroup(signatures, videoByPath)
            resultGroups.addAll(similarGroups)
        }

        resultGroups
    }

    /**
     * 第一层：按时长分组
     * 时长相差在容差范围内的归为一组
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
     * 第二层：按文件大小分组
     * 在时长相近的组内，进一步按文件大小分组
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

    /**
     * 判断两个时长是否相似
     */
    private fun isDurationSimilar(duration1: Long, duration2: Long): Boolean {
        return kotlin.math.abs(duration1 - duration2) <= durationToleranceMs
    }

    /**
     * 判断两个文件大小是否相似
     */
    private fun isSizeSimilar(size1: Long, size2: Long): Boolean {
        // 如果大小完全相同，直接返回true
        if (size1 == size2) return true

        // 如果大小接近，返回true
        if (kotlin.math.abs(size1 - size2) <= sizeToleranceBytes) return true

        // 如果大小差异在2%以内，也认为可能相似（用于不同编码版本）
        val diffPercent = kotlin.math.abs(size1 - size2).toDouble() / maxOf(size1, size2)
        return diffPercent <= 0.02
    }

    /**
     * 在候选组内查找相似视频
     */
    private fun findSimilarInGroup(
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

            val similar = mutableListOf<VideoFile>()
            videoByPath[path1]?.let { similar.add(it) }
            for (j in i + 1 until signatureList.size) {
                val (path2, sig2) = signatureList[j]
                if (path2 in processed) continue

                if (areSimilar(sig1, sig2)) {
                    videoByPath[path2]?.let { similar.add(it) }
                    processed.add(path2)
                }
            }

            if (similar.size > 1) {
                groups.add(DuplicateGroup(similar))
            }
            processed.add(path1)
        }

        return groups
    }

    /**
     * 提取视频开头的特征签名
     */
    private fun extractHeaderSignature(videoPath: String): VideoSignature? {
        var extractor: MediaExtractor? = null
        try {
            extractor = MediaExtractor().apply {
                setDataSource(videoPath)
            }

            // 寻找视频轨道
            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) {
                return null
            }

            // 获取视频分辨率
            val width = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)

            extractor.selectTrack(videoTrackIndex)

            // 读取前10个关键帧的时间戳作为特征
            val keyFrameTimes = mutableListOf<Long>()
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            var framesRead = 0
            while (framesRead < 10) {
                val sampleTime = extractor.sampleTime
                if (sampleTime == -1L) break

                if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    keyFrameTimes.add(sampleTime)
                    framesRead++
                }
                extractor.advance()
            }

            if (keyFrameTimes.size < 3) {
                return null // 视频太短
            }

            return VideoSignature(
                keyFrameTimes = keyFrameTimes,
                frameCount = keyFrameTimes.size,
                width = width,
                height = height
            )
        } catch (_: Exception) {
            return null
        } finally {
            extractor?.release()
        }
    }

    /**
     * 判断两个签名是否相似
     */
    private fun areSimilar(sig1: VideoSignature, sig2: VideoSignature): Boolean {
        // 分辨率不同，直接判定不相似（如果分辨率不同不可能是同一个视频）
        if (sig1.width != sig2.width || sig1.height != sig2.height) {
            return false
        }

        val times1 = sig1.keyFrameTimes
        val times2 = sig2.keyFrameTimes

        // 如果帧数差异过大，直接判定不相似
        if (kotlin.math.abs(times1.size - times2.size) > 2) return false

        // 计算相似度
        val minSize = minOf(times1.size, times2.size)
        if (minSize < 2) return false

        // 计算归一化相关系数
        var correlation = 0.0
        for (i in 0 until minSize) {
            val diff = times1[i] - times2[i]
            // 时间戳差异越小，相似度越高
            correlation += 1.0 / (1 + kotlin.math.abs(diff) / 1000.0)
        }

        val similarity = correlation / minSize
        return similarity > 0.7 // 阈值可调
    }

    private data class VideoSignature(
        val keyFrameTimes: List<Long>,
        val frameCount: Int,
        val width: Int,
        val height: Int
    )
}