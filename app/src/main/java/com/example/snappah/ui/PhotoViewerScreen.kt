package com.example.snappah.ui

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snappah.ui.theme.Black
import com.example.snappah.ui.theme.SnappahBlue
import com.example.snappah.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PhotoViewerScreen"

@Composable
fun PhotoViewerScreen(
    photoUri: Uri?,
    onBackToCamera: () -> Unit,
    onPhotoDeleted: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var bitmap by remember(photoUri) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(photoUri) { mutableStateOf(photoUri != null) }
    var isDeleting by remember { mutableStateOf(false) }

    val deleteIntentSenderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                val nextUri = withContext(Dispatchers.IO) { queryLatestPhotoUri(context) }
                onPhotoDeleted(nextUri)
                Toast.makeText(context, "Photo deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(photoUri) {
        if (photoUri == null) {
            bitmap = null
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            loadDownsampledBitmap(context, photoUri)
        }
        isLoading = false
    }

    fun handleDelete() {
        val uri = photoUri ?: return
        if (isDeleting) return
        isDeleting = true
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val count = context.contentResolver.delete(uri, null, null)
                    count > 0
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                        deleteIntentSenderLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                        null
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is android.app.RecoverableSecurityException) {
                        deleteIntentSenderLauncher.launch(
                            IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                        )
                        null
                    } else {
                        Log.e(TAG, "SecurityException deleting photo", e)
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting photo", e)
                    false
                }
            }

            if (success == true) {
                Toast.makeText(context, "Photo deleted", Toast.LENGTH_SHORT).show()
                val nextUri = withContext(Dispatchers.IO) { queryLatestPhotoUri(context) }
                onPhotoDeleted(nextUri)
            } else if (success == false) {
                Toast.makeText(context, "Failed to delete photo", Toast.LENGTH_SHORT).show()
            }
            isDeleting = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Last captured photo",
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentScale = ContentScale.Fit
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = SnappahBlue
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No photos yet",
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Swipe right to return to camera",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // Top navigation bar overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToCamera,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to camera",
                    tint = White
                )
            }

            if (photoUri != null && bitmap != null) {
                IconButton(
                    onClick = { handleDelete() },
                    enabled = !isDeleting,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete photo",
                        tint = White
                    )
                }
            }
        }
    }
}

private fun loadDownsampledBitmap(context: Context, uri: Uri): ImageBitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val displayMetrics = context.resources.displayMetrics
                val targetW = displayMetrics.widthPixels
                val targetH = displayMetrics.heightPixels
                val size = info.size
                val sample = maxOf(1, minOf(size.width / targetW, size.height / targetH))
                if (sample > 1) {
                    decoder.setTargetSampleSize(sample)
                }
            }
            bmp.asImageBitmap()
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode image from uri: $uri", e)
        null
    }
}

fun queryLatestPhotoUri(context: Context): Uri? {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    // 1. Try querying specifically in Pictures/simplah
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%simplah%")
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(idColumn)
                    return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Query for simplah failed", e)
        }
    }

    // 2. Fallback: try photos with filename prefix SNAP_
    try {
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("SNAP_%")
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Query for SNAP_ photos failed", e)
    }

    return null
}
