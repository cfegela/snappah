package com.saturatah.gallery.util

import android.content.ContentResolver
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreThumbnailFetcher(
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val width = (options.size.width as? Dimension.Pixels)?.px?.coerceAtLeast(128) ?: 360
            val height = (options.size.height as? Dimension.Pixels)?.px?.coerceAtLeast(128) ?: 360
            return withContext(Dispatchers.IO) {
                try {
                    val bitmap = options.context.contentResolver.loadThumbnail(
                        uri,
                        Size(width, height),
                        null
                    )
                    DrawableResult(
                        drawable = BitmapDrawable(options.context.resources, bitmap),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isThumbnail = options.parameters.value("is_thumbnail") as? Boolean ?: false
            val widthPx = (options.size.width as? Dimension.Pixels)?.px
            val heightPx = (options.size.height as? Dimension.Pixels)?.px
            val isSmall = widthPx != null && heightPx != null && widthPx <= 720 && heightPx <= 720

            if ((isThumbnail || isSmall) && isMediaStoreUri(data)) {
                return MediaStoreThumbnailFetcher(data, options)
            }
            return null
        }

        private fun isMediaStoreUri(uri: Uri): Boolean {
            return uri.scheme == ContentResolver.SCHEME_CONTENT &&
                (uri.authority == "media" || uri.authority?.contains("media") == true)
        }
    }
}
