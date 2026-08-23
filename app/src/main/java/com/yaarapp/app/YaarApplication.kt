package com.yaarapp.app

import android.app.Application
import com.yaarapp.app.data.YaarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YaarApplication : Application() {

    lateinit var repository: YaarRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = YaarRepository(this)
        applicationScope.launch {
            repository.seedIfEmpty()
        }
    }
}
