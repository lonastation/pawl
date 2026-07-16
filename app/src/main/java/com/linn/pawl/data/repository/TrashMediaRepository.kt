package com.linn.pawl.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.linn.pawl.data.local.TrashMediaDao
import com.linn.pawl.data.local.TrashMediaEntity
import com.linn.pawl.data.model.DuplicateGroupKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TrashCandidate(
    val contentUri: Uri,
    val mediaType: String,
    val originalMediaId: Long,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val originalPath: String,
    val dateTaken: Long
)

data class StagedTrashItem(
    val entity: TrashMediaEntity,
    val contentUri: Uri
)

@Singleton
class TrashMediaRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: TrashMediaDao
) {

    private val trashDir: File
        get() = File(context.filesDir, TRASH_DIR_NAME).also { it.mkdirs() }

    fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()

    suspend fun getAll(): List<TrashMediaEntity> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun stage(candidates: List<TrashCandidate>): List<StagedTrashItem> =
        withContext(Dispatchers.IO) {
            val staged = mutableListOf<StagedTrashItem>()
            try {
                candidates.forEach { candidate ->
                    staged += stageCopy(
                        contentUri = candidate.contentUri,
                        mediaType = candidate.mediaType,
                        originalMediaId = candidate.originalMediaId,
                        displayName = candidate.displayName,
                        mimeType = candidate.mimeType.ifBlank {
                            guessMimeType(candidate.displayName, candidate.mediaType)
                        },
                        sizeBytes = candidate.sizeBytes,
                        width = candidate.width,
                        height = candidate.height,
                        durationMs = candidate.durationMs,
                        originalPath = candidate.originalPath,
                        dateTaken = candidate.dateTaken
                    )
                }
                staged
            } catch (e: Exception) {
                staged.forEach { item ->
                    File(trashDir, item.entity.trashFileName).delete()
                }
                throw e
            }
        }

    suspend fun commit(staged: List<StagedTrashItem>) = withContext(Dispatchers.IO) {
        if (staged.isEmpty()) return@withContext
        dao.upsertAll(staged.map { it.entity })
    }

    suspend fun abort(staged: List<StagedTrashItem>) = withContext(Dispatchers.IO) {
        staged.forEach { item ->
            File(trashDir, item.entity.trashFileName).delete()
        }
    }

    suspend fun restore(ids: Collection<String>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val entities = dao.getByIds(ids.toList())
        val canWriteAnywhere = hasAllFilesAccess()
        entities.forEach { entity ->
            if (canWriteAnywhere && entity.originalPath.isNotBlank()) {
                restoreToOriginalPath(entity)
            } else {
                restoreToMediaStore(entity)
            }
            File(trashDir, entity.trashFileName).delete()
        }
        deleteEntities(entities.map { it.id })
    }

    suspend fun permanentlyDelete(ids: Collection<String>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val entities = dao.getByIds(ids.toList())
        entities.forEach { entity ->
            File(trashDir, entity.trashFileName).delete()
        }
        deleteEntities(entities.map { it.id })
    }

    /** Permanently removes trash items older than [retentionMillis]. */
    suspend fun purgeExpired(
        nowMillis: Long = System.currentTimeMillis(),
        retentionMillis: Long = RETENTION_MILLIS
    ): Int = withContext(Dispatchers.IO) {
        val expired = dao.getOlderThan(nowMillis - retentionMillis)
        if (expired.isEmpty()) return@withContext 0
        expired.forEach { entity ->
            File(trashDir, entity.trashFileName).delete()
        }
        deleteEntities(expired.map { it.id })
        expired.size
    }

    fun trashFileFor(entity: TrashMediaEntity): File =
        File(trashDir, entity.trashFileName)

    private fun stageCopy(
        contentUri: Uri,
        mediaType: String,
        originalMediaId: Long,
        displayName: String,
        mimeType: String,
        sizeBytes: Long,
        width: Int,
        height: Int,
        durationMs: Long,
        originalPath: String,
        dateTaken: Long
    ): StagedTrashItem {
        val id = UUID.randomUUID().toString()
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
        val trashFileName = if (extension.isNotEmpty()) "$id.$extension" else id
        val trashFile = File(trashDir, trashFileName)

        context.contentResolver.openInputStream(contentUri)?.use { input ->
            trashFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to read media: $displayName")

        val entity = TrashMediaEntity(
            id = id,
            mediaType = mediaType,
            originalMediaId = originalMediaId,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            durationMs = durationMs,
            originalPath = originalPath,
            relativePath = relativePathFromAbsolute(originalPath, mediaType),
            trashFileName = trashFileName,
            dateTaken = dateTaken,
            recycledAt = System.currentTimeMillis()
        )
        return StagedTrashItem(entity = entity, contentUri = contentUri)
    }

    private fun restoreToOriginalPath(entity: TrashMediaEntity) {
        val trashFile = File(trashDir, entity.trashFileName)
        if (!trashFile.exists()) error("Trash file missing: ${entity.displayName}")

        val dest = File(entity.originalPath)
        val parent = dest.parentFile
            ?: error("Invalid original path: ${entity.originalPath}")
        if (!parent.exists() && !parent.mkdirs()) {
            error("Unable to create folder: ${parent.absolutePath}")
        }

        trashFile.inputStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }

        val mime = entity.mimeType.ifBlank { null }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(dest.absolutePath),
            arrayOf(mime),
            null
        )
    }

    private fun restoreToMediaStore(entity: TrashMediaEntity) {
        val trashFile = File(trashDir, entity.trashFileName)
        if (!trashFile.exists()) error("Trash file missing: ${entity.displayName}")

        val isImage = entity.mediaType == DuplicateGroupKey.MEDIA_IMAGE
        val collection = if (isImage) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, entity.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, entity.mimeType)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                allowedRelativePath(entity.relativePath, entity.mediaType)
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (entity.dateTaken > 0L) {
                put(MediaStore.MediaColumns.DATE_TAKEN, entity.dateTaken)
            }
            if (entity.width > 0) put(MediaStore.MediaColumns.WIDTH, entity.width)
            if (entity.height > 0) put(MediaStore.MediaColumns.HEIGHT, entity.height)
            if (!isImage && entity.durationMs > 0L) {
                put(MediaStore.MediaColumns.DURATION, entity.durationMs)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)
            ?: error("Failed to restore ${entity.displayName}")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                trashFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Failed to write restored file: ${entity.displayName}")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private suspend fun deleteEntities(ids: List<String>) {
        if (ids.isEmpty()) return
        ids.chunked(SQLITE_BIND_ARG_LIMIT).forEach { chunk ->
            dao.deleteByIds(chunk)
        }
    }

    companion object {
        private const val TRASH_DIR_NAME = "recycle_bin"
        private const val SQLITE_BIND_ARG_LIMIT = 500
        private const val RETENTION_DAYS = 3L
        val RETENTION_MILLIS: Long = TimeUnit.DAYS.toMillis(RETENTION_DAYS)

        private val ALLOWED_IMAGE_DIRS = setOf("DCIM", "Pictures")
        private val ALLOWED_VIDEO_DIRS = setOf("DCIM", "Movies", "Pictures")

        fun relativePathFromAbsolute(path: String, mediaType: String): String {
            val normalized = path.replace('\\', '/')
            val prefixes = listOf(
                "/storage/emulated/0/",
                "/storage/emulated/1/",
                "/sdcard/"
            )
            var relative = normalized
            for (prefix in prefixes) {
                val index = normalized.indexOf(prefix)
                if (index >= 0) {
                    relative = normalized.substring(index + prefix.length)
                    break
                }
            }
            val lastSlash = relative.lastIndexOf('/')
            val folder = if (lastSlash >= 0) relative.substring(0, lastSlash + 1) else ""
            if (folder.isNotBlank()) return folder
            return if (mediaType == DuplicateGroupKey.MEDIA_IMAGE) {
                "Pictures/PawlRestore/"
            } else {
                "Movies/PawlRestore/"
            }
        }

        /** MediaStore only allows certain primary directories for Images/Video inserts. */
        fun allowedRelativePath(relativePath: String, mediaType: String): String {
            val normalized = relativePath.replace('\\', '/').trim('/')
            val primary = normalized.substringBefore('/', missingDelimiterValue = normalized)
            val isImage = mediaType == DuplicateGroupKey.MEDIA_IMAGE
            val allowed = if (isImage) ALLOWED_IMAGE_DIRS else ALLOWED_VIDEO_DIRS
            if (primary in allowed && normalized.isNotEmpty()) {
                return "$normalized/"
            }
            return if (isImage) "Pictures/PawlRestore/" else "Movies/PawlRestore/"
        }

        fun guessMimeType(displayName: String, mediaType: String): String {
            val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
                .lowercase()
            val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (!fromMap.isNullOrBlank()) return fromMap
            return if (mediaType == DuplicateGroupKey.MEDIA_IMAGE) "image/jpeg" else "video/mp4"
        }
    }
}
