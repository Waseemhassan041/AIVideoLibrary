package com.ainest.aivideolibrary.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainest.aivideolibrary.AiVideoLibraryApp
import com.ainest.aivideolibrary.data.DashboardFilter
import com.ainest.aivideolibrary.data.SortOption
import com.ainest.aivideolibrary.data.VideoItem
import com.ainest.aivideolibrary.util.AiAutoFillResult
import com.ainest.aivideolibrary.util.AiAutoFillUtil
import com.ainest.aivideolibrary.util.AiProvider
import com.ainest.aivideolibrary.util.AuthUtil
import com.ainest.aivideolibrary.util.BackupRestoreUtil
import com.ainest.aivideolibrary.util.CloudSyncUtil
import com.ainest.aivideolibrary.util.CsvExportUtil
import com.ainest.aivideolibrary.util.MediaRelinkUtil
import com.ainest.aivideolibrary.util.PrefsManager
import com.ainest.aivideolibrary.util.RelinkCandidate
import com.ainest.aivideolibrary.util.ThumbnailUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchScope(val label: String) {
    ALL("All"), TITLE("Title"), PROMPT("Prompt"), KEYWORDS("Keywords"),
    CATEGORY("Category"), AI_MODEL("AI Model")
}

data class FilterState(
    val dashboardFilter: DashboardFilter = DashboardFilter.NONE,
    val category: String? = null,
    val aiModel: String? = null
) {
    val isActive: Boolean
        get() = dashboardFilter != DashboardFilter.NONE || category != null || aiModel != null
}

