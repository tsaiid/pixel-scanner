package com.example.pixelscanner

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class PixelScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initOpenCV()
    }

    private fun initOpenCV() {
        if (OpenCVLoader.initLocal()) {
            Log.d("PixelScanner", "OpenCV initialized successfully")
        } else {
            Log.e("PixelScanner", "OpenCV initialization failed")
        }
    }
}
