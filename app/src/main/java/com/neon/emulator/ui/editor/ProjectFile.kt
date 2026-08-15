package com.neon.emulator.ui.editor

data class ProjectFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    var content: String = "",
    val children: List<ProjectFile> = emptyList()
)

fun flattenFiles(file: ProjectFile): List<ProjectFile> {
    val result = mutableListOf<ProjectFile>()
    result.add(file)
    if (file.isDirectory) {
        file.children.forEach { child ->
            result.addAll(flattenFiles(child))
        }
    }
    return result
}
