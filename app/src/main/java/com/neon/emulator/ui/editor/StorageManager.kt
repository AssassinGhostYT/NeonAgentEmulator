package com.neon.emulator.ui.editor

import android.os.Environment
import java.io.File

object StorageManager {

    // Directorio raíz en la memoria del teléfono donde se guardan los proyectos reales
    private val BASE_PROJECTS_DIR = File(Environment.getExternalStorageDirectory(), "NeonEmulatorProjects")

    fun initStorage() {
        if (!BASE_PROJECTS_DIR.exists()) {
            BASE_PROJECTS_DIR.mkdirs()
        }
    }

    // Guarda físicamente un archivo en la memoria del teléfono
    fun saveFileToDevice(projectPath: String, relativeFilePath: String, content: String): File? {
        return try {
            initStorage()
            val file = File(BASE_PROJECTS_DIR, "$projectPath/$relativeFilePath")
            file.parentFile?.mkdirs()
            file.writeText(content)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Carga los proyectos reales que existen en la memoria física del dispositivo
    fun loadProjectsFromDevice(): List<ProjectFile> {
        initStorage()
        val projects = mutableListOf<ProjectFile>()
        
        val files = BASE_PROJECTS_DIR.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    projects.add(scanDirectory(f, f.name))
                }
            }
        }
        return projects
    }

    private fun scanDirectory(dir: File, relativePath: String): ProjectFile {
        val childrenList = mutableListOf<ProjectFile>()
        val children = dir.listFiles()
        if (children != null) {
            for (child in children) {
                val childRelative = "$relativePath/${child.name}"
                if (child.isDirectory) {
                    childrenList.add(scanDirectory(child, childRelative))
                } else {
                    childrenList.add(
                        ProjectFile(
                            path = childRelative,
                            name = child.name,
                            isDirectory = false,
                            content = child.readText()
                        )
                    )
                }
            }
        }
        return ProjectFile(
            path = relativePath,
            name = dir.name,
            isDirectory = true,
            children = childrenList
        )
    }
}
