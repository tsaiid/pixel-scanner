package com.example.pixelscanner

import android.app.Application
import com.example.pixelscanner.app.AppContainer
import com.example.pixelscanner.core.diagnostics.DebugStrictMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PixelScannerApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        DebugStrictMode.install(this)
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
