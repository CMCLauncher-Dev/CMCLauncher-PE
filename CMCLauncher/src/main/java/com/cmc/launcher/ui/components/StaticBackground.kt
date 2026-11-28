package com.cmc.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StaticBackground(file: File, video: Boolean, refresh: Any, modifier: Modifier = Modifier) {
    val windowSize = LocalWindowInfo.current.containerSize
    val width = windowSize.width.coerceAtLeast(1)
    val height = windowSize.height.coerceAtLeast(1)
    val bitmap by produceState<Bitmap?>(null, file, video, refresh, width, height) {
        value = withContext(Dispatchers.IO) {
            try {
                if (video) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        if (Build.VERSION.SDK_INT >= 27) {
                            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, width, height)
                        } else {
                            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }
                    } finally { retriever.release() }
                } else {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    var sample = 1
                    while (options.outWidth / (sample * 2) >= width && options.outHeight / (sample * 2) >= height) sample *= 2
                    options.inJustDecodeBounds = false
                    options.inSampleSize = sample
                    BitmapFactory.decodeFile(file.absolutePath, options)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }
    bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop) }
}
