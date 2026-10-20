package com.saturatah.gallery.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.saturatah.gallery.ui.theme.PureBlack
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun GalleryApp(
    viewModel: GalleryViewModel,
    onRequestPermission: () -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPhotoIndex by viewModel.selectedPhotoIndex.collectAsState()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsState()
    val editingPhoto by viewModel.editingPhoto.collectAsState()

    val gridState = rememberLazyGridState()
    var pendingScrollIndex by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isSelectionMode = selectedPhotoIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearMultiSelection()
    }

    BackHandler(enabled = (!isSelectionMode && editingPhoto == null && selectedPhotoIndex == null)) {
        onBackToCamera()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        val currentEditPhoto = editingPhoto
        val selectedIndex = selectedPhotoIndex

        if (currentEditPhoto != null) {
            EditPhotoScreen(
                photo = currentEditPhoto,
                onCancel = { viewModel.cancelEditing() },
                onSave = { rotationDegrees, cropLeft, cropTop, cropRight, cropBottom, saturationLevel, skinLevel, lightLevel, darkLevel, filter, blurLevel, saveAsCopy ->
                    viewModel.saveEditedPhoto(
                        photo = currentEditPhoto,
                        rotationDegrees = rotationDegrees,
                        cropLeft = cropLeft,
                        cropTop = cropTop,
                        cropRight = cropRight,
                        cropBottom = cropBottom,
                        saturationLevel = saturationLevel,
                        skinLevel = skinLevel,
                        lightLevel = lightLevel,
                        darkLevel = darkLevel,
                        filter = filter,
                        blurLevel = blurLevel,
                        saveAsCopy = saveAsCopy
                    )
                },
                loadBitmap = { photo -> viewModel.loadPreviewBitmap(photo) }
            )
        } else if (selectedIndex != null && uiState is GalleryUiState.Success) {
            val photos = (uiState as GalleryUiState.Success).photos
            PhotoDetailScreen(
                photos = photos,
                initialIndex = selectedIndex,
                onBack = { lastViewedIndex ->
                    if (lastViewedIndex != selectedIndex) {
                        pendingScrollIndex = lastViewedIndex
                    }
                    viewModel.clearSelection()
                },
                onDeletePhoto = { photo -> viewModel.deletePhoto(photo) },
                onEditPhoto = { photo -> viewModel.startEditing(photo) }
            )
        } else {
            LaunchedEffect(pendingScrollIndex) {
                val target = pendingScrollIndex ?: return@LaunchedEffect
                snapshotFlow { gridState.layoutInfo.totalItemsCount }
                    .filter { it > 0 }
                    .first()

                val visibleItem = gridState.layoutInfo.visibleItemsInfo.find { it.index == target }
                val viewportHeight = gridState.layoutInfo.viewportSize.height
                val isFullyVisible = visibleItem != null &&
                    visibleItem.offset.y >= 0 &&
                    (visibleItem.offset.y + visibleItem.size.height) <= viewportHeight

                if (!isFullyVisible) {
                    gridState.scrollToItem(target)
                }
                pendingScrollIndex = null
            }

            GalleryScreen(
                uiState = uiState,
                gridState = gridState,
                onRequestPermission = onRequestPermission,
                onRefresh = { viewModel.loadPhotos() },
                onPhotoClick = { index ->
                    viewModel.selectPhoto(index)
                },
                onBackToCamera = onBackToCamera,
                selectedPhotoIds = selectedPhotoIds,
                onPhotoLongClick = { photo ->
                    viewModel.startSelection(photo.id)
                },
                onTogglePhotoSelection = { photo ->
                    viewModel.togglePhotoSelection(photo.id)
                },
                onClearSelection = {
                    viewModel.clearMultiSelection()
                },
                onDeleteSelected = {
                    coroutineScope.launch {
                        val (deletedCount, failedCount) = viewModel.deleteSelectedPhotos()
                        if (deletedCount > 0) {
                            val msg = if (failedCount == 0) {
                                if (deletedCount == 1) "Item deleted" else "$deletedCount items deleted"
                            } else {
                                "Deleted $deletedCount items, $failedCount failed"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else if (failedCount > 0) {
                            Toast.makeText(context, "Failed to delete selected items", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}
