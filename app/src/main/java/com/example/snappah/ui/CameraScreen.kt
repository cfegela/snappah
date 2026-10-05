package com.example.snappah.ui

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.snappah.R
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.snappah.ui.theme.Black
import com.example.snappah.ui.theme.SnappahBlue
import com.example.snappah.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SnappahCamera"
private const val SATURATION_BOOST = 1.15f // 15% saturation boost for WYSIWYG vibrant look

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isShutterFlashing by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isStreamStreaming by remember { mutableStateOf(false) }

    val cameraSelector = if (isFrontCamera) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }

    LaunchedEffect(cameraProvider, previewView, cameraSelector) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val pView = previewView ?: return@LaunchedEffect

        isStreamStreaming = false

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(pView.surfaceProvider)
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = capture

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Camera binding failed", exc)
        }
    }

    val soundPool = remember {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
    }
    val soundId = remember {
        soundPool.load(context, R.raw.shutter_sound, 1)
    }

    DisposableEffect(soundPool) {
        onDispose {
            soundPool.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top letterbox spacing to keep viewfinder vertically centered
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // Full uncropped native sensor viewfinder with double-tap camera toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                isStreamStreaming = false
                                isFrontCamera = !isFrontCamera
                            }
                        )
                    }
            ) {
                val saturationPaint = remember {
                    android.graphics.Paint().apply {
                        colorFilter = android.graphics.ColorMatrixColorFilter(
                            android.graphics.ColorMatrix().apply {
                                setSaturation(SATURATION_BOOST)
                            }
                        )
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, saturationPaint)
                            setOnHierarchyChangeListener(object : android.view.ViewGroup.OnHierarchyChangeListener {
                                override fun onChildViewAdded(parent: android.view.View?, child: android.view.View?) {
                                    child?.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, saturationPaint)
                                }
                                override fun onChildViewRemoved(parent: android.view.View?, child: android.view.View?) {}
                            })
                            setBackgroundColor(android.graphics.Color.BLACK)
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                            previewStreamState.observe(lifecycleOwner) { state ->
                                isStreamStreaming = (state == PreviewView.StreamState.STREAMING)
                            }
                            previewView = this
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                cameraProvider = cameraProviderFuture.get()
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    }
                )

                // Seamless transition overlay: masks stale camera buffer until new camera stream is active
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isStreamStreaming,
                    enter = fadeIn(animationSpec = tween(30)),
                    exit = fadeOut(animationSpec = tween(120))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Black)
                    )
                }
            }

            // Bottom letterbox area: perfectly centers the shutter button between viewfinder and navigation bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                ShutterButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Play vintage mechanical shutter sound at 40% volume
                        soundPool.play(soundId, 0.40f, 0.40f, 1, 0, 1.0f)
                        // Trigger visual shutter flash
                        coroutineScope.launch {
                            isShutterFlashing = true
                            delay(60)
                            isShutterFlashing = false
                        }

                        takePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onPhotoSaved = { uriString ->
                                Log.d(TAG, "Photo saved: $uriString")
                            },
                            onError = { exc ->
                                Toast.makeText(context, "Capture error: ${exc.message}", Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "Capture failed", exc)
                            }
                        )
                    }
                )
            }
        }

        // Subtle Shutter Flash Feedback
        AnimatedVisibility(
            visible = isShutterFlashing,
            enter = fadeIn(animationSpec = tween(30)),
            exit = fadeOut(animationSpec = tween(80))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 32.dp, color = Color.Gray),
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2f

            // White base circle (matching app launcher icon background)
            drawCircle(
                color = White,
                radius = radius,
                center = center
            )

            // Outer blue ring (matching app icon outer band)
            val outerRingRadius = radius * (52f / 72f)
            val strokeWidth = radius * (3f / 36f)
            drawCircle(
                color = SnappahBlue,
                radius = outerRingRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Inner blue filled circle (matching app icon inner shutter core)
            val innerCircleRadius = radius * (36f / 72f)
            drawCircle(
                color = SnappahBlue,
                radius = innerCircleRadius,
                center = center
            )
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onPhotoSaved: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val capture = imageCapture ?: return

    val tempFile = try {
        File.createTempFile("snap_raw_", ".jpg", context.cacheDir)
    } catch (e: Exception) {
        onError(e)
        return
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    try {
                        saveSaturatedPhoto(context, tempFile, onPhotoSaved)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing saturated photo", e)
                        withContext(Dispatchers.Main) {
                            onError(e)
                        }
                    } finally {
                        tempFile.delete()
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                tempFile.delete()
                onError(exception)
            }
        }
    )
}

