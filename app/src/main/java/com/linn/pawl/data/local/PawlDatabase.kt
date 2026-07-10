package com.linn.pawl.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VideoSignatureEntity::class, ImageSignatureEntity::class],
    version = 3,
    exportSchema = false
)
abstract class PawlDatabase : RoomDatabase() {
    abstract fun videoSignatureDao(): VideoSignatureDao
    abstract fun imageSignatureDao(): ImageSignatureDao
}
