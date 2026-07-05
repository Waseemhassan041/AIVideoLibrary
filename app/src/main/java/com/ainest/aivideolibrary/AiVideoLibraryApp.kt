package com.ainest.aivideolibrary

import android.app.Application
import com.ainest.aivideolibrary.data.AppDatabase
import com.ainest.aivideolibrary.data.VideoRepository

class AiVideoLibraryApp : Application() {

    lateinit var repository: VideoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = VideoRepository(db.videoDao())
    }
}
