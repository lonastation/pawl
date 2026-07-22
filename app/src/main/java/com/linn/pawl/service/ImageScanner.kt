package com.linn.pawl.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.linn.pawl.R
import com.linn.pawl.data.model.ImageDuplicateGroup
import com.linn.pawl.data.model.ImageMedia
import com.linn.pawl.data.model.ImageSignature
import com.linn.pawl.data.model.ScanLogLevel
import com.linn.pawl.data.repository.ImageSignatureRepository
import com.linn.pawl.data.repository.ScanLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap

class ImageScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val signatureRepository: ImageSignatureRepository,
    private val scanLogRepository: ScanLogRepository
) {

    /** Max Hamming distance per hash (64-bit). Both dHash and pHash must pass. */
    private val maxHammingDistance = 6

    suspend fun findSimilarImages(
        images: List<ImageMedia>,
        onProgress: (Int) -> Unit
    ): List<ImageDuplicateGroup> = withContext(Dispatchers.IO) {
        if (images.size < 2) return@withContext emptyList()

        signatureRepository.deleteStale(images.map { it.path })
        val signatureCache = loadOrComputeSignatures(images, onProgress)

        if (signatureCache.size < 2) return@withContext emptyList()

        // GIFs only match GIFs; static images only match static images.
        // GIF fingerprints use the first frame (BitmapFactory.decodeFile).
        val (gifs, staticImages) = images.partition { it.isGif }
        val imageByPath = images.associateBy { it.path }

        findVisualSimilarGroups(signaturesFor(gifs, signatureCache), imageByPath) +
            findVisualSimilarGroups(signaturesFor(staticImages, signatureCache), imageByPath)
    }

    private fun signaturesFor(
        images: List<ImageMedia>,
        signatureCache: Map<String, ImageSignature>
    ): Map<String, ImageSignature> {
        if (images.size < 2) return emptyMap()
        val paths = images.mapTo(HashSet()) { it.path }
        return signatureCache.filterKeys { it in paths }
    }

    private suspend fun loadOrComputeSignatures(
        images: List<ImageMedia>,
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
        imageByPath: Map<String, ImageMedia>
    ): List<ImageDuplicateGroup> {
        val groups = mutableListOf<ImageDuplicateGroup>()
        val processed = mutableSetOf<String>()
        val signatureList = signatures.entries.toList()

        for (i in signatureList.indices) {
            val (path1, sig1) = signatureList[i]
            if (path1 in processed) continue

            val similar = mutableListOf<ImageMedia>()
            imageByPath[path1]?.let { similar.add(it) }

            for (j in i + 1 until signatureList.size) {
                val (path2, sig2) = signatureList[j]
                if (path2 in processed) continue

                if (areVisuallySimilar(sig1, sig2)) {
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

    private fun areVisuallySimilar(sig1: ImageSignature, sig2: ImageSignature): Boolean {
        return hammingDistance(sig1.dHash, sig2.dHash) <= maxHammingDistance &&
            hammingDistance(sig1.pHash, sig2.pHash) <= maxHammingDistance
    }

    private suspend fun extractSignature(image: ImageMedia): ImageSignature? {
        return try {
            val md5 = computeFileMd5(image.path) ?: return null
            // BitmapFactory decodes only the first frame for GIFs.
            val bitmap = decodeBitmap(image.path) ?: return null

            try {
                if (bitmap.width <= 0 || bitmap.height <= 0) return null
                ImageSignature(
                    md5 = md5,
                    dHash = computeDHash(bitmap),
                    pHash = computePHash(bitmap),
                    width = bitmap.width,
                    height = bitmap.height
                )
            } finally {
                bitmap.recycle()
            }
        } catch (t: Throwable) {
            scanLogRepository.append(
                mediaType = ScanLogRepository.MEDIA_IMAGE,
                level = ScanLogLevel.ERROR,
                message = context.getString(R.string.scan_log_image_signature_failed),
                path = image.path,
                throwable = t
            )
            null
        }
    }

    private fun decodeBitmap(path: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(path)
            }
            BitmapFactory.decodeFile(path, options)?.takeIf { it.width > 0 && it.height > 0 }
        } catch (_: Throwable) {
            null
        }
    }

    private fun calculateInSampleSize(path: String): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxDim <= 0) return 1
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
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Difference hash: aspect-preserving center-crop to 9×8, then adjacent-pixel gradients → 64 bits.
     * Intermediate 36×32 downsample reduces aliasing vs stretching straight to 9×8.
     */
    private fun computeDHash(bitmap: Bitmap): Long {
        val hashWidth = 9
        val hashHeight = 8
        val intermediate = scaleCenterCrop(bitmap, hashWidth * 4, hashHeight * 4)
        val scaled = intermediate.scale(hashWidth, hashHeight)
        if (intermediate !== bitmap && intermediate !== scaled) {
            intermediate.recycle()
        }

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

    /**
     * Perceptual hash: DCT of 32×32 grayscale, then low-frequency 8×8 coefficients vs median.
     */
    private fun computePHash(bitmap: Bitmap): Long {
        val size = 32
        val hashSize = 8
        val scaled = scaleCenterCrop(bitmap, size, size)

        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== bitmap) {
            scaled.recycle()
        }

        val gray = Array(size) { y ->
            DoubleArray(size) { x -> grayscale(pixels[y * size + x]).toDouble() }
        }
        val dct = dct2D(gray, size)

        val coeffs = DoubleArray(hashSize * hashSize)
        var idx = 0
        for (y in 0 until hashSize) {
            for (x in 0 until hashSize) {
                coeffs[idx++] = dct[y][x]
            }
        }

        // Median of AC coefficients (exclude DC at [0]).
        val ac = coeffs.copyOfRange(1, coeffs.size).sorted()
        val median = if (ac.size % 2 == 0) {
            (ac[ac.size / 2 - 1] + ac[ac.size / 2]) / 2.0
        } else {
            ac[ac.size / 2]
        }

        var hash = 0L
        for (i in 1 until coeffs.size) {
            if (coeffs[i] > median) {
                hash = hash or (1L shl (i - 1))
            }
        }
        return hash
    }

    /**
     * Aspect-preserving center-crop to [targetW×targetH].
     * Crops in source space first, then scales — avoids allocating huge bitmaps for
     * extreme aspect ratios (which previously could OOM and kill the scan).
     */
    private fun scaleCenterCrop(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
            return bitmap
        }
        if (bitmap.width <= 0 || bitmap.height <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return createBitmap(targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1))
        }

        val scale = max(
            targetWidth.toFloat() / bitmap.width,
            targetHeight.toFloat() / bitmap.height
        )
        val srcW = (targetWidth / scale).toInt().coerceIn(1, bitmap.width)
        val srcH = (targetHeight / scale).toInt().coerceIn(1, bitmap.height)
        val srcX = ((bitmap.width - srcW) / 2).coerceAtLeast(0)
        val srcY = ((bitmap.height - srcH) / 2).coerceAtLeast(0)
        // Clamp so createBitmap never exceeds source bounds after integer rounding.
        val safeW = min(srcW, bitmap.width - srcX)
        val safeH = min(srcH, bitmap.height - srcY)

        if (safeW <= 0 || safeH <= 0) {
            return createBitmap(targetWidth, targetHeight)
        }

        val cropped = Bitmap.createBitmap(bitmap, srcX, srcY, safeW, safeH)
        if (cropped.width == targetWidth && cropped.height == targetHeight) {
            return cropped
        }

        val scaled = cropped.scale(targetWidth, targetHeight)
        if (cropped !== bitmap && cropped !== scaled) {
            cropped.recycle()
        }
        return scaled
    }

    private fun dct2D(input: Array<DoubleArray>, n: Int): Array<DoubleArray> {
        val temp = Array(n) { DoubleArray(n) }
        val output = Array(n) { DoubleArray(n) }
        val rowOut = DoubleArray(n)
        val colIn = DoubleArray(n)
        val colOut = DoubleArray(n)

        for (y in 0 until n) {
            dct1D(input[y], rowOut, n)
            for (x in 0 until n) {
                temp[y][x] = rowOut[x]
            }
        }

        for (x in 0 until n) {
            for (y in 0 until n) {
                colIn[y] = temp[y][x]
            }
            dct1D(colIn, colOut, n)
            for (y in 0 until n) {
                output[y][x] = colOut[y]
            }
        }
        return output
    }

    private fun dct1D(input: DoubleArray, output: DoubleArray, n: Int) {
        val factor = PI / (2.0 * n)
        for (k in 0 until n) {
            var sum = 0.0
            for (i in 0 until n) {
                sum += input[i] * cos(factor * k * (2 * i + 1))
            }
            val alpha = if (k == 0) sqrt(1.0 / n) else sqrt(2.0 / n)
            output[k] = alpha * sum
        }
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
