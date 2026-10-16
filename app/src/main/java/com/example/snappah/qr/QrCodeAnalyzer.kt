package com.example.snappah.qr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val isPaused: () -> Boolean,
    private val isFrontCamera: () -> Boolean,
    private val onQrCodeDetected: (QrCodeResult?) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_AZTEC
            )
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // ML Kit coordinates correspond to rotated dimensions
        val (rotatedWidth, rotatedHeight) = if (rotationDegrees == 90 || rotationDegrees == 270) {
            Pair(mediaImage.height.toFloat(), mediaImage.width.toFloat())
        } else {
            Pair(mediaImage.width.toFloat(), mediaImage.height.toFloat())
        }

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (isPaused()) return@addOnSuccessListener

                val qrBarcode = barcodes.firstOrNull {
                    !it.rawValue.isNullOrBlank() || !it.displayValue.isNullOrBlank()
                }

                if (qrBarcode != null) {
                    val box = qrBarcode.boundingBox
                    val normalizedBox = if (box != null && rotatedWidth > 0 && rotatedHeight > 0) {
                        val l = box.left / rotatedWidth
                        val t = box.top / rotatedHeight
                        val r = box.right / rotatedWidth
                        val b = box.bottom / rotatedHeight

                        if (isFrontCamera()) {
                            Rect(
                                left = (1f - r).coerceIn(0f, 1f),
                                top = t.coerceIn(0f, 1f),
                                right = (1f - l).coerceIn(0f, 1f),
                                bottom = b.coerceIn(0f, 1f)
                            )
                        } else {
                            Rect(
                                left = l.coerceIn(0f, 1f),
                                top = t.coerceIn(0f, 1f),
                                right = r.coerceIn(0f, 1f),
                                bottom = b.coerceIn(0f, 1f)
                            )
                        }
                    } else null

                    val result = QrCodeParser.parse(qrBarcode, normalizedBox)
                    onQrCodeDetected(result)
                } else {
                    onQrCodeDetected(null)
                }
            }
            .addOnFailureListener {
                onQrCodeDetected(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