data class DashboardStats(
    val total: Int = 0,
    val facebook: Int = 0,
    val tiktok: Int = 0,
    val youtube: Int = 0,
    val instagram: Int = 0,
    val favorites: Int = 0
)

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AiVideoLibraryApp).repository
    private val prefs = PrefsManager(application)

    init {
        viewModelScope.launch { repository.purgeExpiredFromRecycleBin() }
        runAutoSyncIfDue()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchScope = MutableStateFlow(SearchScope.ALL)
    val searchScope: StateFlow<SearchScope> = _searchScope

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds
    val isSelectionMode: StateFlow<Boolean> = _selectedIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val categories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiModels: StateFlow<List<String>> = repository.allAiModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allVideos: StateFlow<List<VideoItem>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinVideos: StateFlow<List<VideoItem>> = repository.deletedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayedVideos: StateFlow<List<VideoItem>> = combine(
        allVideos, _searchQuery, _searchScope, _filterState, _sortOption
    ) { videos, query, scope, filters, sort ->
        applyQuery(videos, query, scope, filters, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = allVideos
        .map { v ->
            DashboardStats(
                total = v.size,
                facebook = v.count { it.uploadFacebook },
                tiktok = v.count { it.uploadTiktok },
                youtube = v.count { it.uploadYoutube },
                instagram = v.count { it.uploadInstagram },
                favorites = v.count { it.isFavorite }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    private fun applyQuery(
        videos: List<VideoItem>,
        query: String,
        scope: SearchScope,
        filters: FilterState,
        sort: SortOption
    ): List<VideoItem> {
        var result = videos

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { v ->
                when (scope) {
                    SearchScope.ALL -> v.title.lowercase().contains(q) ||
                        v.prompt.lowercase().contains(q) ||
                        v.keywords.lowercase().contains(q) ||
                        v.category.lowercase().contains(q) ||
                        (v.aiModel?.lowercase()?.contains(q) == true)
                    SearchScope.TITLE -> v.title.lowercase().contains(q)
                    SearchScope.PROMPT -> v.prompt.lowercase().contains(q)
                    SearchScope.KEYWORDS -> v.keywords.lowercase().contains(q)
                    SearchScope.CATEGORY -> v.category.lowercase().contains(q)
                    SearchScope.AI_MODEL -> v.aiModel?.lowercase()?.contains(q) == true
                }
            }
        }

        when (filters.dashboardFilter) {
            DashboardFilter.FACEBOOK -> result = result.filter { it.uploadFacebook }
            DashboardFilter.TIKTOK -> result = result.filter { it.uploadTiktok }
            DashboardFilter.YOUTUBE -> result = result.filter { it.uploadYoutube }
            DashboardFilter.INSTAGRAM -> result = result.filter { it.uploadInstagram }
            DashboardFilter.FAVORITES -> result = result.filter { it.isFavorite }
            DashboardFilter.NONE -> {}
        }
        filters.category?.let { c -> result = result.filter { it.category == c } }
        filters.aiModel?.let { m -> result = result.filter { it.aiModel == m } }

        result = when (sort) {
            SortOption.NEWEST -> result.sortedByDescending { it.dateAdded }
            SortOption.OLDEST -> result.sortedBy { it.dateAdded }
            SortOption.A_Z -> result.sortedBy { it.title.lowercase() }
            SortOption.RECENTLY_EDITED -> result.sortedByDescending { it.lastEdited }
            SortOption.FAVORITES -> result.sortedByDescending { it.isFavorite }
        }

        // Pinned items always float to the top, preserving the chosen sort within each group.
        return result.sortedByDescending { it.isPinned }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSearchScope(s: SearchScope) { _searchScope.value = s }
    fun setFilterState(f: FilterState) { _filterState.value = f }
    fun setSortOption(s: SortOption) { _sortOption.value = s }

    fun toggleDashboardFilter(filter: DashboardFilter) {
        val current = _filterState.value
        _filterState.value = current.copy(
            dashboardFilter = if (current.dashboardFilter == filter) DashboardFilter.NONE else filter
        )
    }

    // --- Multi-select ---
    fun toggleSelected(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
    }

    fun clearSelection() { _selectedIds.value = emptySet() }

    suspend fun getVideo(id: Long): VideoItem? = repository.getById(id)

    /** Returns true if a non-deleted entry already points at this exact URI. */
    suspend fun isDuplicate(uri: String): Boolean = repository.findDuplicate(uri) != null

    fun addOrUpdateVideo(video: VideoItem, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = if (video.id == 0L) repository.save(video) else {
                repository.update(video)
                video.id
            }
            onDone(id)
            if (CloudSyncUtil.isSignedIn) {
                val saved = repository.getById(id)
                if (saved != null) CloudSyncUtil.pushVideo(getApplication(), saved)
            }
        }
    }

    // --- Delete / Recycle Bin ---
    fun softDeleteSelected(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            if (CloudSyncUtil.isSignedIn) {
                ids.forEach { id -> repository.getById(id)?.let { CloudSyncUtil.deleteFromCloud(it) } }
            }
            repository.softDelete(ids)
            clearSelection()
            onDone()
        }
    }

    fun softDeleteSingle(video: VideoItem) {
        viewModelScope.launch {
            if (CloudSyncUtil.isSignedIn) CloudSyncUtil.deleteFromCloud(video)
            repository.softDelete(listOf(video.id))
        }
    }

    fun restoreFromBin(video: VideoItem) {
        viewModelScope.launch { repository.restore(video.id) }
    }

    fun permanentlyDelete(video: VideoItem) {
        viewModelScope.launch {
            ThumbnailUtil.deleteThumbnail(video.thumbnailPath)
            repository.permanentlyDelete(video)
        }
    }

    fun emptyRecycleBin(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            recycleBinVideos.value.forEach { ThumbnailUtil.deleteThumbnail(it.thumbnailPath) }
            repository.emptyRecycleBin()
            onDone()
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch { repository.update(video.copy(isFavorite = !video.isFavorite)) }
    }

    fun togglePinned(video: VideoItem) {
        viewModelScope.launch { repository.update(video.copy(isPinned = !video.isPinned)) }
    }

    fun toggleUploadStatus(video: VideoItem, platform: String) {
        val updated = when (platform) {
            "FB" -> video.copy(uploadFacebook = !video.uploadFacebook)
            "TT" -> video.copy(uploadTiktok = !video.uploadTiktok)
            "YT" -> video.copy(uploadYoutube = !video.uploadYoutube)
            "IG" -> video.copy(uploadInstagram = !video.uploadInstagram)
            else -> video
        }
        viewModelScope.launch { repository.update(updated) }
    }

    suspend fun generateThumbnail(uri: Uri): String? = ThumbnailUtil.generateThumbnail(getApplication(), uri)

    // --- Backup / Restore ---
    fun exportBackup(uri: Uri, password: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = BackupRestoreUtil.export(getApplication(), uri, allVideos.value, password)
            onDone(ok)
        }
    }

    fun importBackup(uri: Uri, password: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val videos = BackupRestoreUtil.import(getApplication(), uri, password)
            if (videos == null) {
                onDone(false)
                return@launch
            }
            repository.replaceAll(videos)
            onDone(true)
            runAutoRelinkPass()
        }
    }

    /**
     * Runs after a restore: for every entry whose original URI no longer resolves
     * on this device, tries to find a video with a matching file name and,
     * if found, re-links to it automatically. Anything left unmatched just
     * stays flagged as missing (shown with a warning badge), and can be fixed
     * via "Locate Video" in the card menu.
     */
    private suspend fun runAutoRelinkPass() {
        val current = repository.allVideos.first()
        for (video in current) {
            val reachable = MediaRelinkUtil.isReachable(getApplication(), video.videoUri)
            if (!reachable) {
                val match = findAutoRelinkMatch(video)
                if (match != null) {
                    applyRelinkSuspend(video, match.uri)
                } else {
                    markMissingSuspend(video, true)
                }
            }
        }
    }

    suspend fun isBackupEncrypted(uri: Uri): Boolean = BackupRestoreUtil.isEncrypted(getApplication(), uri)

    fun exportCsv(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = CsvExportUtil.export(getApplication(), uri, allVideos.value)
            onDone(ok)
        }
    }

    // --- Re-link ---
    suspend fun checkReachable(video: VideoItem): Boolean =
        MediaRelinkUtil.isReachable(getApplication(), video.videoUri)

    suspend fun findAutoRelinkMatch(video: VideoItem): RelinkCandidate? {
        val name = video.originalFileName ?: MediaRelinkUtil.guessFileName(video.videoUri, video.title)
        return MediaRelinkUtil.findMatchByName(getApplication(), name)
    }

    fun applyRelink(video: VideoItem, newUri: Uri) {
        viewModelScope.launch { applyRelinkSuspend(video, newUri) }
    }

    private suspend fun applyRelinkSuspend(video: VideoItem, newUri: Uri) {
        val name = com.ainest.aivideolibrary.util.UriUtil.getDisplayName(getApplication(), newUri)
        repository.update(
            video.copy(videoUri = newUri.toString(), originalFileName = name, isMissing = false)
        )
    }

    fun markMissing(video: VideoItem, missing: Boolean) {
        viewModelScope.launch { markMissingSuspend(video, missing) }
    }

    private suspend fun markMissingSuspend(video: VideoItem, missing: Boolean) {
        repository.update(video.copy(isMissing = missing))
    }

    // --- Cloud Sync ---
    private val _isSignedIn = MutableStateFlow(AuthUtil.currentUser() != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    fun refreshSignInState() {
        _isSignedIn.value = AuthUtil.currentUser() != null
    }

    /** Pulls cloud entries + AI settings, and pushes local AI settings - the one full sync action. */
    fun syncNow(onDone: (Boolean) -> Unit = {}) {
        if (!CloudSyncUtil.isSignedIn) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            var ok = true

            // Bring in any videos added from other devices.
            CloudSyncUtil.pullAll().onSuccess { cloudVideos ->
                cloudVideos.forEach { cv ->
                    val existing = repository.findBySyncId(cv.syncId)
                    if (existing == null) {
                        val thumbPath = cv.thumbnailBase64?.let {
                            CloudSyncUtil.saveThumbnailFromBase64(getApplication(), cv.syncId, it)
                        }
                        repository.save(
                            VideoItem(
                                videoUri = "",
                                syncId = cv.syncId,
                                thumbnailPath = thumbPath,
                                title = cv.title,
                                prompt = cv.prompt,
                                hashtags = cv.hashtags,
                                keywords = cv.keywords,
                                notes = cv.notes,
                                category = cv.category,
                                aiModel = cv.aiModel,
                                isFavorite = cv.isFavorite,
                                isPinned = cv.isPinned,
                                dateAdded = cv.dateAdded,
                                lastEdited = cv.lastEdited,
                                uploadFacebook = cv.uploadFacebook,
                                uploadTiktok = cv.uploadTiktok,
                                uploadYoutube = cv.uploadYoutube,
                                uploadInstagram = cv.uploadInstagram,
                                isMissing = true
                            )
                        )
                    }
                }
            }.onFailure { ok = false }

            // Push any local videos not yet in the cloud (e.g. added while offline).
            val local = repository.allVideos.first()
            local.forEach { CloudSyncUtil.pushVideo(getApplication(), it) }

            // AI settings: pull first and only fill in fields that are currently
            // blank locally (e.g. right after clearing app data) - never touches
            // a field that already has a value. Then push, which by this point
            // only sends real values, never overwrites the cloud with blanks.
            CloudSyncUtil.pullApiSettings().onSuccess { settings ->
                if (settings != null) {
                    if (prefs.geminiApiKey.isNullOrBlank()) settings.geminiApiKey?.let { prefs.geminiApiKey = it }
                    if (prefs.claudeApiKey.isNullOrBlank()) settings.claudeApiKey?.let { prefs.claudeApiKey = it }
                    if (prefs.aiPromptTemplate.isNullOrBlank()) settings.promptTemplate?.let { prefs.aiPromptTemplate = it }
                }
            }
            CloudSyncUtil.pushApiSettings(
                CloudSyncUtil.ApiSettings(
                    provider = prefs.aiProvider,
                    geminiApiKey = prefs.geminiApiKey,
                    claudeApiKey = prefs.claudeApiKey,
                    promptTemplate = prefs.aiPromptTemplate
                )
            )

            onDone(ok)
            if (ok) prefs.lastSyncTimestamp = System.currentTimeMillis()
        }
    }

    /** Called once at app start: runs a sync automatically if the chosen interval has elapsed. */
    fun runAutoSyncIfDue() {
        if (!CloudSyncUtil.isSignedIn) return
        val intervalHours = prefs.autoSyncIntervalHours
        if (intervalHours <= 0) return
        val elapsedMs = System.currentTimeMillis() - prefs.lastSyncTimestamp
        val intervalMs = intervalHours * 60L * 60L * 1000L
        if (elapsedMs >= intervalMs) {
            syncNow()
        }
    }

    fun signOut() {
        AuthUtil.signOut(getApplication())
        refreshSignInState()
    }

    // --- AI Auto-fill ---
    suspend fun autoFillFromPrompt(prompt: String): Result<AiAutoFillResult> {
        val providerName = prefs.aiProvider
        val provider = if (providerName == "CLAUDE") AiProvider.CLAUDE else AiProvider.GEMINI
        val apiKey = if (provider == AiProvider.CLAUDE) prefs.claudeApiKey else prefs.geminiApiKey
        if (apiKey.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No API key set for $providerName. Add one in Settings."))
        }
        val template = prefs.aiPromptTemplate ?: AiAutoFillUtil.DEFAULT_TEMPLATE
        return AiAutoFillUtil.generate(provider, apiKey, prompt, template)
    }
}
