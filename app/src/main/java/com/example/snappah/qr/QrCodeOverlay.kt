package com.example.snappah.qr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snappah.ui.theme.SnappahBlue
import com.example.snappah.ui.theme.White
import kotlin.math.roundToInt

private val BracketYellow = Color(0xFFFFD54F)
private val ChipBackground = Color(0xF21C1C1E)

@Composable
fun QrCodeOverlay(
    qrResult: QrCodeResult?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = qrResult != null,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        if (qrResult == null) return@AnimatedVisibility

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            // Smooth animated target coordinates for the bounding box
            val rawBox = qrResult.normalizedBoundingBox

            val animLeft by animateFloatAsState(
                targetValue = (rawBox?.left ?: 0.25f) * widthPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "qrLeft"
            )
            val animTop by animateFloatAsState(
                targetValue = (rawBox?.top ?: 0.35f) * heightPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "qrTop"
            )
            val animRight by animateFloatAsState(
                targetValue = (rawBox?.right ?: 0.75f) * widthPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "qrRight"
            )
            val animBottom by animateFloatAsState(
                targetValue = (rawBox?.bottom ?: 0.65f) * heightPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "qrBottom"
            )

            // 1. Draw viewfinder corner brackets around QR code if bounding box is valid
            if (rawBox != null && (animRight - animLeft) > 10f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.5.dp.toPx()
                    val cornerLen = ((animRight - animLeft).coerceAtMost(animBottom - animTop) * 0.22f)
                        .coerceIn(16.dp.toPx(), 36.dp.toPx())

                    val pad = 4.dp.toPx()
                    val l = (animLeft - pad).coerceAtLeast(4f)
                    val t = (animTop - pad).coerceAtLeast(4f)
                    val r = (animRight + pad).coerceAtMost(size.width - 4f)
                    val b = (animBottom + pad).coerceAtMost(size.height - 4f)

                    val strokeStyle = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )

                    // Top-Left corner
                    val pathTL = Path().apply {
                        moveTo(l, t + cornerLen)
                        lineTo(l, t)
                        lineTo(l + cornerLen, t)
                    }
                    drawPath(pathTL, BracketYellow, style = strokeStyle)

                    // Top-Right corner
                    val pathTR = Path().apply {
                        moveTo(r - cornerLen, t)
                        lineTo(r, t)
                        lineTo(r, t + cornerLen)
                    }
                    drawPath(pathTR, BracketYellow, style = strokeStyle)

                    // Bottom-Right corner
                    val pathBR = Path().apply {
                        moveTo(r, b - cornerLen)
                        lineTo(r, b)
                        lineTo(r - cornerLen, b)
                    }
                    drawPath(pathBR, BracketYellow, style = strokeStyle)

                    // Bottom-Left corner
                    val pathBL = Path().apply {
                        moveTo(l + cornerLen, b)
                        lineTo(l, b)
                        lineTo(l, b - cornerLen)
                    }
                    drawPath(pathBL, BracketYellow, style = strokeStyle)
                }
            }

            // 2. Floating interactive pill chip
            // Place directly beneath the QR code bounding box, or above if close to bottom
            val boxCenterX = ((animLeft + animRight) / 2f)
            val boxBottomY = animBottom
            val spaceBelow = heightPx - boxBottomY

            val targetYPx = if (spaceBelow >= with(density) { 88.dp.toPx() }) {
                boxBottomY + with(density) { 14.dp.toPx() }
            } else {
                (animTop - with(density) { 72.dp.toPx() }).coerceAtLeast(with(density) { 16.dp.toPx() })
            }

            val animPillY by animateFloatAsState(
                targetValue = targetYPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "qrPillY"
            )

            val animPillX by animateFloatAsState(
                targetValue = boxCenterX,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "qrPillX"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset {
                            // Horizontally center or track with offset, clamping within view
                            val xOffset = ((animPillX - (widthPx / 2f)) * 0.45f).roundToInt()
                            IntOffset(xOffset, animPillY.roundToInt())
                        }
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(ChipBackground)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = SnappahBlue)
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            qrResult.executePrimaryAction(context)
                        }
                        .padding(start = 12.dp, top = 8.dp, end = 6.dp, bottom = 8.dp)
                        .widthIn(min = 180.dp, max = 340.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type-specific Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SnappahBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getQrTypeIcon(qrResult.type),
                            contentDescription = qrResult.type.name,
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title & Action Subtitle
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = qrResult.title,
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = qrResult.actionLabel,
                                color = BracketYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (qrResult.type == QrType.URL) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = BracketYellow,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Copy action button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            qrResult.copyToClipboard(context)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy QR content",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Dismiss button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0x88FFFFFF),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getQrTypeIcon(type: QrType): ImageVector {
    return when (type) {
        QrType.URL -> Icons.Default.Language
        QrType.WIFI -> Icons.Default.Wifi
        QrType.PHONE -> Icons.Default.Phone
        QrType.EMAIL -> Icons.Default.Email
        QrType.SMS -> Icons.Default.Sms
        QrType.GEO -> Icons.Default.Navigation
        QrType.TEXT -> Icons.Default.QrCode
    }
}
