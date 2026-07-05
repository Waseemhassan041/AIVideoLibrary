package com.ainest.aivideolibrary.util

import android.content.Context
import android.util.Base64
import com.ainest.aivideolibrary.data.VideoItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Syncs metadata + a small thumbnail image per video to the signed-in user's
 * private Firestore space. Thumbnails are stored as base64 text directly in
 * the document (not Firebase Storage) so this stays on the free Spark plan -
 * no billing account needed. The actual video file is NEVER uploaded - only
 * enough to recognize and manage the entry from another device.
 */
object CloudSyncUtil {

    // Keep comfortably under Firestore's 1MB document limit even after
    // base64's ~33% size increase.
    private const val MAX_THUMBNAIL_BYTES = 500_000

    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun videosCollection(uid: String) =
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("videos")

    val isSignedIn: Boolean get() = uid() != null

    suspend fun pushVideo(context: Context, video: VideoItem): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = uid() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        try {
            var thumbnailBase64: String? = null
            video.thumbnailPath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists() && file.length() <= MAX_THUMBNAIL_BYTES) {
                        thumbnailBase64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                    }
                } catch (_: Exception) {
                }
            }
            val data = hashMapOf(
                "syncId" to video.syncId,
                "title" to video.title,
                "prompt" to video.prompt,
                "hashtags" to video.hashtags,
                "keywords" to video.keywords,
                "notes" to video.notes,
                "category" to video.category,
                "aiModel" to video.aiModel,
                "isFavorite" to video.isFavorite,
                "isPinned" to video.isPinned,
                "dateAdded" to video.dateAdded,
                "lastEdited" to video.lastEdited,
                "uploadFacebook" to video.uploadFacebook,
                "uploadTiktok" to video.uploadTiktok,
                "uploadYoutube" to video.uploadYoutube,
                "uploadInstagram" to video.uploadInstagram,
                "thumbnailBase64" to (thumbnailBase64 ?: "")
            )
            videosCollection(userId).document(video.syncId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFromCloud(video: VideoItem): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = uid() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        try {
            videosCollection(userId).document(video.syncId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class CloudVideo(
        val syncId: String,
        val title: String,
        val prompt: String,
        val hashtags: String,
        val keywords: String,
        val notes: String,
        val category: String,
        val aiModel: String?,
        val isFavorite: Boolean,
        val isPinned: Boolean,
        val dateAdded: Long,
        val lastEdited: Long,
        val uploadFacebook: Boolean,
        val uploadTiktok: Boolean,
        val uploadYoutube: Boolean,
        val uploadInstagram: Boolean,
        val thumbnailBase64: String?
    )

    suspend fun pullAll(): Result<List<CloudVideo>> = withContext(Dispatchers.IO) {
        val userId = uid() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        try {
            val snapshot = videosCollection(userId).get().await()
            val results = snapshot.documents.mapNotNull { doc ->
                val syncId = doc.getString("syncId") ?: return@mapNotNull null
                CloudVideo(
                    syncId = syncId,
                    title = doc.getString("title") ?: "",
                    prompt = doc.getString("prompt") ?: "",
                    hashtags = doc.getString("hashtags") ?: "",
                    keywords = doc.getString("keywords") ?: "",
                    notes = doc.getString("notes") ?: "",
                    category = doc.getString("category") ?: "Other",
                    aiModel = doc.getString("aiModel"),
                    isFavorite = doc.getBoolean("isFavorite") ?: false,
                    isPinned = doc.getBoolean("isPinned") ?: false,
                    dateAdded = doc.getLong("dateAdded") ?: System.currentTimeMillis(),
                    lastEdited = doc.getLong("lastEdited") ?: System.currentTimeMillis(),
                    uploadFacebook = doc.getBoolean("uploadFacebook") ?: false,
                    uploadTiktok = doc.getBoolean("uploadTiktok") ?: false,
                    uploadYoutube = doc.getBoolean("uploadYoutube") ?: false,
                    uploadInstagram = doc.getBoolean("uploadInstagram") ?: false,
                    thumbnailBase64 = doc.getString("thumbnailBase64")?.ifBlank { null }
                )
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Decodes a pulled base64 thumbnail into a local file; returns the local path. */
    fun saveThumbnailFromBase64(context: Context, syncId: String, base64: String): String? {
        return try {
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val outFile = File(dir, "$syncId.jpg")
            outFile.writeBytes(Base64.decode(base64, Base64.NO_WRAP))
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    data class ApiSettings(
        val provider: String,
        val geminiApiKey: String?,
        val claudeApiKey: String?,
        val promptTemplate: String?
    )

    private fun settingsDoc(uid: String) =
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("settings").document("aiConfig")

    /** Saves the AI provider/keys/template to the signed-in user's account. */
    suspend fun pushApiSettings(settings: ApiSettings): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = uid() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        try {
            val data = hashMapOf(
                "provider" to settings.provider,
                "geminiApiKey" to (settings.geminiApiKey ?: ""),
                "claudeApiKey" to (settings.claudeApiKey ?: ""),
                "promptTemplate" to (settings.promptTemplate ?: "")
            )
            settingsDoc(userId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Restores AI provider/keys/template - call this right after a successful sign-in. */
    suspend fun pullApiSettings(): Result<ApiSettings?> = withContext(Dispatchers.IO) {
        val userId = uid() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
        try {
            val doc = settingsDoc(userId).get().await()
            if (!doc.exists()) return@withContext Result.success(null)
            Result.success(
                ApiSettings(
                    provider = doc.getString("provider") ?: "GEMINI",
                    geminiApiKey = doc.getString("geminiApiKey")?.ifBlank { null },
                    claudeApiKey = doc.getString("claudeApiKey")?.ifBlank { null },
                    promptTemplate = doc.getString("promptTemplate")?.ifBlank { null }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
