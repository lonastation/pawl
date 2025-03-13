package com.linn.pawl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log")
data class Log (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val card: String,
    val year: Int = 0,
    val month: Int = 0,
    val day: Int= 0,
    val datetime: String,
)