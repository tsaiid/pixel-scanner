package com.example.pixelscanner.domain

import java.util.UUID

class SessionIdGenerator {
    fun generate(): String {
        return "session_${UUID.randomUUID()}"
    }
}
