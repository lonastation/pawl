package com.linn.pawl.ui.video

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.data.model.DuplicateGroupKey
import com.linn.pawl.data.repository.IgnoredDuplicateGroupRepository
import com.linn.pawl.data.repository.VideoSignatureRepository
import com.linn.pawl.service.VideoScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoScannerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val videoScanner: VideoScanner,
    private val signatureRepository: VideoSignatureRepository,
    private val ignoredGroupRepository: IgnoredDuplicateGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun startScan() {
        runScan(clearFingerprints = false)
    }

    fun regenerateFingerprintsAndScan() {
        runScan(clearFingerprints = true)
    }

    private fun runScan(clearFingerprints: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                duplicateGroups = emptyList(),
                totalVideos = 0,
                scannedCount = 0,
                totalDuplicates = 0,
                selectedVideoIds = emptySet()
            )

            try {
                if (clearFingerprints) {
                    signatureRepository.clearAll()
                }

                val allVideos = withContext(Dispatchers.IO) { getAllVideos() }
                _uiState.value = _uiState.value.copy(totalVideos = allVideos.size)

                val groups = videoScanner.findDuplicateVideos(
                    videos = allVideos,
                    onProgress = { scanned ->
                        _uiState.value = _uiState.value.copy(scannedCount = scanned)
                    }
                )

                ignoredGroupRepository.pruneStale(
                    DuplicateGroupKey.MEDIA_VIDEO,
                    allVideos.map { it.path }
                )
                val visibleGroups = filterIgnoredGroups(groups)

                val totalDuplicates = visibleGroups.sumOf { it.videos.size } - visibleGroups.size

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    duplicateGroups = visibleGroups,
                    totalDuplicates = totalDuplicates
                )
            } catch (_: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    duplicateGroups = emptyList(),
                    totalDuplicates = 0
                )
            }
        }
    }

    private suspend fun filterIgnoredGroups(
        groups: List<DuplicateGroup>
    ): List<DuplicateGroup> {
        val ignoredKeys = ignoredGroupRepository.getIgnoredKeys(DuplicateGroupKey.MEDIA_VIDEO)
        if (ignoredKeys.isEmpty()) return groups
        return groups.filter { group ->
            DuplicateGroupKey.fromPaths(
                DuplicateGroupKey.MEDIA_VIDEO,
                group.videos.map { it.path }
            ) !in ignoredKeys
        }
    }

    private fun getAllVideos(): List<VideoFile> {
        val videos = mutableListOf<VideoFile>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                val path = it.getString(dataColumn) ?: continue
                val file = File(path)
                if (file.exists()) {
                    val mediaId = it.getLong(idColumn)
                    val mediaStoreModified = it.getLong(dateModifiedColumn) * 1000L
                    videos.add(
                        VideoFile(
                            mediaId = mediaId,
                            contentUri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                mediaId
                            ),
                            path = path,
                            name = it.getString(nameColumn) ?: "unknown",
                            size = it.getLong(sizeColumn),
                            duration = it.getLong(durationColumn),
                            width = it.getInt(widthColumn),
                            height = it.getInt(heightColumn),
                            dateCreated = it.getLong(dateAddedColumn) * 1000L,
                            lastModified = maxOf(file.lastModified(), mediaStoreModified)
                        )
                    )
                }
            }
        }

        return videos
    }

    fun toggleVideoSelection(mediaId: Long) {
        val current = _uiState.value.selectedVideoIds
        val updated = if (mediaId in current) current - mediaId else current + mediaId
        _uiState.value = _uiState.value.copy(selectedVideoIds = updated)
    }

    fun getSelectedVideoUris(): List<Uri> {
        val selected = _uiState.value.selectedVideoIds
        return _uiState.value.duplicateGroups
            .flatMap { it.videos }
            .filter { it.mediaId in selected }
            .map { it.contentUri }
    }

    fun ignoreGroup(group: DuplicateGroup) {
        viewModelScope.launch {
            ignoredGroupRepository.ignore(
                DuplicateGroupKey.MEDIA_VIDEO,
                group.videos.map { it.path }
            )
            val groupKey = DuplicateGroupKey.fromPaths(
                DuplicateGroupKey.MEDIA_VIDEO,
                group.videos.map { it.path }
            )
            val removedIds = group.videos.map { it.mediaId }.toSet()
            val updatedGroups = _uiState.value.duplicateGroups.filter { existing ->
                DuplicateGroupKey.fromPaths(
                    DuplicateGroupKey.MEDIA_VIDEO,
                    existing.videos.map { it.path }
                ) != groupKey
            }
            val totalDuplicates = updatedGroups.sumOf { it.videos.size } - updatedGroups.size
            _uiState.value = _uiState.value.copy(
                duplicateGroups = updatedGroups,
                totalDuplicates = totalDuplicates,
                selectedVideoIds = _uiState.value.selectedVideoIds - removedIds
            )
        }
    }

    fun onVideosDeleted(deletedIds: Set<Long>) {
        val deletedPaths = _uiState.value.duplicateGroups
            .flatMap { it.videos }
            .filter { it.mediaId in deletedIds }
            .map { it.path }

        viewModelScope.launch {
            signatureRepository.deleteByPaths(deletedPaths)
            ignoredGroupRepository.pruneContainingPaths(
                DuplicateGroupKey.MEDIA_VIDEO,
                deletedPaths
            )
        }

        val updatedGroups = _uiState.value.duplicateGroups.mapNotNull { group ->
            val remaining = group.videos.filter { it.mediaId !in deletedIds }
            if (remaining.size >= 2) DuplicateGroup(remaining) else null
        }
        val totalDuplicates = updatedGroups.sumOf { it.videos.size } - updatedGroups.size
        _uiState.value = _uiState.value.copy(
            duplicateGroups = updatedGroups,
            totalDuplicates = totalDuplicates,
            totalVideos = _uiState.value.totalVideos - deletedIds.size,
            selectedVideoIds = _uiState.value.selectedVideoIds - deletedIds
        )
    }

    data class UiState(
        val isScanning: Boolean = false,
        val totalVideos: Int = 0,
        val scannedCount: Int = 0,
        val totalDuplicates: Int = 0,
        val duplicateGroups: List<DuplicateGroup> = emptyList(),
        val selectedVideoIds: Set<Long> = emptySet()
    )
}

data class VideoFile(
    val mediaId: Long,
    val contentUri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val duration: Long,
    val width: Int = 0,
    val height: Int = 0,
    val dateCreated: Long = 0L,
    val lastModified: Long = 0L
)

data class DuplicateGroup(
    val videos: List<VideoFile>
)
