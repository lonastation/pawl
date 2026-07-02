package com.linn.pawl.ui.viewmodels

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.service.VideoScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoScannerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val videoScanner: VideoScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                duplicateGroups = emptyList(),
                totalVideos = 0,
                scannedCount = 0,
                totalDuplicates = 0,
                selectedVideoIds = emptySet()
            )

            // 获取所有视频
            val allVideos = getAllVideos()
            _uiState.value = _uiState.value.copy(
                totalVideos = allVideos.size
            )

            // 分析并分组相似视频
            val groups = videoScanner.findSimilarVideos(
                videos = allVideos,
                onProgress = { scanned ->
                    _uiState.value = _uiState.value.copy(
                        scannedCount = scanned
                    )
                }
            )

            // 计算重复总数
            val totalDuplicates = groups.sumOf { it.videos.size } - groups.size

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                duplicateGroups = groups,
                totalDuplicates = totalDuplicates
            )
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
            MediaStore.Video.Media.HEIGHT
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

            while (it.moveToNext()) {
                val path = it.getString(dataColumn) ?: continue
                if (File(path).exists()) {
                    val mediaId = it.getLong(idColumn)
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
                            height = it.getInt(heightColumn)
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

    fun onVideosDeleted(deletedIds: Set<Long>) {
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
    val height: Int = 0
)

data class DuplicateGroup(
    val videos: List<VideoFile>
)