package com.linn.pawl.ui.trash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.R
import com.linn.pawl.data.local.TrashMediaEntity
import com.linn.pawl.data.model.DuplicateGroupKey
import com.linn.pawl.data.model.ImageMedia
import com.linn.pawl.data.model.VideoMedia
import com.linn.pawl.data.repository.TrashCandidate
import com.linn.pawl.data.repository.TrashMediaRepository
import com.linn.pawl.data.repository.StagedTrashItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TrashFilter {
    All,
    Images,
    Videos
}

@HiltViewModel
class TrashViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val trashMediaRepository: TrashMediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pendingStaged: List<StagedTrashItem> = emptyList()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val items = trashMediaRepository.getAll()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    selectedIds = _uiState.value.selectedIds.intersect(items.map { it.id }.toSet()),
                    hasAllFilesAccess = trashMediaRepository.hasAllFilesAccess()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: context.getString(R.string.trash_load_failed),
                    hasAllFilesAccess = trashMediaRepository.hasAllFilesAccess()
                )
            }
        }
    }

    fun refreshAllFilesAccess() {
        _uiState.value = _uiState.value.copy(
            hasAllFilesAccess = trashMediaRepository.hasAllFilesAccess()
        )
    }

    fun setFilter(filter: TrashFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun toggleSelection(id: String) {
        val current = _uiState.value.selectedIds
        val updated = if (id in current) current - id else current + id
        _uiState.value = _uiState.value.copy(selectedIds = updated)
    }

    suspend fun stageImagesForTrash(images: List<ImageMedia>): List<StagedTrashItem> {
        val staged = trashMediaRepository.stage(
            images.map { image ->
                TrashCandidate(
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

    suspend fun stageVideosForTrash(videos: List<VideoMedia>): List<StagedTrashItem> {
        val staged = trashMediaRepository.stage(
            videos.map { video ->
                TrashCandidate(
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
            trashMediaRepository.commit(staged)
            load()
        }
    }

    fun onMediaStoreDeleteCancelled() {
        val staged = pendingStaged
        pendingStaged = emptyList()
        viewModelScope.launch {
            trashMediaRepository.abort(staged)
        }
    }

    fun restoreSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            try {
                trashMediaRepository.restore(ids)
                _uiState.value = _uiState.value.copy(isBusy = false, selectedIds = emptySet())
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = e.message ?: context.getString(R.string.trash_restore_failed)
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
                trashMediaRepository.permanentlyDelete(ids)
                _uiState.value = _uiState.value.copy(isBusy = false, selectedIds = emptySet())
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = e.message ?: context.getString(R.string.trash_delete_failed)
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
                trashMediaRepository.permanentlyDelete(ids)
                _uiState.value = _uiState.value.copy(isBusy = false, selectedIds = emptySet())
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = e.message ?: context.getString(R.string.trash_delete_failed)
                )
            }
        }
    }

    fun trashFilePath(entity: TrashMediaEntity): String =
        trashMediaRepository.trashFileFor(entity).absolutePath

    data class UiState(
        val isLoading: Boolean = false,
        val isBusy: Boolean = false,
        val items: List<TrashMediaEntity> = emptyList(),
        val filter: TrashFilter = TrashFilter.All,
        val selectedIds: Set<String> = emptySet(),
        val errorMessage: String? = null,
        val hasAllFilesAccess: Boolean = false
    ) {
        val images: List<TrashMediaEntity>
            get() = items.filter { it.mediaType == DuplicateGroupKey.MEDIA_IMAGE }

        val videos: List<TrashMediaEntity>
            get() = items.filter { it.mediaType == DuplicateGroupKey.MEDIA_VIDEO }

        val visibleItems: List<TrashMediaEntity>
            get() = when (filter) {
                TrashFilter.All -> items
                TrashFilter.Images -> images
                TrashFilter.Videos -> videos
            }
    }
}
