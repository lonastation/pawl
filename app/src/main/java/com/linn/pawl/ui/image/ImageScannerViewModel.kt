package com.linn.pawl.ui.image

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.data.model.DuplicateGroupKey
import com.linn.pawl.data.repository.IgnoredDuplicateGroupRepository
import com.linn.pawl.data.repository.ImageSignatureRepository
import com.linn.pawl.service.ImageScanner
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

data class ImageFile(
    val mediaId: Long,
    val contentUri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val dateCreated: Long = 0L,
    val lastModified: Long = 0L,
    val mimeType: String = ""
) {
    val isGif: Boolean
        get() = mimeType.equals("image/gif", ignoreCase = true) ||
            name.endsWith(".gif", ignoreCase = true) ||
            path.endsWith(".gif", ignoreCase = true)
}

data class ImageDuplicateGroup(
    val images: List<ImageFile>
)

@HiltViewModel
class ImageScannerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageScanner: ImageScanner,
    private val signatureRepository: ImageSignatureRepository,
    private val ignoredGroupRepository: IgnoredDuplicateGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun startSimilarScan() {
        runScan(clearFingerprints = false)
    }

    fun regenerateFingerprintsAndScan() {
        if (_uiState.value.isScanning) return
        runScan(clearFingerprints = true)
    }

    private fun runScan(clearFingerprints: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                duplicateGroups = emptyList(),
                totalImages = 0,
                scannedCount = 0,
                totalDuplicates = 0,
                selectedImageIds = emptySet()
            )

            try {
                if (clearFingerprints) {
                    signatureRepository.clearAll()
                }

                val allImages = withContext(Dispatchers.IO) { getAllImages() }
                _uiState.value = _uiState.value.copy(totalImages = allImages.size)

                val groups = imageScanner.findSimilarImages(
                    images = allImages,
                    onProgress = { scanned ->
                        _uiState.value = _uiState.value.copy(scannedCount = scanned)
                    }
                )

                ignoredGroupRepository.pruneStale(
                    DuplicateGroupKey.MEDIA_IMAGE,
                    allImages.map { it.path }
                )
                val visibleGroups = filterIgnoredGroups(groups)

                val totalDuplicates = visibleGroups.sumOf { it.images.size } - visibleGroups.size

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
        groups: List<ImageDuplicateGroup>
    ): List<ImageDuplicateGroup> {
        val ignoredKeys = ignoredGroupRepository.getIgnoredKeys(DuplicateGroupKey.MEDIA_IMAGE)
        if (ignoredKeys.isEmpty()) return groups
        return groups.filter { group ->
            DuplicateGroupKey.fromPaths(
                DuplicateGroupKey.MEDIA_IMAGE,
                group.images.map { it.path }
            ) !in ignoredKeys
        }
    }

    private fun getAllImages(): List<ImageFile> {
        val images = mutableListOf<ImageFile>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        )

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (it.moveToNext()) {
                val path = it.getString(dataColumn) ?: continue
                val file = File(path)
                if (file.exists()) {
                    val mediaId = it.getLong(idColumn)
                    val mediaStoreModified = it.getLong(dateModifiedColumn) * 1000L
                    images.add(
                        ImageFile(
                            mediaId = mediaId,
                            contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                mediaId
                            ),
                            path = path,
                            name = it.getString(nameColumn) ?: "unknown",
                            size = it.getLong(sizeColumn),
                            width = it.getInt(widthColumn),
                            height = it.getInt(heightColumn),
                            dateCreated = it.getLong(dateAddedColumn) * 1000L,
                            lastModified = maxOf(file.lastModified(), mediaStoreModified),
                            mimeType = it.getString(mimeTypeColumn).orEmpty()
                        )
                    )
                }
            }
        }

        return images
    }

    fun toggleImageSelection(mediaId: Long) {
        val current = _uiState.value.selectedImageIds
        val updated = if (mediaId in current) current - mediaId else current + mediaId
        _uiState.value = _uiState.value.copy(selectedImageIds = updated)
    }

    fun getSelectedImageUris(): List<Uri> {
        return getSelectedImages().map { it.contentUri }
    }

    fun getSelectedImages(): List<ImageFile> {
        val selected = _uiState.value.selectedImageIds
        return _uiState.value.duplicateGroups
            .flatMap { it.images }
            .filter { it.mediaId in selected }
    }

    fun ignoreGroup(group: ImageDuplicateGroup) {
        viewModelScope.launch {
            ignoredGroupRepository.ignore(
                DuplicateGroupKey.MEDIA_IMAGE,
                group.images.map { it.path }
            )
            val groupKey = DuplicateGroupKey.fromPaths(
                DuplicateGroupKey.MEDIA_IMAGE,
                group.images.map { it.path }
            )
            val removedIds = group.images.map { it.mediaId }.toSet()
            val updatedGroups = _uiState.value.duplicateGroups.filter { existing ->
                DuplicateGroupKey.fromPaths(
                    DuplicateGroupKey.MEDIA_IMAGE,
                    existing.images.map { it.path }
                ) != groupKey
            }
            val totalDuplicates = updatedGroups.sumOf { it.images.size } - updatedGroups.size
            _uiState.value = _uiState.value.copy(
                duplicateGroups = updatedGroups,
                totalDuplicates = totalDuplicates,
                selectedImageIds = _uiState.value.selectedImageIds - removedIds
            )
        }
    }

    fun onImagesDeleted(deletedIds: Set<Long>) {
        val deletedPaths = _uiState.value.duplicateGroups
            .flatMap { it.images }
            .filter { it.mediaId in deletedIds }
            .map { it.path }

        viewModelScope.launch {
            signatureRepository.deleteByPaths(deletedPaths)
            ignoredGroupRepository.pruneContainingPaths(
                DuplicateGroupKey.MEDIA_IMAGE,
                deletedPaths
            )
        }

        val updatedGroups = _uiState.value.duplicateGroups.mapNotNull { group ->
            val remaining = group.images.filter { it.mediaId !in deletedIds }
            if (remaining.size >= 2) ImageDuplicateGroup(remaining) else null
        }
        val totalDuplicates = updatedGroups.sumOf { it.images.size } - updatedGroups.size
        _uiState.value = _uiState.value.copy(
            duplicateGroups = updatedGroups,
            totalDuplicates = totalDuplicates,
            totalImages = _uiState.value.totalImages - deletedIds.size,
            selectedImageIds = _uiState.value.selectedImageIds - deletedIds
        )
    }

    data class UiState(
        val isScanning: Boolean = false,
        val totalImages: Int = 0,
        val scannedCount: Int = 0,
        val totalDuplicates: Int = 0,
        val duplicateGroups: List<ImageDuplicateGroup> = emptyList(),
        val selectedImageIds: Set<Long> = emptySet()
    )
}
