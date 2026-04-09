package com.example.pixelscanner.core.diagnostics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.StrictMode

object DebugStrictMode {
    fun install(context: Context) {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) return

        val threadPolicy = StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
        StrictMode.setThreadPolicy(threadPolicy)

        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectLeakedClosableObjects()
            .penaltyLog()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vmPolicyBuilder.detectContentUriWithoutPermission()
        }

        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}
