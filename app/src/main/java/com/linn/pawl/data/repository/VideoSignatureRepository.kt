package com.linn.pawl.data.repository

import com.linn.pawl.data.local.VideoSignatureDao
import com.linn.pawl.data.local.VideoSignatureEntity
import com.linn.pawl.data.model.VideoSignature
import com.linn.pawl.ui.VideoFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoSignatureRepository @Inject constructor(
    private val dao: VideoSignatureDao
) {

    suspend fun getCached(video: VideoFile): VideoSignature? {
        val entity = dao.getByPath(video.path) ?: return null
        if (!isCacheValid(entity, video)) return null
        return entity.toModel()
    }

    suspend fun getCachedBatch(videos: List<VideoFile>): Map<String, VideoSignature> {
        if (videos.isEmpty()) return emptyMap()

        val videoByPath = videos.associateBy { it.path }
        val entities = videos.map { it.path }
            .chunked(SQLITE_BIND_ARG_LIMIT)
            .flatMap { chunk -> dao.getByPaths(chunk) }

        return entities.mapNotNull { entity ->
            val video = videoByPath[entity.path] ?: return@mapNotNull null
            if (!isCacheValid(entity, video)) return@mapNotNull null
            entity.path to entity.toModel()
        }.toMap()
    }

    suspend fun save(video: VideoFile, signature: VideoSignature) {
        dao.upsert(video.toEntity(signature))
    }

    suspend fun deleteStale(currentPaths: Collection<String>) {
        val current = currentPaths.toSet()
        val stalePaths = dao.getAllPaths().filter { it !in current }
        stalePaths.chunked(SQLITE_BIND_ARG_LIMIT).forEach { chunk ->
            dao.deleteByPaths(chunk)
        }
    }

    suspend fun deleteByPaths(paths: Collection<String>) {
        if (paths.isEmpty()) return
        paths.chunked(SQLITE_BIND_ARG_LIMIT).forEach { chunk ->
            dao.deleteByPaths(chunk)
        }
    }

    private fun isCacheValid(entity: VideoSignatureEntity, video: VideoFile): Boolean {
        return entity.fileName == video.name &&
            entity.lastModified == video.lastModified &&
            entity.fileSize == video.size
    }

    private fun VideoSignatureEntity.toModel(): VideoSignature {
        return VideoSignature(
            frameHashes = frameHashes.split(',').map { it.toLong() },
            width = width,
            height = height
        )
    }

    private fun VideoFile.toEntity(signature: VideoSignature): VideoSignatureEntity {
        return VideoSignatureEntity(
            path = path,
            fileName = name,
            lastModified = lastModified,
            fileSize = size,
            width = signature.width,
            height = signature.height,
            frameHashes = signature.frameHashes.joinToString(","),
            computedAt = System.currentTimeMillis()
        )
    }

    private companion object {
        // SQLite default bind-arg limit on Android
        const val SQLITE_BIND_ARG_LIMIT = 500
    }
}
