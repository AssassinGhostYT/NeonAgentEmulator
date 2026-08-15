package com.neon.emulator.runtime

enum class ToolStatus {
    AVAILABLE,
    PARTIAL,
    UNAVAILABLE,
    UNKNOWN
}

data class CapabilityItem(
    val name: String,
    val available: Boolean,
    val status: ToolStatus,
    val path: String?,
    val version: String?,
    val details: String
)

data class AndroidDeviceInfo(
    val sdkInt: Int,
    val release: String,
    val abi: String
)

data class StorageMetrics(
    val availableBytes: Long,
    val totalBytes: Long
)

data class MemoryMetrics(
    val availableBytes: Long,
    val totalBytes: Long
)
