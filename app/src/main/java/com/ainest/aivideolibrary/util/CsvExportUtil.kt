package com.ainest.aivideolibrary.util

import android.content.Context
import android.net.Uri
import com.ainest.aivideolibrary.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportUtil {

    private val HEADERS = listOf(
        "Title", "Category", "AI Model", "Prompt", "Hashtags", "Keywords", "Notes",
        "Favorite", "Facebook", "TikTok", "YouTube", "Instagram", "Date Added", "Last Edited"
    )

    suspend fun export(context: Context, uri: Uri, videos: List<VideoItem>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val writer = out.bufferedWriter()
                    writer.write(HEADERS.joinToString(",") { csvEscape(it) })
                    writer.newLine()
                    videos.forEach { v ->
                        val row = listOf(
                            v.title,
                            v.category,
                            v.aiModel ?: "",
                            v.prompt,
                            v.hashtags,
                            v.keywords,
                            v.notes,
                            if (v.isFavorite) "Yes" else "No",
                            if (v.uploadFacebook) "Yes" else "No",
                            if (v.uploadTiktok) "Yes" else "No",
                            if (v.uploadYoutube) "Yes" else "No",
                            if (v.uploadInstagram) "Yes" else "No",
                            sdf.format(Date(v.dateAdded)),
                            sdf.format(Date(v.lastEdited))
                        )
                        writer.write(row.joinToString(",") { csvEscape(it) })
                        writer.newLine()
                    }
                    writer.flush()
                }
                true
            } catch (e: Exception) {
                false
            }
        }

    private fun csvEscape(value: String): String {
        val needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
