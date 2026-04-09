package com.example.pixelscanner.domain

interface SessionCleanupRepository {
    suspend fun cleanStaleSessions()
}
