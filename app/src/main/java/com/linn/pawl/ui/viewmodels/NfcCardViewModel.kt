package com.linn.pawl.ui.viewmodels

import NfcStatus
import android.app.Application
import android.content.ComponentName
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.cardemulation.CardEmulation
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.PawlApplication
import com.linn.pawl.data.CardRepository
import com.linn.pawl.data.NfcCardEntity
import com.linn.pawl.data.NfcLogEntity
import com.linn.pawl.model.NfcCard
import com.linn.pawl.service.YourHceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NfcCardViewModel(application: Application, private val cardRepository: CardRepository) :
    AndroidViewModel(application) {

    private val _isNfcAvailable = MutableStateFlow(false)

    private val _isReadingNewCard = MutableStateFlow(false)
    val isReadingNewCard: StateFlow<Boolean> = _isReadingNewCard.asStateFlow()

    private val _defaultCard = MutableStateFlow<NfcCard?>(null)
    val defaultCard: StateFlow<NfcCard?> = _defaultCard.asStateFlow()

    val hasDefaultCard = cardRepository.hasDefaultCard()

    val logs: StateFlow<List<NfcLogEntity>> = cardRepository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val nfcAdapter: NfcAdapter? by lazy {
        val nfcManager = getApplication<Application>().getSystemService(NfcManager::class.java)
        nfcManager?.defaultAdapter
    }

    private val _nfcStatus = MutableStateFlow<NfcStatus>(NfcStatus.DISABLED)
    val nfcStatus: StateFlow<NfcStatus> = _nfcStatus.asStateFlow()

    private val cardEmulationManager by lazy {
        getApplication<Application>().getSystemService(CardEmulation::class.java)
    }

    init {
        checkNfcAvailability()
        viewModelScope.launch {
            cardRepository.getDefaultCard().collect { entity ->
                _defaultCard.value = entity?.toNfcCard()
            }
        }
    }

    private fun checkNfcAvailability() {
        _isNfcAvailable.value = nfcAdapter != null && nfcAdapter?.isEnabled == true
    }

    fun updateNfcStatus(status: NfcStatus) {
        _nfcStatus.value = status
    }

    fun startReadingNewCard() {
        _isReadingNewCard.value = true
    }

    fun stopReadingNewCard() {
        _isReadingNewCard.value = false
    }

    fun handleNewNfcTag(tag: Tag?) {
        if (tag == null || !_isReadingNewCard.value) return

        val id = bytesToHexString(tag.id)
        val type = detectTagType(tag)
        val card = NfcCard(
            id = id ?: "Unknown",
            type = type,
            lastReadTime = System.currentTimeMillis(),
            description = getTagDescription(tag)
        )

        viewModelScope.launch {
            cardRepository.insertCard(NfcCardEntity.fromNfcCard(card))
            cardRepository.setDefaultCard(card.id)
            
            // Set as preferred HCE service
            cardEmulationManager?.let { manager ->
                val componentName = ComponentName(getApplication<Application>(), YourHceService::class.java)
                manager.setPreferredService(getApplication<PawlApplication>().currentActivity, componentName)
            }
            
            stopReadingNewCard()
        }
    }

    fun handleNfcTag(tag: Tag?) {
        if (tag == null) return

        val id = bytesToHexString(tag.id)
        viewModelScope.launch {
            if (id != null) {
                // Only log the tap event, don't change the default card
                cardRepository.insertLog(NfcLogEntity(
                    cardId = id,
                    cardType = "Card tapped",
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
    }

    private fun bytesToHexString(bytes: ByteArray?): String? {
        if (bytes == null) return null
        val sb = StringBuilder()
        for (b in bytes) {
            val hex = String.format("%02X", b)
            sb.append(hex)
        }
        return sb.toString()
    }

    private fun detectTagType(tag: Tag): String {
        val techList = tag.techList
        return when {
            techList.contains("android.nfc.tech.MifareClassic") -> "MIFARE Classic"
            techList.contains("android.nfc.tech.MifareUltralight") -> "MIFARE Ultralight"
            techList.contains("android.nfc.tech.IsoDep") -> "ISO-DEP"
            techList.contains("android.nfc.tech.NfcA") -> "NFC-A"
            techList.contains("android.nfc.tech.NfcB") -> "NFC-B"
            techList.contains("android.nfc.tech.NfcF") -> "NFC-F"
            techList.contains("android.nfc.tech.NfcV") -> "NFC-V"
            else -> "Unknown"
        }
    }

    private fun getTagDescription(tag: Tag): String {
        return "NFC Tag (${tag.techList.joinToString(", ")})"
    }
} 