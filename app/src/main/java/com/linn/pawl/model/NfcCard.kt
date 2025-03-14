package com.linn.pawl.model

data class NfcCard(
    val id: String,
    val type: String,
    val lastReadTime: Long,
    val description: String = ""
) 