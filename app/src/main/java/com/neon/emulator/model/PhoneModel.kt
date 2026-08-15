package com.neon.emulator.model

import com.neon.emulator.ui.editor.ProjectFile

enum class PhoneModel(val displayName: String, val aspectRatioWidth: Float, val cornerRadius: Int) {
    SAMSUNG_A55("Samsung Galaxy A55 5G", 0.98f, 28),
    SAMSUNG_A50("Samsung Galaxy A50", 0.94f, 24),
    SAMSUNG_S24_ULTRA("Samsung Galaxy S24 Ultra", 1.05f, 16),
    PIXEL_8_PRO("Google Pixel 8 Pro", 1f, 36),
    IPHONE_15_PRO_MAX("iPhone 15 Pro Max", 0.95f, 44),
    IPHONE_SE("iPhone SE / Compact", 0.85f, 20)
}

data class OpenTab(
    val id: String,
    val title: String,
    val isEmulator: Boolean = false,
    val file: ProjectFile? = null
)
