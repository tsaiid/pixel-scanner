package com.example.pixelscanner.domain

import java.io.File

sealed interface DiskSpacePreflightResult {
    data class Sufficient(val availableBytes: Long) : DiskSpacePreflightResult
    data class Insufficient(
        val availableBytes: Long,
        val requiredBytes: Long,
    ) : DiskSpacePreflightResult
    data class Unknown(val reason: String? = null) : DiskSpacePreflightResult
}

interface DiskSpacePreflightRepository {
    fun check(
        targetPath: File,
        requiredBytes: Long,
    ): DiskSpacePreflightResult
}
