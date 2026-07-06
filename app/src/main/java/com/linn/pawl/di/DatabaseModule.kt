package com.linn.pawl.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.linn.pawl.data.local.PawlDatabase
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
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideVideoSignatureDao(database: PawlDatabase): VideoSignatureDao {
        return database.videoSignatureDao()
    }
}
