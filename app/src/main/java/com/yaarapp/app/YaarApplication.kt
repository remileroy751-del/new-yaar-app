package com.yaarapp.app

import android.app.Application
import com.yaarapp.app.data.YaarRepository

class YaarApplication : Application() {

    lateinit var repository: YaarRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = YaarRepository(this)
        repository.startRemoteSync()
    }
}
