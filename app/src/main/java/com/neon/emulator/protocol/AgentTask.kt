package com.neon.emulator.protocol

data class AgentTask(
    val id: String,
    val action: String,
    val projectId: String,
    var status: String,
    var stage: String,
    var message: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var progress: Int = 0,
    var error: String? = null,
    var result: Any? = null
)
