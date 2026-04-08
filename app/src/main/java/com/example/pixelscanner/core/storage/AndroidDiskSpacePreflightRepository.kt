package com.example.pixelscanner.core.storage

import android.os.StatFs
import com.example.pixelscanner.domain.DiskSpacePreflightRepository
import com.example.pixelscanner.domain.DiskSpacePreflightResult
import java.io.File

class AndroidDiskSpacePreflightRepository : DiskSpacePreflightRepository {
    override fun check(targetPath: File, requiredBytes: Long): DiskSpacePreflightResult {
        return runCatching {
            StatFs(targetPath.absolutePath).availableBytes
        }.fold(
            onSuccess = { availableBytes ->
                if (availableBytes >= requiredBytes) {
                    DiskSpacePreflightResult.Sufficient(availableBytes)
                } else {
                    DiskSpacePreflightResult.Insufficient(
                        availableBytes = availableBytes,
                        requiredBytes = requiredBytes,
                    )
                }
            },
            onFailure = { error ->
                DiskSpacePreflightResult.Unknown(error.message)
            },
        )
    }
}
