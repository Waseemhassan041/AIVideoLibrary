package com.ainest.aivideolibrary.data

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class VideoRepository(private val dao: VideoDao) {

    val allVideos: Flow<List<VideoItem>> = dao.getAll()
    val deletedVideos: Flow<List<VideoItem>> = dao.getDeleted()
    val allCategories: Flow<List<String>> = dao.getAllCategories()
    val allAiModels: Flow<List<String>> = dao.getAllAiModels()

    suspend fun getById(id: Long): VideoItem? = dao.getById(id)

    /** Returns an existing (non-deleted) entry with the same video URI, if any. */
    suspend fun findDuplicate(uri: String): VideoItem? = dao.findByUri(uri)

    suspend fun findBySyncId(syncId: String): VideoItem? = dao.findBySyncId(syncId)

    suspend fun save(video: VideoItem): Long = dao.insert(video)

    suspend fun update(video: VideoItem) = dao.update(video.copy(lastEdited = System.currentTimeMillis()))

    suspend fun softDelete(ids: List<Long>) = dao.softDelete(ids, System.currentTimeMillis())

    suspend fun restore(id: Long) = dao.restore(id)

    suspend fun permanentlyDelete(video: VideoItem) = dao.deleteHard(video)

    suspend fun emptyRecycleBin() = dao.emptyRecycleBin()

    /** Call on app start to silently drop anything past the retention window. */
    suspend fun purgeExpiredFromRecycleBin() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RECYCLE_BIN_RETENTION_DAYS.toLong())
        dao.purgeExpired(cutoff)
    }

    suspend fun replaceAll(videos: List<VideoItem>) {
        dao.deleteAll()
        dao.insertAll(videos)
    }
}
