package com.example.pixelscanner

import android.app.Application
import com.example.pixelscanner.app.AppContainer

class PixelScannerApp : Application() {
    val appContainer by lazy { AppContainer() }

    override fun onCreate() {
        super.onCreate()
    }
}
