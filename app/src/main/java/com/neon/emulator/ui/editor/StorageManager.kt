package com.neon.emulator.ui.editor

import android.content.Context
import com.neon.emulator.model.ProjectItem
import java.io.File

object StorageManager {

    private fun getPrivateWorkspaceDir(context: Context): File {
        val privateDir = File(context.filesDir, "UserWorkspace")
        if (!privateDir.exists()) {
            privateDir.mkdirs()
        }
        return privateDir
    }

    fun saveFileToDevice(context: Context, projectPath: String, relativeFilePath: String, content: String): File? {
        return try {
            val workspaceDir = getPrivateWorkspaceDir(context)
            val file = File(workspaceDir, "$projectPath/$relativeFilePath")
            file.parentFile?.mkdirs()
            file.writeText(content)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteProjectOrFile(context: Context, relativePath: String): Boolean {
        return try {
            val workspaceDir = getPrivateWorkspaceDir(context)
            val target = File(workspaceDir, relativePath)
            if (target.exists()) {
                target.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ⚡ TIEMPO REAL: Escanea y lee físicamente el disco del teléfono para cargar proyectos reales
    fun loadProjectsFromDevice(context: Context): List<ProjectItem> {
        val workspaceDir = getPrivateWorkspaceDir(context)
        val projectItems = mutableListOf<ProjectItem>()

        val subFiles = workspaceDir.listFiles()
        if (subFiles != null) {
            for (f in subFiles) {
                if (f.isDirectory) {
                    val rootProjFile = scanDirectory(f, f.name)
                    projectItems.add(
                        ProjectItem(
                            id = f.name,
                            name = f.name,
                            description = "Proyecto Creado en Tiempo Real",
                            rootFolder = rootProjFile
                        )
                    )
                }
            }
        }
        return projectItems
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
