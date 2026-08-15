package com.neon.emulator.protocol

data class DeviceStatus(
    val online: Boolean,
    val mode: String,
    val connectedClients: Int,
    val activeTasks: Int
)
