package com.neon.emulator.protocol

data class AgentRequest(
    val id: String? = null,
    val action: String,
    val projectId: String? = null,
    val prompt: String? = null,
    val token: String? = null,
    val options: Map<String, Any>? = null,
    val payload: String? = null
)
