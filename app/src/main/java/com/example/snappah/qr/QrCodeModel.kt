package com.example.snappah.qr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.barcode.common.Barcode
import java.net.URI

enum class QrType {
    URL,
    WIFI,
    PHONE,
    EMAIL,
    SMS,
    GEO,
    TEXT
}

data class QrWifiData(
    val ssid: String,
    val password: String?,
    val encryptionType: Int
) {
    val encryptionSummary: String
        get() = when (encryptionType) {
            Barcode.WiFi.TYPE_WPA -> "WPA/WPA2"
            Barcode.WiFi.TYPE_WEP -> "WEP"
            Barcode.WiFi.TYPE_OPEN -> "Open"
            else -> "Secured"
        }
}

data class QrCodeResult(
    val rawValue: String,
    val type: QrType,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val normalizedBoundingBox: Rect?, // Normalized to [0f..1f] coordinate space
    val wifiData: QrWifiData? = null,
    val targetUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun executePrimaryAction(context: Context) {
        try {
            when (type) {
                QrType.URL -> {
                    val urlToOpen = if (!rawValue.startsWith("http://", ignoreCase = true) &&
                        !rawValue.startsWith("https://", ignoreCase = true)
                    ) {
                        "https://$rawValue"
                    } else {
                        rawValue
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                QrType.PHONE -> {
                    val number = targetUri ?: rawValue.removePrefix("tel:")
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                QrType.EMAIL -> {
                    val emailUri = if (rawValue.startsWith("mailto:", ignoreCase = true)) {
                        rawValue
                    } else {
                        "mailto:${targetUri ?: rawValue}"
                    }
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(emailUri)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                QrType.SMS -> {
                    val smsUri = if (rawValue.startsWith("smsto:", ignoreCase = true) ||
                        rawValue.startsWith("sms:", ignoreCase = true)
                    ) {
                        rawValue
                    } else {
                        "smsto:${targetUri ?: rawValue}"
                    }
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(smsUri)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                QrType.GEO -> {
                    val geoUri = if (rawValue.startsWith("geo:", ignoreCase = true)) {
                        rawValue
                    } else {
                        "geo:${targetUri ?: rawValue}"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                QrType.WIFI -> {
                    // Copy password to clipboard if present and open Wi-Fi settings
                    wifiData?.password?.let { pwd ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Wi-Fi Password", pwd))
                        Toast.makeText(context, "Wi-Fi password copied: $pwd", Toast.LENGTH_SHORT).show()
                    }
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                QrType.TEXT -> {
                    copyToClipboard(context)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open action: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR Code Content", rawValue)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}

object QrCodeParser {
    fun parse(
        barcode: Barcode,
        normalizedBox: Rect? = null
    ): QrCodeResult {
        val raw = barcode.rawValue ?: barcode.displayValue.orEmpty()

        when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                val url = barcode.url?.url ?: raw
                val domain = extractDomain(url)
                return QrCodeResult(
                    rawValue = url,
                    type = QrType.URL,
                    title = domain,
                    subtitle = url,
                    actionLabel = "Open Link",
                    normalizedBoundingBox = normalizedBox,
                    targetUri = url
                )
            }

            Barcode.TYPE_WIFI -> {
                val wifi = barcode.wifi
                val ssid = wifi?.ssid ?: "Wi-Fi Network"
                val wifiData = wifi?.let {
                    QrWifiData(
                        ssid = it.ssid.orEmpty(),
                        password = it.password,
                        encryptionType = it.encryptionType
                    )
                }
                val subtitle = buildString {
                    append(wifiData?.encryptionSummary ?: "Network")
                    if (!wifiData?.password.isNullOrBlank()) {
                        append(" • Password copied on tap")
                    }
                }
                return QrCodeResult(
                    rawValue = raw,
                    type = QrType.WIFI,
                    title = ssid,
                    subtitle = subtitle,
                    actionLabel = "Connect",
                    normalizedBoundingBox = normalizedBox,
                    wifiData = wifiData
                )
            }

            Barcode.TYPE_PHONE -> {
                val phone = barcode.phone?.number ?: raw.removePrefix("tel:")
                return QrCodeResult(
                    rawValue = raw,
                    type = QrType.PHONE,
                    title = phone,
                    subtitle = "Phone number",
                    actionLabel = "Call",
                    normalizedBoundingBox = normalizedBox,
                    targetUri = phone
                )
            }

            Barcode.TYPE_EMAIL -> {
                val email = barcode.email?.address ?: raw.removePrefix("mailto:")
                val subject = barcode.email?.subject
                val subtitle = if (!subject.isNullOrBlank()) "Email: $subject" else "Email address"
                return QrCodeResult(
                    rawValue = raw,
                    type = QrType.EMAIL,
                    title = email,
                    subtitle = subtitle,
                    actionLabel = "Send Email",
                    normalizedBoundingBox = normalizedBox,
                    targetUri = email
                )
            }

            Barcode.TYPE_SMS -> {
                val phone = barcode.sms?.phoneNumber ?: raw.removePrefix("smsto:").removePrefix("sms:")
                val msg = barcode.sms?.message
                val subtitle = if (!msg.isNullOrBlank()) "SMS: $msg" else "Send SMS"
                return QrCodeResult(
                    rawValue = raw,
                    type = QrType.SMS,
                    title = phone,
                    subtitle = subtitle,
                    actionLabel = "Message",
                    normalizedBoundingBox = normalizedBox,
                    targetUri = phone
                )
            }

            Barcode.TYPE_GEO -> {
                val geo = barcode.geoPoint
                val latLng = if (geo != null) "${geo.lat}, ${geo.lng}" else raw.removePrefix("geo:")
                return QrCodeResult(
                    rawValue = raw,
                    type = QrType.GEO,
                    title = "Location",
                    subtitle = latLng,
                    actionLabel = "View Map",
                    normalizedBoundingBox = normalizedBox,
                    targetUri = latLng
                )
            }

            else -> {
                // If it looks like a URL despite being classified as text, handle as URL
                if (isProbableUrl(raw)) {
                    val domain = extractDomain(raw)
                    return QrCodeResult(
                        rawValue = raw,
                        type = QrType.URL,
                        title = domain,
                        subtitle = raw,
                        actionLabel = "Open Link",
                        normalizedBoundingBox = normalizedBox,
                        targetUri = raw
                    )
                }

                val preview = raw.trim().replace("\n", " ")
                val title = if (preview.length > 36) preview.take(33) + "..." else preview
                return QrCodeResult(
                    rawValue = raw,
                    type = QrType.TEXT,
                    title = title,
                    subtitle = "Tap to copy",
                    actionLabel = "Copy",
                    normalizedBoundingBox = normalizedBox
                )
            }
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val formatted = if (!url.startsWith("http://", ignoreCase = true) &&
                !url.startsWith("https://", ignoreCase = true)
            ) {
                "https://$url"
            } else {
                url
            }
            val uri = URI(formatted)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                host.removePrefix("www.")
            } else {
                formatted.take(28)
            }
        } catch (_: Exception) {
            url.take(28)
        }
    }

    private fun isProbableUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return true
        }
        val domainRegex = Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$")
        return domainRegex.matches(trimmed) && !trimmed.contains(" ")
    }
}
