package com.example.pixelscanner.core.diagnostics

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import java.io.File

object DebugLog {
    private const val TAG = "PixelScannerPerf"

    fun event(
        name: String,
        vararg fields: Pair<String, Any?>,
    ) {
        if (!shouldLog()) return
        Log.d(TAG, buildMessage(name, fields))
    }

    fun error(
        name: String,
        throwable: Throwable,
        vararg fields: Pair<String, Any?>,
    ) {
        event(name, *fields, "error" to throwable.javaClass.simpleName)
        Log.e(TAG, buildMessage(name, fields), throwable)
    }

    inline fun <T> timed(
        name: String,
        vararg fields: Pair<String, Any?>,
        block: () -> T,
    ): T {
        val startedAt = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            event(name, *fields, "durationMs" to (SystemClock.elapsedRealtime() - startedAt))
        }
    }

    suspend inline fun <T> timedSuspend(
        name: String,
        vararg fields: Pair<String, Any?>,
        crossinline block: suspend () -> T,
    ): T {
        val startedAt = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            event(name, *fields, "durationMs" to (SystemClock.elapsedRealtime() - startedAt))
        }
    }

    private fun shouldLog(): Boolean {
        return Log.isLoggable(TAG, Log.DEBUG)
    }

    private fun buildMessage(
        name: String,
        fields: Array<out Pair<String, Any?>>,
    ): String {
        return buildString {
            append("event=").append(name)
            fields.forEach { (key, value) ->
                append(' ')
                append(key)
                append('=')
                append(formatValue(value))
            }
        }
    }

    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is Boolean,
            is Number,
            is Enum<*> -> value.toString()
            is Throwable -> value.javaClass.simpleName
            is Uri -> value.redacted()
            is File -> value.name
            else -> value.toString().replace('\n', ' ').replace('\r', ' ')
        }
    }

    private fun Uri.redacted(): String {
        val schemePart = scheme ?: "unknown"
        val authorityPart = authority?.let { ",authority=$it" }.orEmpty()
        return "uri(scheme=$schemePart$authorityPart)"
    }
}
