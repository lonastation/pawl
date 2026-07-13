package com.linn.pawl.data.repository

import com.linn.pawl.data.local.IgnoredDuplicateGroupDao
import com.linn.pawl.data.local.IgnoredDuplicateGroupEntity
import com.linn.pawl.data.model.DuplicateGroupKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IgnoredDuplicateGroupRepository @Inject constructor(
    private val dao: IgnoredDuplicateGroupDao
) {

    suspend fun ignore(mediaType: String, paths: Collection<String>) {
        if (paths.size < 2) return
        val groupKey = DuplicateGroupKey.fromPaths(mediaType, paths)
        dao.upsert(
            IgnoredDuplicateGroupEntity(
                mediaType = mediaType,
                groupKey = groupKey,
                memberPaths = DuplicateGroupKey.encodeMemberPaths(paths),
                ignoredAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getIgnoredKeys(mediaType: String): Set<String> =
        dao.getKeys(mediaType).toSet()

    /**
     * Drop ignore entries whose members are no longer all present in the library
     * (e.g. user deleted one file from an ignored pair).
     */
    suspend fun pruneStale(mediaType: String, currentPaths: Collection<String>) {
        val current = currentPaths.toSet()
        val staleKeys = dao.getAll(mediaType).mapNotNull { entity ->
            val members = DuplicateGroupKey.decodeMemberPaths(entity.memberPaths)
            if (members.isEmpty() || members.any { it !in current }) entity.groupKey else null
        }
        deleteKeys(mediaType, staleKeys)
    }

    /** Drop ignore entries that include any of the given paths. */
    suspend fun pruneContainingPaths(mediaType: String, paths: Collection<String>) {
        if (paths.isEmpty()) return
        val removed = paths.toSet()
        val staleKeys = dao.getAll(mediaType).mapNotNull { entity ->
            val members = DuplicateGroupKey.decodeMemberPaths(entity.memberPaths)
            if (members.any { it in removed }) entity.groupKey else null
        }
        deleteKeys(mediaType, staleKeys)
    }

    suspend fun clear(mediaType: String) {
        dao.deleteAll(mediaType)
    }

    suspend fun getCount(mediaType: String): Int = dao.count(mediaType)

    private suspend fun deleteKeys(mediaType: String, keys: List<String>) {
        if (keys.isEmpty()) return
        keys.chunked(SQLITE_BIND_ARG_LIMIT).forEach { chunk ->
            dao.deleteByKeys(mediaType, chunk)
        }
    }

    private companion object {
        const val SQLITE_BIND_ARG_LIMIT = 500
    }
}
