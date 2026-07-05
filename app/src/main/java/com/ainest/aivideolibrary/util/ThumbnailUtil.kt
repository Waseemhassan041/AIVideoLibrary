package com.ainest.aivideolibrary.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Generates a small preview thumbnail for a picked video and stores ONLY that
 * preview image inside the app's private storage. The original video file is
 * never touched, copied, or moved.
 */
object ThumbnailUtil {

    suspend fun generateThumbnail(context: Context, videoUri: Uri): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val frame: Bitmap? = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame == null) return@withContext null

            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val outFile = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(outFile).use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            outFile.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun deleteThumbnail(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).delete()
        } catch (_: Exception) {
        }
    }
}
