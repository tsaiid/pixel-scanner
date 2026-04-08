package com.example.pixelscanner

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.example.pixelscanner.app.AppContainer

class PixelScannerApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            appContainer.sessionCleanupRepository.cleanStaleSessions()
            appContainer.exportArtifactCacheRepository.cleanStaleTempArtifacts(applicationContext)
        }
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }
}
