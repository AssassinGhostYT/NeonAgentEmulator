package com.neon.emulator.protocol

data class BuildEvent(
    val id: String,
    val type: String, // progress, log, completed, failed, cancelled
    val stage: String,
    val message: String,
    val progress: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
