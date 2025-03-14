package com.linn.pawl.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.linn.pawl.model.NfcCard

@Entity(tableName = "nfc_cards")
data class NfcCardEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val lastReadTime: Long,
    val description: String,
    val isDefault: Boolean = false
) {
    fun toNfcCard() = NfcCard(
        id = id,
        type = type,
        lastReadTime = lastReadTime,
        description = description
    )

    companion object {
        fun fromNfcCard(card: NfcCard, isDefault: Boolean = false) = NfcCardEntity(
            id = card.id,
            type = card.type,
            lastReadTime = card.lastReadTime,
            description = card.description,
            isDefault = isDefault
        )
    }
} 