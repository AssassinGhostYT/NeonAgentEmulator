package com.neon.emulator.ui.editor

import android.content.Context
import java.io.File

object StorageManager {

    // 🔒 Almacenamiento Privado y Aislado del Proyecto Creado (/data/data/com.neon.emulator/files/UserWorkspace/)
    private fun getPrivateWorkspaceDir(context: Context): File {
        val privateDir = File(context.filesDir, "UserWorkspace")
        if (!privateDir.exists()) {
            privateDir.mkdirs()
        }
        return privateDir
    }

    // Guarda físicamente el archivo CREADO en el espacio de almacenamiento aislado del proyecto
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

    // Elimina un proyecto o archivo del almacenamiento aislado
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
}
