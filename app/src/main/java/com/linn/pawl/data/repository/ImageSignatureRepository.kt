package com.linn.pawl.data.repository

import com.linn.pawl.data.local.ImageSignatureDao
import com.linn.pawl.data.local.ImageSignatureEntity
import com.linn.pawl.data.model.ImageMedia
import com.linn.pawl.data.model.ImageSignature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageSignatureRepository @Inject constructor(
    private val dao: ImageSignatureDao
) {

    suspend fun getCachedBatch(images: List<ImageMedia>): Map<String, ImageSignature> {
        if (images.isEmpty()) return emptyMap()

        val imageByPath = images.associateBy { it.path }
        val entities = images.map { it.path }
            .chunked(SQLITE_BIND_ARG_LIMIT)
            .flatMap { chunk -> dao.getByPaths(chunk) }

        return entities.mapNotNull { entity ->
            val image = imageByPath[entity.path] ?: return@mapNotNull null
            if (!isCacheValid(entity, image)) return@mapNotNull null
            entity.path to entity.toModel()
        }.toMap()
    }

    suspend fun save(image: ImageMedia, signature: ImageSignature) {
        dao.upsert(image.toEntity(signature))
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

    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun getCount(): Int = dao.count()

    private fun isCacheValid(entity: ImageSignatureEntity, image: ImageMedia): Boolean {
        return entity.md5.isNotEmpty() &&
            entity.fileName == image.name &&
            entity.lastModified == image.lastModified &&
            entity.fileSize == image.size
    }

    private fun ImageSignatureEntity.toModel(): ImageSignature {
        return ImageSignature(
            md5 = md5,
            dHash = dHash,
            pHash = pHash,
            width = width,
            height = height
        )
    }

    private fun ImageMedia.toEntity(signature: ImageSignature): ImageSignatureEntity {
        return ImageSignatureEntity(
            path = path,
            fileName = name,
            lastModified = lastModified,
            fileSize = size,
            width = signature.width,
            height = signature.height,
            md5 = signature.md5,
            dHash = signature.dHash,
            pHash = signature.pHash,
            computedAt = System.currentTimeMillis()
        )
    }

    private companion object {
        const val SQLITE_BIND_ARG_LIMIT = 500
    }
}
