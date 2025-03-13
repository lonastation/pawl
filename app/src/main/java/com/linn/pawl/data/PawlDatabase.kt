package com.linn.pawl.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Card::class, Log::class],
    version = 1,
    exportSchema = false
)
abstract class PawlDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao

    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var Instance: PawlDatabase? = null
        fun getDatabase(context: Context): PawlDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, PawlDatabase::class.java, "pawl_database")
                    .build().also { Instance = it }
            }
        }
    }
}