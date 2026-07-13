package com.linn.pawl.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoSignatureEntity::class,
        ImageSignatureEntity::class,
        IgnoredDuplicateGroupEntity::class,
        RecycledMediaEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class PawlDatabase : RoomDatabase() {
    abstract fun videoSignatureDao(): VideoSignatureDao
    abstract fun imageSignatureDao(): ImageSignatureDao
    abstract fun ignoredDuplicateGroupDao(): IgnoredDuplicateGroupDao
    abstract fun recycledMediaDao(): RecycledMediaDao
}
