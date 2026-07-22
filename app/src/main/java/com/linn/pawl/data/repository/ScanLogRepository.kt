package com.linn.pawl.data.repository

import com.linn.pawl.data.local.ScanLogDao
import com.linn.pawl.data.local.ScanLogEntity
import com.linn.pawl.data.model.DuplicateGroupKey
import com.linn.pawl.data.model.ScanLogEntry
import com.linn.pawl.data.model.ScanLogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanLogRepository @Inject constructor(
    private val dao: ScanLogDao
) {

    fun observeLogs(): Flow<List<ScanLogEntry>> {
        return dao.observeAll().map { entities -> entities.map { it.toModel() } }
    }

    suspend fun append(
        mediaType: String,
        level: ScanLogLevel,
        message: String,
        path: String? = null,
        throwable: Throwable? = null
    ) {
        val detail = throwable?.let { formatThrowable(it) }
        dao.insert(
            ScanLogEntity(
                timestamp = System.currentTimeMillis(),
                mediaType = mediaType,
                level = level.name,
                path = path,
                message = message,
                detail = detail
            )
        )
        pruneIfNeeded()
    }

    suspend fun clear() {
        dao.deleteAll()
    }

    private suspend fun pruneIfNeeded() {
        if (dao.count() > MAX_ENTRIES) {
            val overflowIds = dao.getIdsBeyondLimit(MAX_ENTRIES)
            if (overflowIds.isNotEmpty()) {
                overflowIds.chunked(500).forEach { chunk ->
                    dao.deleteByIds(chunk)
                }
            }
        }
    }

    private fun ScanLogEntity.toModel(): ScanLogEntry {
        return ScanLogEntry(
            id = id,
            timestamp = timestamp,
            mediaType = mediaType,
            level = runCatching { ScanLogLevel.valueOf(level) }.getOrDefault(ScanLogLevel.ERROR),
            path = path,
            message = message,
            detail = detail
        )
    }

    private fun formatThrowable(throwable: Throwable): String {
        val stack = throwable.stackTraceToString()
        return if (stack.length <= MAX_DETAIL_CHARS) {
            stack
        } else {
            stack.take(MAX_DETAIL_CHARS) + "\n…(truncated)"
        }
    }

    companion object {
        const val MAX_ENTRIES = 500
        private const val MAX_DETAIL_CHARS = 4000

        const val MEDIA_IMAGE = DuplicateGroupKey.MEDIA_IMAGE
        const val MEDIA_VIDEO = DuplicateGroupKey.MEDIA_VIDEO
    }
}
