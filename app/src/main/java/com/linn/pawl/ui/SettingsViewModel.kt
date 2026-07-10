package com.linn.pawl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.data.repository.ImageSignatureRepository
import com.linn.pawl.data.repository.VideoSignatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val videoSignatureRepository: VideoSignatureRepository,
    private val imageSignatureRepository: ImageSignatureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun refreshCounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                videoFingerprintCount = videoSignatureRepository.getCount(),
                imageFingerprintCount = imageSignatureRepository.getCount()
            )
        }
    }

    data class UiState(
        val videoFingerprintCount: Int = 0,
        val imageFingerprintCount: Int = 0
    )
}
