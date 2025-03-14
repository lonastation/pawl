package com.linn.pawl.ui.viewmodels

import NfcStatus
import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.data.CardRepository
import com.linn.pawl.data.NfcCardEntity
import com.linn.pawl.data.NfcLogEntity
import com.linn.pawl.model.NfcCard
import com.linn.pawl.nfc.NfcCardReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.cardemulation.CardEmulation

class NfcCardViewModel(application: Application, private val cardRepository: CardRepository) :
    AndroidViewModel(application) {

    private val _isNfcAvailable = MutableStateFlow(false)

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

    private val _nfcStatus = MutableStateFlow<NfcStatus>(NfcStatus.DISABLED)
    val nfcStatus: StateFlow<NfcStatus> = _nfcStatus.asStateFlow()

    private val nfcCardReader = NfcCardReader(application)
    private val _isDiscovering = MutableStateFlow(false)

    init {
        checkNfcAvailability()
        loadStoredCards()
    }

    private fun checkNfcAvailability() {
        _isNfcAvailable.value = nfcAdapter != null && nfcAdapter?.isEnabled == true
    }

    private fun loadStoredCards() {
        viewModelScope.launch {
            try {
                val storedCards = nfcCardReader.getStoredCards()
                storedCards.forEach { card ->
                    cardRepository.insertCard(NfcCardEntity.fromNfcCard(card))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDefaultCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.setDefaultCard(cardId)
        }
    }

    fun updateNfcStatus(status: NfcStatus) {
        _nfcStatus.value = status
    }
} 