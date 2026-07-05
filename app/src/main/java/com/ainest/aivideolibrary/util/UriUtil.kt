package com.ainest.aivideolibrary.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object UriUtil {

    /**
     * Persist read access to a picked video so we can keep opening it later,
     * across app restarts and device reboots, without ever copying the file.
     */
    fun takePersistablePermission(context: Context, uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Some providers don't support persistable permissions; playback will
            // still work for the current session via the granted read permission.
        }
    }

    /** Opens the given video URI in the user's default video/gallery app. */
    fun openInDefaultPlayer(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // No app available to handle this video type / the file may have moved.
        }
    }

    /** Best-effort display name for a picked video, used later to auto re-link if the URI breaks. */
    fun getDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
