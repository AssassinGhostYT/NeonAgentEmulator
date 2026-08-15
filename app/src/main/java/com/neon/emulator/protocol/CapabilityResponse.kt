package com.neon.emulator.protocol

import com.neon.emulator.runtime.AndroidDeviceInfo
import com.neon.emulator.runtime.CapabilityItem
import com.neon.emulator.runtime.MemoryMetrics
import com.neon.emulator.runtime.StorageMetrics

data class CapabilityResponse(
    val success: Boolean,
    val android: AndroidDeviceInfo,
    val storage: StorageMetrics,
    val memory: MemoryMetrics,
    val capabilities: List<CapabilityItem>
)
