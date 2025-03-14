package com.linn.pawl.ui.viewmodels

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.data.CardRepository
import com.linn.pawl.data.NfcCardEntity
import com.linn.pawl.data.NfcLogEntity
import com.linn.pawl.model.NfcCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class NfcCardViewModel(application: Application, private val cardRepository: CardRepository) :
    AndroidViewModel(application) {

    private val _isNfcAvailable = MutableStateFlow(false)
    val isNfcAvailable: StateFlow<Boolean> = _isNfcAvailable.asStateFlow()

    val hasDefaultCard: StateFlow<Boolean> = cardRepository.hasDefaultCard()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val defaultCard: StateFlow<NfcCard?> = cardRepository.getDefaultCard()
        .map { it?.toNfcCard() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val cards: StateFlow<List<NfcCard>> = cardRepository.getAllCards()
        .map { entities -> entities.map { it.toNfcCard() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val logs: StateFlow<List<NfcLogEntity>> = cardRepository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val nfcAdapter: NfcAdapter? by lazy {
        val nfcManager = getApplication<Application>().getSystemService(NfcManager::class.java)
        nfcManager?.defaultAdapter
    }

    init {
        checkNfcAvailability()
    }

    private fun checkNfcAvailability() {
        _isNfcAvailable.value = nfcAdapter != null && nfcAdapter?.isEnabled == true
    }

    fun handleNfcTag(tag: Tag?) {
        if (tag == null) return

        val id = bytesToHexString(tag.id)
        val type = detectTagType(tag)
        val timestamp = System.currentTimeMillis()
        
        val card = NfcCard(
            id = id ?: "Unknown",
            type = type,
            lastReadTime = timestamp,
            description = getTagDescription(tag)
        )

        viewModelScope.launch {
            cardRepository.insertCard(NfcCardEntity.fromNfcCard(card))
            cardRepository.insertLog(
                NfcLogEntity(
                    cardId = card.id,
                    timestamp = timestamp,
                    cardType = card.type
                )
            )
        }
    }

    fun setDefaultCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.setDefaultCard(cardId)
        }
    }

    private fun detectTagType(tag: Tag): String {
        return when {
            IsoDep.get(tag) != null -> "ISO 14443-4"
            MifareClassic.get(tag) != null -> "MIFARE Classic"
            MifareUltralight.get(tag) != null -> "MIFARE Ultralight"
            Ndef.get(tag) != null -> "NDEF"
            NfcA.get(tag) != null -> "NFC-A"
            NfcB.get(tag) != null -> "NFC-B"
            NfcF.get(tag) != null -> "NFC-F"
            NfcV.get(tag) != null -> "NFC-V"
            else -> "Unknown"
        }
    }

    private fun getTagDescription(tag: Tag): String {
        val description = StringBuilder()

        try {
            when {
                MifareClassic.get(tag) != null -> {
                    val mfc = MifareClassic.get(tag)
                    description.append("Size: ${mfc?.size ?: 0} bytes")
                }

                MifareUltralight.get(tag) != null -> {
                    val mfu = MifareUltralight.get(tag)
                    description.append("Type: ${mfu?.type ?: "Unknown"}")
                }

                Ndef.get(tag) != null -> {
                    val ndef = Ndef.get(tag)
                    description.append("Max size: ${ndef?.maxSize ?: 0} bytes")
                }
            }
        } catch (e: IOException) {
            description.append("Error reading card details")
        }

        return description.toString()
    }

    private fun bytesToHexString(bytes: ByteArray?): String? {
        if (bytes == null) return null
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
} 