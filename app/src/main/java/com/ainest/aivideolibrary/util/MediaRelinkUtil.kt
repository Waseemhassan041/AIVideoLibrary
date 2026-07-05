package com.ainest.aivideolibrary.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RelinkCandidate(val uri: Uri, val displayName: String)

object MediaRelinkUtil {

    /** True if the app can currently open this URI (e.g. after restoring on a new phone). */
    suspend fun isReachable(context: Context, uriString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Searches the device's video library for a file whose display name matches
     * (case-insensitive) the given original file name. Used to offer an automatic
     * re-link suggestion after a restore where the original content:// URI no
     * longer resolves on this device.
     */
    suspend fun findMatchByName(context: Context, originalFileName: String): RelinkCandidate? =
        withContext(Dispatchers.IO) {
            if (originalFileName.isBlank()) return@withContext null
            val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME)
            val selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(originalFileName)
            try {
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol)
                        val contentUri = android.content.ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )
                        return@withContext RelinkCandidate(contentUri, name)
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }

    /** Best-effort guess at the original file's display name from a stored URI/title. */
    fun guessFileName(uriString: String, fallbackTitle: String): String {
        val uri = Uri.parse(uriString)
        return uri.lastPathSegment?.substringAfterLast('/') ?: fallbackTitle
    }
}
