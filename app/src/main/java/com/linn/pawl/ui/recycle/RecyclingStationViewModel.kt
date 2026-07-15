package com.linn.pawl.ui.recycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.data.local.RecycledMediaEntity
import com.linn.pawl.data.model.DuplicateGroupKey
import com.linn.pawl.data.repository.RecycleCandidate
import com.linn.pawl.data.repository.RecycledMediaRepository
import com.linn.pawl.data.repository.StagedRecycleItem
import com.linn.pawl.ui.image.ImageFile
import com.linn.pawl.ui.video.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RecycleFilter {
    All,
    Images,
    Videos
}

@HiltViewModel
class RecyclingStationViewModel @Inject constructor(
    private val recycledMediaRepository: RecycledMediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pendingStaged: List<StagedRecycleItem> = emptyList()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val items = recycledMediaRepository.getAll()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    selectedIds = _uiState.value.selectedIds.intersect(items.map { it.id }.toSet()),
                    hasAllFilesAccess = recycledMediaRepository.hasAllFilesAccess()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load recycling station",
                    hasAllFilesAccess = recycledMediaRepository.hasAllFilesAccess()
                )
            }
        }
    }

    fun refreshAllFilesAccess() {
        _uiState.value = _uiState.value.copy(
            hasAllFilesAccess = recycledMediaRepository.hasAllFilesAccess()
        )
    }

    fun setFilter(filter: RecycleFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun toggleSelection(id: String) {
        val current = _uiState.value.selectedIds
        val updated = if (id in current) current - id else current + id
        _uiState.value = _uiState.value.copy(selectedIds = updated)
    }

    suspend fun stageImagesForRecycle(images: List<ImageFile>): List<StagedRecycleItem> {
        val staged = recycledMediaRepository.stage(
            images.map { image ->
                RecycleCandidate(
                    contentUri = image.contentUri,
                    mediaType = DuplicateGroupKey.MEDIA_IMAGE,
                    originalMediaId = image.mediaId,
                    displayName = image.name,
                    mimeType = image.mimeType,
                    sizeBytes = image.size,
                    width = image.width,
                    height = image.height,
                    durationMs = 0L,
                    originalPath = image.path,
                    dateTaken = image.dateCreated
                )
            }
        )
        pendingStaged = staged
        return staged
    }

    suspend fun stageVideosForRecycle(videos: List<VideoFile>): List<StagedRecycleItem> {
        val staged = recycledMediaRepository.stage(
            videos.map { video ->
                RecycleCandidate(
                    contentUri = video.contentUri,
                    mediaType = DuplicateGroupKey.MEDIA_VIDEO,
                    originalMediaId = video.mediaId,
                    displayName = video.name,
                    mimeType = "",
                    sizeBytes = video.size,
                    width = video.width,
                    height = video.height,
                    durationMs = video.duration,
                    originalPath = video.path,
                    dateTaken = video.dateCreated
                )
            }
        )
        pendingStaged = staged
        return staged
    }

    fun onMediaStoreDeleteConfirmed() {
        val staged = pendingStaged
        pendingStaged = emptyList()
        viewModelScope.launch {
            recycledMediaRepository.commit(staged)
            load()
        }
    }

    fun onMediaStoreDeleteCancelled() {
        val staged = pendingStaged
        pendingStaged = emptyList()
        viewModelScope.launch {
            recycledMediaRepository.abort(staged)
        }
    }

    fun restoreSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            try {
                recycledMediaRepository.restore(ids)
                _uiState.value = _uiState.value.copy(isBusy = false, selectedIds = emptySet())
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = e.message ?: "Failed to restore"
                )
            }
        }
    }

    fun permanentlyDeleteSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            try {
                recycledMediaRepository.permanentlyDelete(ids)
                _uiState.value = _uiState.value.copy(isBusy = false, selectedIds = emptySet())
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = e.message ?: "Failed to delete"
                )
            }
        }
    }

    fun permanentlyDeleteAll() {
        val ids = _uiState.value.items.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            try {
                recycledMediaRepository.permanentlyDelete(ids)
                _uiState.value = _uiState.value.copy(isBusy = false, selectedIds = emptySet())
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = e.message ?: "Failed to delete"
                )
            }
        }
    }

    fun trashFilePath(entity: RecycledMediaEntity): String =
        recycledMediaRepository.trashFileFor(entity).absolutePath

    data class UiState(
        val isLoading: Boolean = false,
        val isBusy: Boolean = false,
        val items: List<RecycledMediaEntity> = emptyList(),
        val filter: RecycleFilter = RecycleFilter.All,
        val selectedIds: Set<String> = emptySet(),
        val errorMessage: String? = null,
        val hasAllFilesAccess: Boolean = false
    ) {
        val images: List<RecycledMediaEntity>
            get() = items.filter { it.mediaType == DuplicateGroupKey.MEDIA_IMAGE }

        val videos: List<RecycledMediaEntity>
            get() = items.filter { it.mediaType == DuplicateGroupKey.MEDIA_VIDEO }

        val visibleItems: List<RecycledMediaEntity>
            get() = when (filter) {
                RecycleFilter.All -> items
                RecycleFilter.Images -> images
                RecycleFilter.Videos -> videos
            }
    }
}
