package com.ainest.aivideolibrary.util

import android.content.Context
import android.net.Uri
import com.ainest.aivideolibrary.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backs up / restores the video library (metadata only) as a JSON file the user
 * picks a location for via the system file picker (SAF). Optionally password
 * encrypted (AES-GCM) via CryptoUtil. Never touches the actual video files.
 */
object BackupRestoreUtil {

    suspend fun export(context: Context, uri: Uri, videos: List<VideoItem>, password: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                videos.forEach { v -> array.put(toJson(v)) }
                val root = JSONObject().apply {
                    put("version", 2)
                    put("exportedAt", System.currentTimeMillis())
                    put("encrypted", password != null)
                    put("videos", array)
                }
                val plain = root.toString(2)
                val output = if (password != null) {
                    JSONObject().apply {
                        put("encrypted", true)
                        put("payload", CryptoUtil.encrypt(plain, password))
                    }.toString(2)
                } else plain

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(output.toByteArray())
                }
                true
            } catch (e: Exception) {
                false
            }
        }

    /** Returns null if decryption fails (wrong password) or the file is invalid. */
    suspend fun import(context: Context, uri: Uri, password: String? = null): List<VideoItem>? =
        withContext(Dispatchers.IO) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@withContext null

                val outer = JSONObject(text)
                val root = if (outer.optBoolean("encrypted", false)) {
                    if (password == null) return@withContext null
                    val decrypted = CryptoUtil.decrypt(outer.getString("payload"), password) ?: return@withContext null
                    JSONObject(decrypted)
                } else outer

                val array = root.getJSONArray("videos")
                val result = mutableListOf<VideoItem>()
                for (i in 0 until array.length()) {
                    result.add(fromJson(array.getJSONObject(i)))
                }
                result
            } catch (e: Exception) {
                null
            }
        }

    /** Peek at a backup file to see if it's password protected, without fully importing it. */
    suspend fun isEncrypted(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext false
            JSONObject(text).optBoolean("encrypted", false)
        } catch (e: Exception) {
            false
        }
    }

    private fun toJson(v: VideoItem): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("videoUri", v.videoUri)
        put("syncId", v.syncId)
        put("originalFileName", v.originalFileName ?: JSONObject.NULL)
        put("thumbnailPath", v.thumbnailPath ?: JSONObject.NULL)
        put("title", v.title)
        put("prompt", v.prompt)
        put("hashtags", v.hashtags)
        put("keywords", v.keywords)
        put("notes", v.notes)
        put("category", v.category)
        put("aiModel", v.aiModel ?: JSONObject.NULL)
        put("isFavorite", v.isFavorite)
        put("isPinned", v.isPinned)
        put("dateAdded", v.dateAdded)
        put("lastEdited", v.lastEdited)
        put("uploadFacebook", v.uploadFacebook)
        put("uploadTiktok", v.uploadTiktok)
        put("uploadYoutube", v.uploadYoutube)
        put("uploadInstagram", v.uploadInstagram)
    }

    private fun fromJson(o: JSONObject): VideoItem = VideoItem(
        id = 0L, // let Room assign new ids on restore to avoid conflicts
        videoUri = o.optString("videoUri"),
        originalFileName = if (o.isNull("originalFileName")) null else o.optString("originalFileName"),
        thumbnailPath = if (o.isNull("thumbnailPath")) null else o.optString("thumbnailPath"),
        title = o.optString("title"),
        prompt = o.optString("prompt"),
        hashtags = o.optString("hashtags"),
        keywords = o.optString("keywords"),
        notes = o.optString("notes"),
        category = o.optString("category", "Other"),
        aiModel = if (o.isNull("aiModel")) null else o.optString("aiModel"),
        isFavorite = o.optBoolean("isFavorite"),
        isPinned = o.optBoolean("isPinned"),
        dateAdded = o.optLong("dateAdded", System.currentTimeMillis()),
        lastEdited = o.optLong("lastEdited", System.currentTimeMillis()),
        uploadFacebook = o.optBoolean("uploadFacebook"),
        uploadTiktok = o.optBoolean("uploadTiktok"),
        uploadYoutube = o.optBoolean("uploadYoutube"),
        uploadInstagram = o.optBoolean("uploadInstagram"),
        // Freshly restored entries are checked for reachability on next load
        isMissing = true
    )
}
