package com.neon.emulator.protocol

data class AgentResponse(
    val id: String,
    val success: Boolean,
    val status: String,
    val stage: String,
    val message: String,
    val error: String? = null,
    val result: Any? = null
)
