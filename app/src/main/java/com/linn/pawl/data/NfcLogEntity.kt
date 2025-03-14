package com.linn.pawl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nfc_logs")
data class NfcLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cardId: String,
    val timestamp: Long,
    val cardType: String
) 