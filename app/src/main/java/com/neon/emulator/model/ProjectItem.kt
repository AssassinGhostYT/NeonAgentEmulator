package com.neon.emulator.model

import com.neon.emulator.ui.editor.ProjectFile

data class ProjectItem(
    val id: String,
    val name: String,
    val description: String,
    val rootFolder: ProjectFile
)
