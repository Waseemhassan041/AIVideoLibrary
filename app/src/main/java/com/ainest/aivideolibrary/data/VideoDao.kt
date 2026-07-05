package com.ainest.aivideolibrary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos WHERE isDeleted = 0 ORDER BY dateAdded DESC")
    fun getAll(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeleted(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VideoItem?

    @Query("SELECT * FROM videos WHERE videoUri = :uri AND isDeleted = 0 LIMIT 1")
    suspend fun findByUri(uri: String): VideoItem?

    @Query("SELECT * FROM videos WHERE syncId = :syncId LIMIT 1")
    suspend fun findBySyncId(syncId: String): VideoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoItem): Long

    @Update
    suspend fun update(video: VideoItem)

    @Delete
    suspend fun deleteHard(video: VideoItem)

    @Query("UPDATE videos SET isDeleted = 1, deletedAt = :now WHERE id IN (:ids)")
    suspend fun softDelete(ids: List<Long>, now: Long)

    @Query("UPDATE videos SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM videos WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Query("DELETE FROM videos WHERE isDeleted = 1 AND deletedAt < :cutoff")
    suspend fun purgeExpired(cutoff: Long)

    @Query("DELETE FROM videos")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT category FROM videos WHERE isDeleted = 0")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT aiModel FROM videos WHERE isDeleted = 0 AND aiModel IS NOT NULL")
    fun getAllAiModels(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoItem>)
}
