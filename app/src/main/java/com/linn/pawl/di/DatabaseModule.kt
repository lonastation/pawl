package com.linn.pawl.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun providePawlDatabase(
        @ApplicationContext context: Context
    ): PawlDatabase {
        return Room.databaseBuilder(
            context,
            PawlDatabase::class.java,
            "pawl.db"
        ).build()
    }

    @Provides
    fun provideVideoSignatureDao(database: PawlDatabase): VideoSignatureDao {
        return database.videoSignatureDao()
    }
}
