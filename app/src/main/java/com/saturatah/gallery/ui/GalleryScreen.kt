package com.saturatah.gallery.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.saturatah.gallery.model.Photo
import com.saturatah.gallery.ui.theme.DarkSurface
import com.saturatah.gallery.ui.theme.PureBlack
import com.saturatah.gallery.ui.theme.SubduedGray
import java.util.Locale

@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onPhotoClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    onBackToCamera: (() -> Unit)? = null,
    selectedPhotoIds: Set<Long> = emptySet(),
    onPhotoLongClick: (Photo) -> Unit = {},
    onTogglePhotoSelection: (Photo) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val isSelectionMode = selectedPhotoIds.isNotEmpty()

    val selectedPhotos = remember(uiState, selectedPhotoIds) {
        if (uiState is GalleryUiState.Success) {
            uiState.photos.filter { it.id in selectedPhotoIds }
        } else {
            emptyList()
        }
    }

    if (showDeleteConfirmation) {
        val count = selectedPhotoIds.size
        val title = if (count == 1) "Delete item?" else "Delete $count items?"
        val message = if (count == 1) {
            "This item will be permanently deleted from your device."
        } else {
            "These $count items will be permanently deleted from your device."
        }

        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubduedGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteSelected()
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = Color(0xFFEF5350),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White
                    )
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Dedicated black header area with camera return button or multi-select action bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close selection",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "${selectedPhotoIds.size} selected",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                shareMediaList(context, selectedPhotos)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share selected",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                showDeleteConfirmation = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            } else {
                if (onBackToCamera != null) {
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBackToCamera()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Back to camera",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Gallery content area below the header
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState) {
                is GalleryUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
                is GalleryUiState.PermissionRequired -> {
                    PermissionCard(
                        onRequestPermission = onRequestPermission,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is GalleryUiState.Empty -> {
                    EmptyGallery(
                        onRefresh = onRefresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is GalleryUiState.Success -> {
                    PhotoGrid(
                        photos = uiState.photos,
                        selectedPhotoIds = selectedPhotoIds,
                        isSelectionMode = isSelectionMode,
                        onPhotoClick = onPhotoClick,
                        onPhotoLongClick = onPhotoLongClick,
                        onTogglePhotoSelection = onTogglePhotoSelection,
                        gridState = gridState
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoGrid(
    photos: List<Photo>,
    selectedPhotoIds: Set<Long>,
    isSelectionMode: Boolean,
    onPhotoClick: (Int) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onTogglePhotoSelection: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val hapticFeedback = LocalHapticFeedback.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        itemsIndexed(
            items = photos,
            key = { _, photo -> photo.id }
        ) { index, photo ->
            val isSelected = selectedPhotoIds.contains(photo.id)
            PhotoThumbnailItem(
                photo = photo,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onClick = {
                    if (isSelectionMode) {
                        onTogglePhotoSelection(photo)
                    } else {
                        onPhotoClick(index)
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPhotoLongClick(photo)
                    } else {
                        onTogglePhotoSelection(photo)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoThumbnailItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(photo.uri, photo.dateModified) {
        ImageRequest.Builder(context)
            .data(photo.uri)
            .memoryCacheKey("${photo.uri}_${photo.dateModified}_thumb")
            .diskCacheKey("${photo.uri}_${photo.dateModified}_thumb")
            .setParameter("is_thumbnail", true)
            .size(360, 360)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(DarkSurface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val contentModifier = if (isSelectionMode && isSelected) {
            Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(8.dp))
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = contentModifier) {
            AsyncImage(
                model = imageRequest,
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A73E8).copy(alpha = 0.25f))
                )
            }
        }

        if (photo.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(if (isSelectionMode && isSelected) 10.dp else 6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                if (photo.duration > 0) {
                    val totalSec = photo.duration / 1000
                    val min = totalSec / 60
                    val sec = totalSec % 60
                    Text(
                        text = String.format(Locale.US, "%d:%02d", min, sec),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(if (isSelected) 10.dp else 8.dp)
                    .size(24.dp)
                    .then(
                        if (isSelected) {
                            Modifier.background(Color(0xFF1A73E8), CircleShape)
                        } else {
                            Modifier
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun shareMediaList(context: Context, photos: List<Photo>) {
    if (photos.isEmpty()) return
    val uris = ArrayList(photos.map { it.uri })
    val hasVideo = photos.any { it.isVideo }
    val hasImage = photos.any { !it.isVideo }
    val mimeType = when {
        hasImage && hasVideo -> "*/*"
        hasVideo -> "video/*"
        else -> "image/*"
    }

    val shareIntent = if (photos.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, photos[0].uri)
            clipData = ClipData.newRawUri("", photos[0].uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            val firstUri = uris.firstOrNull()
            if (firstUri != null) {
                val clipData = ClipData.newRawUri("", firstUri)
                for (i in 1 until uris.size) {
                    clipData.addItem(ClipData.Item(uris[i]))
                }
                this.clipData = clipData
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    try {
        val chooser = Intent.createChooser(
            shareIntent,
            if (photos.size == 1) "Share item" else "Share ${photos.size} items"
        )
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.util.Log.e("GalleryScreen", "Failed launching share intent", e)
        Toast.makeText(context, "No app available to share", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = SubduedGray,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Access Photos & Videos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Saturatah requires storage access to display the photos and videos stored on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = SubduedGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = PureBlack
            )
        ) {
            Text(
                text = "Grant Permission",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun EmptyGallery(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = SubduedGray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Photos Found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Take a picture or add photos to your device gallery to view them here.",
            style = MaterialTheme.typography.bodyMedium,
            color = SubduedGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkSurface,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Text("Check Again")
        }
    }
}
