package com.linn.pawl.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.data.model.DuplicateGroupKey
import com.linn.pawl.data.repository.IgnoredDuplicateGroupRepository
import com.linn.pawl.data.repository.ImageSignatureRepository
import com.linn.pawl.data.repository.RecycledMediaRepository
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
    private val imageSignatureRepository: ImageSignatureRepository,
    private val ignoredGroupRepository: IgnoredDuplicateGroupRepository,
    private val recycledMediaRepository: RecycledMediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun refreshCounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                videoFingerprintCount = videoSignatureRepository.getCount(),
                imageFingerprintCount = imageSignatureRepository.getCount(),
                videoIgnoredGroupCount = ignoredGroupRepository.getCount(DuplicateGroupKey.MEDIA_VIDEO),
                imageIgnoredGroupCount = ignoredGroupRepository.getCount(DuplicateGroupKey.MEDIA_IMAGE),
                hasAllFilesAccess = recycledMediaRepository.hasAllFilesAccess()
            )
        }
    }

    fun refreshAllFilesAccess() {
        _uiState.value = _uiState.value.copy(
            hasAllFilesAccess = recycledMediaRepository.hasAllFilesAccess()
        )
    }

    fun clearIgnoredVideoGroups() {
        viewModelScope.launch {
            ignoredGroupRepository.clear(DuplicateGroupKey.MEDIA_VIDEO)
            refreshCounts()
        }
    }

    fun clearIgnoredImageGroups() {
        viewModelScope.launch {
            ignoredGroupRepository.clear(DuplicateGroupKey.MEDIA_IMAGE)
            refreshCounts()
        }
    }

    data class UiState(
        val videoFingerprintCount: Int = 0,
        val imageFingerprintCount: Int = 0,
        val videoIgnoredGroupCount: Int = 0,
        val imageIgnoredGroupCount: Int = 0,
        val hasAllFilesAccess: Boolean = false
    )
}
