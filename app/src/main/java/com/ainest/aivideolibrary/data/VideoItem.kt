package com.ainest.aivideolibrary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single library entry. Only the URI + metadata are stored here.
 * The actual video file is NEVER copied or moved by this app.
 */
@Entity(tableName = "videos")
data class VideoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Persisted content:// URI (or file:// path) pointing to the original video.
    val videoUri: String,

    // Stable ID used for cloud sync across devices (local Room id differs per device).
    val syncId: String = java.util.UUID.randomUUID().toString(),

    // Captured at pick-time; used to auto re-link if the URI stops resolving later.
    val originalFileName: String? = null,

    // Path to a small cached thumbnail image stored in the app's private files dir.
    val thumbnailPath: String? = null,

    val title: String = "",
    val prompt: String = "",
    val hashtags: String = "",
    val keywords: String = "",
    val notes: String = "",

    val category: String = "Other",
    // null = "Select" (no AI model chosen yet) -> no badge shown on thumbnail
    val aiModel: String? = null,

    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,

    val dateAdded: Long = System.currentTimeMillis(),
    val lastEdited: Long = System.currentTimeMillis(),

    val uploadFacebook: Boolean = false,
    val uploadTiktok: Boolean = false,
    val uploadYoutube: Boolean = false,
    val uploadInstagram: Boolean = false,

    // Soft delete / recycle bin
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,

    // Set when the original file can't be resolved on this device (e.g. after restore)
    val isMissing: Boolean = false
)

object Categories {
    val defaults = listOf("Baby", "Motorcycle", "Fish Spa", "Other")
}

object AiModels {
    // "Select" = null aiModel (no badge shown). "Dola" per user request, sits right after Select.
    val defaults = listOf("Select", "Dola", "Veo", "Runway", "Kling", "Pika", "Hailuo", "Other")
}

enum class SortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    A_Z("A-Z"),
    RECENTLY_EDITED("Recently Edited"),
    FAVORITES("Favorites")
}

enum class DashboardFilter {
    NONE, FACEBOOK, TIKTOK, YOUTUBE, INSTAGRAM, FAVORITES
}

const val RECYCLE_BIN_RETENTION_DAYS = 10
