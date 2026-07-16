package com.linn.pawl.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.linn.pawl.data.local.IgnoredDuplicateGroupDao
import com.linn.pawl.data.local.ImageSignatureDao
import com.linn.pawl.data.local.PawlDatabase
import com.linn.pawl.data.local.TrashMediaDao
import com.linn.pawl.data.local.VideoSignatureDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE video_signatures ADD COLUMN md5 TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS image_signatures (
                    path TEXT NOT NULL PRIMARY KEY,
                    fileName TEXT NOT NULL,
                    lastModified INTEGER NOT NULL,
                    fileSize INTEGER NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    md5 TEXT NOT NULL,
                    dHash INTEGER NOT NULL,
                    computedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE image_signatures ADD COLUMN pHash INTEGER NOT NULL DEFAULT 0"
            )
            // Old dHash used a coarser, aspect-stretching algorithm ù?force recompute.
            db.execSQL("DELETE FROM image_signatures")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ignored_duplicate_groups (
                    mediaType TEXT NOT NULL,
                    groupKey TEXT NOT NULL,
                    memberPaths TEXT NOT NULL,
                    ignoredAt INTEGER NOT NULL,
                    PRIMARY KEY(mediaType, groupKey)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recycled_media (
                    id TEXT NOT NULL PRIMARY KEY,
                    mediaType TEXT NOT NULL,
                    originalMediaId INTEGER NOT NULL,
                    displayName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    sizeBytes INTEGER NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    durationMs INTEGER NOT NULL,
                    originalPath TEXT NOT NULL,
                    relativePath TEXT NOT NULL,
                    trashFileName TEXT NOT NULL,
                    dateTaken INTEGER NOT NULL,
                    recycledAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recycled_media RENAME TO trash_media")
        }
    }

    @Provides
    @Singleton
    fun providePawlDatabase(
        @ApplicationContext context: Context
    ): PawlDatabase {
        return Room.databaseBuilder(
            context,
            PawlDatabase::class.java,
            "pawl.db"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7
            )
            .build()
    }

    @Provides
    fun provideVideoSignatureDao(database: PawlDatabase): VideoSignatureDao {
        return database.videoSignatureDao()
    }

    @Provides
    fun provideImageSignatureDao(database: PawlDatabase): ImageSignatureDao {
        return database.imageSignatureDao()
    }

    @Provides
    fun provideIgnoredDuplicateGroupDao(database: PawlDatabase): IgnoredDuplicateGroupDao {
        return database.ignoredDuplicateGroupDao()
    }

    @Provides
    fun provideTrashMediaDao(database: PawlDatabase): TrashMediaDao {
        return database.trashMediaDao()
    }
}
