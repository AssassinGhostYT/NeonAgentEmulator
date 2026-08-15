package com.neon.emulator.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object PackageInstallerManager {

    enum class InstallStatus {
        INSTALL_READY,
        USER_CONFIRMATION_REQUIRED,
        INSTALLING,
        INSTALLED,
        INSTALL_FAILED,
        INSTALL_CANCELLED
    }

    data class InstallResult(
        val status: InstallStatus,
        val message: String,
        val packageName: String? = null
    )

    fun installApk(context: Context, apkFile: File): InstallResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return InstallResult(
                status = InstallStatus.INSTALL_FAILED,
                message = "El archivo APK no existe o está vacío (0 bytes)"
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)

            InstallResult(
                status = InstallStatus.USER_CONFIRMATION_REQUIRED,
                message = "Se abrió el instalador oficial de Android. Confirmación del usuario requerida."
            )
        } catch (e: Exception) {
            InstallResult(
                status = InstallStatus.INSTALL_FAILED,
                message = "Error al lanzar el instalador nativo: ${e.localizedMessage}"
            )
        }
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun launchInstalledApp(context: Context, packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
