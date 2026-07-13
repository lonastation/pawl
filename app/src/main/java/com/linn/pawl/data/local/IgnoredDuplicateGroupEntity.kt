package com.linn.pawl.data.local

import androidx.room.Entity

@Entity(
    tableName = "ignored_duplicate_groups",
    primaryKeys = ["mediaType", "groupKey"]
)
data class IgnoredDuplicateGroupEntity(
    val mediaType: String,
    val groupKey: String,
    /** Sorted member paths joined by '\n' — used to prune stale ignores. */
    val memberPaths: String,
    val ignoredAt: Long
)
