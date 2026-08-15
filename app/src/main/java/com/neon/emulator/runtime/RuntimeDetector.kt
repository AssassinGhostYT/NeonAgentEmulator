package com.neon.emulator.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.concurrent.TimeUnit

interface ToolRunner {
    fun checkTool(name: String, binaryPath: String?, versionFlag: String = "--version"): CapabilityItem
}

class RuntimeDetector(private val context: Context) : ToolRunner {

    fun detectCapabilities(): List<CapabilityItem> {
        val list = mutableListOf<CapabilityItem>()

        // 1. Android PackageInstaller
        list.add(
            CapabilityItem(
                name = "package_installer",
                available = true,
                status = ToolStatus.AVAILABLE,
                path = "android.content.pm.PackageInstaller",
                version = "Android SDK ${Build.VERSION.SDK_INT}",
                details = "PackageInstaller nativo de Android disponible para DEVICE_MODE"
            )
        )

        // 2. WebView Preview
        list.add(
            CapabilityItem(
                name = "webview_preview",
                available = true,
                status = ToolStatus.AVAILABLE,
                path = "android.webkit.WebView",
                version = "System WebView Component",
                details = "Renderizado de vistas maquetadas disponible para PREVIEW_ONLY"
            )
        )

        // 3. JDK / Runtime
        list.add(checkJavaRuntime())

        // 4. Compiladores y herramientas móviles nativas
        list.add(checkTool("gradle", findBinaryInPath("gradle"), "-v"))
        list.add(checkTool("gradle_wrapper", findBinaryInPath("gradlew"), "-v"))
        list.add(checkTool("kotlinc", findBinaryInPath("kotlinc"), "-version"))
        list.add(checkTool("aapt2", findBinaryInPath("aapt2"), "version"))
        list.add(checkTool("d8", findBinaryInPath("d8"), "--version"))
        list.add(checkTool("r8", findBinaryInPath("r8"), "--version"))
        list.add(checkTool("zipalign", findBinaryInPath("zipalign")))
        list.add(checkTool("apksigner", findBinaryInPath("apksigner"), "version"))
        list.add(checkAndroidSdk())
        list.add(checkBuildTools())
        list.add(checkTool("adb", findBinaryInPath("adb"), "version"))

        return list
    }

    override fun checkTool(name: String, binaryPath: String?, versionFlag: String): CapabilityItem {
        if (binaryPath == null || !File(binaryPath).exists()) {
            return CapabilityItem(
                name = name,
                available = false,
                status = ToolStatus.UNAVAILABLE,
                path = null,
                version = null,
                details = "Ejecutable no encontrado en la ruta del sistema o sandbox"
            )
        }

        return try {
            val process = ProcessBuilder(binaryPath, versionFlag)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(3, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return CapabilityItem(
                    name = name,
                    available = false,
                    status = ToolStatus.UNKNOWN,
                    path = binaryPath,
                    version = null,
                    details = "Tiempo de espera agotado al verificar la herramienta (Timeout 3s)"
                )
            }

            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.exitValue()

            if (exitCode == 0 || output.isNotEmpty()) {
                CapabilityItem(
                    name = name,
                    available = true,
                    status = ToolStatus.AVAILABLE,
                    path = binaryPath,
                    version = output.take(100),
                    details = "Herramienta verificada con éxito (ExitCode: $exitCode)"
                )
            } else {
                CapabilityItem(
                    name = name,
                    available = false,
                    status = ToolStatus.PARTIAL,
                    path = binaryPath,
                    version = null,
                    details = "El proceso terminó con error (ExitCode: $exitCode). Output: $output"
                )
            }
        } catch (e: Exception) {
            CapabilityItem(
                name = name,
                available = false,
                status = ToolStatus.UNKNOWN,
                path = binaryPath,
                version = null,
                details = "Error de ejecución: ${e.localizedMessage}"
            )
        }
    }

    private fun checkJavaRuntime(): CapabilityItem {
        val javaVersion = System.getProperty("java.version")
        val javaVendor = System.getProperty("java.vendor")
        return if (javaVersion != null) {
            CapabilityItem(
                name = "java_runtime",
                available = true,
                status = ToolStatus.AVAILABLE,
                path = System.getProperty("java.home"),
                version = "$javaVendor Java $javaVersion",
                details = "Entorno de ejecución Java detectado"
            )
        } else {
            CapabilityItem(
                name = "java_runtime",
                available = false,
                status = ToolStatus.UNAVAILABLE,
                path = null,
                version = null,
                details = "No se pudo recuperar la propiedad java.version"
            )
        }
    }

    private fun checkAndroidSdk(): CapabilityItem {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        return if (androidHome != null && File(androidHome).exists()) {
            CapabilityItem(
                name = "android_sdk",
                available = true,
                status = ToolStatus.AVAILABLE,
                path = androidHome,
                version = "SDK Root Detected",
                details = "Directorio de Android SDK localizado"
            )
        } else {
            CapabilityItem(
                name = "android_sdk",
                available = false,
                status = ToolStatus.UNAVAILABLE,
                path = null,
                version = null,
                details = "Variables ANDROID_HOME / ANDROID_SDK_ROOT no definidas o inaccesibles"
            )
        }
    }

    private fun checkBuildTools(): CapabilityItem {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val buildToolsDir = File(androidHome, "build-tools")
            if (buildToolsDir.exists() && buildToolsDir.isDirectory) {
                val versions = buildToolsDir.list()
                if (versions != null && versions.isNotEmpty()) {
                    return CapabilityItem(
                        name = "build_tools",
                        available = true,
                        status = ToolStatus.AVAILABLE,
                        path = buildToolsDir.absolutePath,
                        version = versions.joinToString(", "),
                        details = "Versiones de Build-Tools instaladas encontradas"
                    )
                }
            }
        }
        return CapabilityItem(
            name = "build_tools",
            available = false,
            status = ToolStatus.UNAVAILABLE,
            path = null,
            version = null,
            details = "Directorio build-tools no encontrado en Android SDK"
        )
    }

    private fun findBinaryInPath(binaryName: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        for (dir in pathEnv.split(File.pathSeparator)) {
            val file = File(dir, binaryName)
            if (file.exists() && file.canExecute()) {
                return file.absolutePath
            }
        }
        return null
    }

    fun getDeviceInfo(): AndroidDeviceInfo {
        val abi = if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else "unknown"
        return AndroidDeviceInfo(
            sdkInt = Build.VERSION.SDK_INT,
            release = Build.VERSION.RELEASE ?: "",
            abi = abi
        )
    }

    fun getStorageMetrics(): StorageMetrics {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val available = stat.availableBytes
            val total = stat.totalBytes
            StorageMetrics(availableBytes = available, totalBytes = total)
        } catch (e: Exception) {
            StorageMetrics(availableBytes = 0L, totalBytes = 0L)
        }
    }

    fun getMemoryMetrics(): MemoryMetrics {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            MemoryMetrics(availableBytes = memInfo.availMem, totalBytes = memInfo.totalMem)
        } catch (e: Exception) {
            MemoryMetrics(availableBytes = 0L, totalBytes = 0L)
        }
    }
}
