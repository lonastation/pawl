package com.linn.pawl.service

import android.media.MediaExtractor
import android.media.MediaFormat
import com.linn.pawl.ui.viewmodels.DuplicateGroup
import com.linn.pawl.ui.viewmodels.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VideoScanner @Inject constructor() {

    suspend fun findSimilarVideos(
        videos: List<VideoFile>,
        onProgress: (Int) -> Unit
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {

        if (videos.size < 2) return@withContext emptyList()

        // 提取每个视频的签名
        val signatures = mutableMapOf<String, VideoSignature>()
        videos.forEachIndexed { index, video ->
            try {
                val signature = extractHeaderSignature(video.path)
                signature?.let { signatures[video.path] = it }
            } catch (e: Exception) {
                // 跳过无法分析的视频
                e.printStackTrace()
            }
            onProgress(index + 1)
        }

        // 分组相似视频
        val groups = mutableListOf<DuplicateGroup>()
        val processed = mutableSetOf<String>()

        val signatureList = signatures.entries.toList()
        for (i in signatureList.indices) {
            val (path1, sig1) = signatureList[i]
            if (path1 in processed) continue

            val similar = mutableListOf(path1)
            for (j in i + 1 until signatureList.size) {
                val (path2, sig2) = signatureList[j]
                if (path2 in processed) continue

                if (areSimilar(sig1, sig2)) {
                    similar.add(path2)
                    processed.add(path2)
                }
            }

            if (similar.size > 1) {
                groups.add(DuplicateGroup(similar))
            }
            processed.add(path1)
        }

        // 按组大小降序排列
        groups.sortedByDescending { it.videos.size }
    }

    private fun extractHeaderSignature(videoPath: String): VideoSignature? {
        var extractor: MediaExtractor? = null
        try {
            extractor = MediaExtractor().apply {
                setDataSource(videoPath)
            }

            // 寻找视频轨道
            var videoTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    videoTrackIndex = i
                    break
                }
            }

            if (videoTrackIndex == -1) {
                return null
            }

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
                frameCount = keyFrameTimes.size
            )
        } catch (e: Exception) {
            return null
        } finally {
            extractor?.release()
        }
    }

    private fun areSimilar(sig1: VideoSignature, sig2: VideoSignature): Boolean {
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
        val frameCount: Int
    )
}