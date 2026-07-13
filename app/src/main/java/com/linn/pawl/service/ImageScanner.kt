package com.linn.pawl.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.linn.pawl.data.model.ImageSignature
import com.linn.pawl.data.repository.ImageSignatureRepository
import com.linn.pawl.ui.image.ImageDuplicateGroup
import com.linn.pawl.ui.image.ImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject

class ImageScanner @Inject constructor(
    private val signatureRepository: ImageSignatureRepository
) {

    private val maxHammingDistance = 10

    suspend fun findSimilarImages(
        images: List<ImageFile>,
        onProgress: (Int) -> Unit
    ): List<ImageDuplicateGroup> = withContext(Dispatchers.IO) {
        if (images.size < 2) return@withContext emptyList()

        signatureRepository.deleteStale(images.map { it.path })
        val signatureCache = loadOrComputeSignatures(images, onProgress)

        if (signatureCache.size < 2) return@withContext emptyList()

        findVisualSimilarGroups(signatureCache, images.associateBy { it.path })
    }

    private suspend fun loadOrComputeSignatures(
        images: List<ImageFile>,
        onProgress: (Int) -> Unit
    ): Map<String, ImageSignature> {
        val signatures = signatureRepository.getCachedBatch(images).toMutableMap()
        var processedCount = 0

        for (image in images) {
            if (image.path !in signatures) {
                val signature = extractSignature(image)
                if (signature != null) {
                    signatureRepository.save(image, signature)
                    signatures[image.path] = signature
                }
            }
            processedCount++
            onProgress(processedCount)
        }

        return signatures
    }

    private fun findVisualSimilarGroups(
        signatures: Map<String, ImageSignature>,
        imageByPath: Map<String, ImageFile>
    ): List<ImageDuplicateGroup> {
        val groups = mutableListOf<ImageDuplicateGroup>()
        val processed = mutableSetOf<String>()
        val signatureList = signatures.entries.toList()

        for (i in signatureList.indices) {
            val (path1, sig1) = signatureList[i]
            if (path1 in processed) continue

            val similar = mutableListOf<ImageFile>()
            imageByPath[path1]?.let { similar.add(it) }

            for (j in i + 1 until signatureList.size) {
                val (path2, sig2) = signatureList[j]
                if (path2 in processed) continue

                if (hammingDistance(sig1.dHash, sig2.dHash) <= maxHammingDistance) {
                    imageByPath[path2]?.let { similar.add(it) }
                    processed.add(path2)
                }
            }

            if (similar.size > 1) {
                groups.add(ImageDuplicateGroup(similar))
            }
            processed.add(path1)
        }

        return groups
    }

    private fun extractSignature(image: ImageFile): ImageSignature? {
        val md5 = computeFileMd5(image.path) ?: return null
        val bitmap = decodeBitmap(image.path) ?: return null

        return try {
            val dHash = computeDHash(bitmap)
            ImageSignature(
                md5 = md5,
                dHash = dHash,
                width = bitmap.width,
                height = bitmap.height
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeBitmap(path: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(path)
            }
            BitmapFactory.decodeFile(path, options)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(path: String): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (maxDim / sampleSize > 512) {
            sampleSize *= 2
        }
        return sampleSize
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

    private fun computeDHash(bitmap: Bitmap): Long {
        val hashWidth = 9
        val hashHeight = 8
        val scaled = bitmap.scale(hashWidth, hashHeight)

        val pixels = IntArray(hashWidth * hashHeight)
        scaled.getPixels(pixels, 0, hashWidth, 0, 0, hashWidth, hashHeight)
        if (scaled !== bitmap) {
            scaled.recycle()
        }

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
}