private suspend fun saveSaturatedPhoto(
    context: Context,
    tempFile: File,
    onPhotoSaved: (String) -> Unit
) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "SNAP_$timeStamp.jpg"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Snappah")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: throw IllegalStateException("Failed to create MediaStore entry")

    try {
        val srcExif = try {
            android.media.ExifInterface(tempFile.absolutePath)
        } catch (e: Exception) {
            null
        }

        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
        if (bitmap != null) {
            val resultBitmap = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                bitmap.config ?: Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(resultBitmap)
            val paint = android.graphics.Paint().apply {
                colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix().apply {
                        setSaturation(SATURATION_BOOST)
                    }
                )
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            bitmap.recycle()

            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 98, outStream)
            }
            resultBitmap.recycle()
        } else {
            FileInputStream(tempFile).use { input ->
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    input.copyTo(output)
                }
            }
        }

        if (srcExif != null) {
            try {
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val destExif = android.media.ExifInterface(pfd.fileDescriptor)
                    copyExifAttributes(srcExif, destExif)
                    destExif.saveAttributes()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy EXIF attributes", e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
        }

        withContext(Dispatchers.Main) {
            onPhotoSaved(uri.toString())
        }
    } catch (e: Exception) {
        context.contentResolver.delete(uri, null, null)
        throw e
    }
}

private fun copyExifAttributes(
    src: android.media.ExifInterface,
    dest: android.media.ExifInterface
) {
    val attributes = arrayOf(
        android.media.ExifInterface.TAG_ORIENTATION,
        android.media.ExifInterface.TAG_DATETIME,
        android.media.ExifInterface.TAG_DATETIME_DIGITIZED,
        android.media.ExifInterface.TAG_DATETIME_ORIGINAL,
        android.media.ExifInterface.TAG_EXPOSURE_TIME,
        android.media.ExifInterface.TAG_F_NUMBER,
        android.media.ExifInterface.TAG_FLASH,
        android.media.ExifInterface.TAG_FOCAL_LENGTH,
        android.media.ExifInterface.TAG_GPS_ALTITUDE,
        android.media.ExifInterface.TAG_GPS_ALTITUDE_REF,
        android.media.ExifInterface.TAG_GPS_DATESTAMP,
        android.media.ExifInterface.TAG_GPS_LATITUDE,
        android.media.ExifInterface.TAG_GPS_LATITUDE_REF,
        android.media.ExifInterface.TAG_GPS_LONGITUDE,
        android.media.ExifInterface.TAG_GPS_LONGITUDE_REF,
        android.media.ExifInterface.TAG_GPS_PROCESSING_METHOD,
        android.media.ExifInterface.TAG_GPS_TIMESTAMP,
        android.media.ExifInterface.TAG_IMAGE_LENGTH,
        android.media.ExifInterface.TAG_IMAGE_WIDTH,
        android.media.ExifInterface.TAG_ISO_SPEED_RATINGS,
        android.media.ExifInterface.TAG_MAKE,
        android.media.ExifInterface.TAG_MODEL,
        android.media.ExifInterface.TAG_WHITE_BALANCE
    )
    for (attr in attributes) {
        val value = src.getAttribute(attr)
        if (value != null) {
            dest.setAttribute(attr, value)
        }
    }
}
