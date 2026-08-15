package com.neon.emulator

import android.os.Bundle
import android.util.Base64
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.NetworkInterface

enum class ViewMode { EMULATOR, PROJECT_EXPLORER, FILE_EDITOR }

data class ProjectFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val content: String = "",
    val children: List<ProjectFile> = emptyList()
)

class MainActivity : ComponentActivity() {

    private var agentServer: AgentServer? = null
    private var webViewRef: WebView? = null
    private var serverStatusText by mutableStateOf("127.0.0.1:8080")
    private var isServerConnected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startAgentServer()

        setContent {
            NeonIDEStudioApp(
                statusText = serverStatusText,
                isConnected = isServerConnected,
                onReload = { webViewRef?.reload() },
                onWebViewCreated = { webViewRef = it }
            )
        }
    }

    private fun startAgentServer() {
        val port = 8080
        val ipAddress = getLocalIpAddress() ?: "127.0.0.1"

        agentServer = AgentServer(port) { command, payload ->
            runOnUiThread {
                handleAgentCommand(command, payload)
            }
        }
        agentServer?.start()

        serverStatusText = "http://$ipAddress:$port"
        isServerConnected = true
    }

    private fun handleAgentCommand(command: String, payload: String) {
        when (command) {
            "load_url" -> webViewRef?.loadUrl(payload)
            "load_html" -> webViewRef?.loadDataWithBase64(payload)
            "eval_js" -> webViewRef?.evaluateJavascript(payload, null)
            "reload" -> webViewRef?.reload()
        }
    }

    private fun WebView.loadDataWithBase64(htmlContent: String) {
        val encodedHtml = Base64.encodeToString(htmlContent.toByteArray(), Base64.NO_WRAP)
        this.loadData(encodedHtml, "text/html; charset=utf-8", "base64")
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        agentServer?.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonIDEStudioApp(
    statusText: String,
    isConnected: Boolean,
    onReload: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    var currentViewMode by remember { mutableStateOf(ViewMode.EMULATOR) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("MusicApp") }
    var activeEditingFile by remember { mutableStateOf<ProjectFile?>(null) }
    var fileCodeContent by remember { mutableStateOf("") }

    // Estructura oficial del proyecto Jetpack Compose en memoria
    val sampleMusicProject = remember(projectName) {
        ProjectFile(
            path = projectName,
            name = projectName,
            isDirectory = true,
            children = listOf(
                ProjectFile(
                    path = "$projectName/app",
                    name = "app",
                    isDirectory = true,
                    children = listOf(
                        ProjectFile(
                            path = "$projectName/app/build.gradle.kts",
                            name = "build.gradle.kts",
                            isDirectory = false,
                            content = """
                                plugins {
                                    id("com.android.application")
                                    id("org.jetbrains.kotlin.android")
                                }
                                android {
                                    namespace = "com.ejemplo.musicapp"
                                    compileSdk = 34
                                    buildFeatures { compose = true }
                                }
                            """.trimIndent()
                        ),
                        ProjectFile(
                            path = "$projectName/app/src/main/AndroidManifest.xml",
                            name = "AndroidManifest.xml",
                            isDirectory = false,
                            content = """
                                <?xml version="1.0" encoding="utf-8"?>
                                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                                    <application
                                        android:label="MusicApp"
                                        android:theme="@style/Theme.MusicApp">
                                        <activity android:name=".MainActivity" android:exported="true">
                                            <intent-filter>
                                                <action android:name="android.intent.action.MAIN" />
                                                <category android:name="android.intent.category.LAUNCHER" />
                                            </intent-filter>
                                        </activity>
                                    </application>
                                </manifest>
                            """.trimIndent()
                        ),
                        ProjectFile(
                            path = "$projectName/app/src/main/java/com/ejemplo/musicapp",
                            name = "src/main/java/com/ejemplo/musicapp",
                            isDirectory = true,
                            children = listOf(
                                ProjectFile(
                                    path = "$projectName/app/src/main/java/com/ejemplo/musicapp/MainActivity.kt",
                                    name = "MainActivity.kt",
                                    isDirectory = false,
                                    content = """
                                        package com.ejemplo.musicapp

                                        import android.os.Bundle
                                        import androidx.activity.ComponentActivity
                                        import androidx.activity.compose.setContent
                                        import com.ejemplo.musicapp.ui.theme.MusicAppTheme

                                        class MainActivity : ComponentActivity() {
                                            override fun onCreate(savedInstanceState: Bundle?) {
                                                super.onCreate(savedInstanceState)
                                                setContent {
                                                    MusicAppTheme {
                                                        // Renderizando App de Música
                                                    }
                                                }
                                            }
                                        }
                                    """.trimIndent()
                                ),
                                ProjectFile(
                                    path = "$projectName/app/src/main/java/com/ejemplo/musicapp/ui/screens/HomeScreen.kt",
                                    name = "HomeScreen.kt",
                                    isDirectory = false,
                                    content = """
                                        package com.ejemplo.musicapp.ui.screens

                                        import androidx.compose.runtime.Composable
                                        import androidx.compose.material3.Text

                                        @Composable
                                        fun HomeScreen() {
                                            Text(text = "Reproductor Cyberpunk Synthwave", color = Color.White)
                                        }
                                    """.trimIndent()
                                ),
                                ProjectFile(
                                    path = "$projectName/app/src/main/java/com/ejemplo/musicapp/domain/model/Song.kt",
                                    name = "Song.kt",
                                    isDirectory = false,
                                    content = """
                                        package com.ejemplo.musicapp.domain.model

                                        data class Song(
                                            val id: String,
                                            val title: String,
                                            val artist: String,
                                            val coverUrl: String
                                        )
                                    """.trimIndent()
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // ⚙️ Top Bar IDE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentViewMode != ViewMode.EMULATOR) {
                        IconButton(
                            onClick = { currentViewMode = ViewMode.EMULATOR },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver al Emulador", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (currentViewMode == ViewMode.FILE_EDITOR) activeEditingFile?.name ?: "Editor" else "📦 Proyecto: $projectName",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            currentViewMode = if (currentViewMode == ViewMode.PROJECT_EXPLORER) ViewMode.EMULATOR else ViewMode.PROJECT_EXPLORER
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Explorador de Archivos", tint = Color(0xFF38BDF8))
                    }

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración", tint = Color(0xFF38BDF8))
                    }
                }
            }

            // 🔄 CUERPO PRINCIPAL SWAPPER: EMULADOR <-> EXPLORADOR <-> EDITOR DE CÓDIGO NATIVO
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentViewMode) {
                    ViewMode.EMULATOR -> {
                        // 📲 Vista Emulador Nativo
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.allowFileAccess = true
                                        settings.allowContentAccess = true
                                        settings.allowUniversalAccessFromFileURLs = true
                                        webViewClient = WebViewClient()
                                        webChromeClient = WebChromeClient()

                                        onWebViewCreated(this)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    ViewMode.PROJECT_EXPLORER -> {
                        // 📂 Explorador de Archivos del Proyecto Android Studio
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A))
                                .padding(16.dp)
                        ) {
                            Text("📁 Estructura del Proyecto Jetpack Compose", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyColumn {
                                items(flattenFiles(sampleMusicProject)) { file ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!file.isDirectory) {
                                                    activeEditingFile = file
                                                    fileCodeContent = file.content
                                                    currentViewMode = ViewMode.FILE_EDITOR
                                                }
                                            }
                                            .padding(vertical = 8.dp, horizontal = (file.path.split("/").size * 8).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = if (file.isDirectory) Color(0xFF38BDF8) else Color(0xFFEC4899),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = file.name, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    ViewMode.FILE_EDITOR -> {
                        // 💻 Editor de Código Kotlin Completo
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D1117))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✏️ Editando: ${activeEditingFile?.name}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Button(
                                    onClick = {
                                        // Guardar cambios en el dispositivo móvil
                                        currentViewMode = ViewMode.EMULATOR
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Guardar", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Guardar en Móvil", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = fileCodeContent,
                                onValueChange = { fileCodeContent = it },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = LocalTextStyle.current.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                        }
                    }
                }
            }
        }

        // ⚙️ SLIDING BOTTOM SHEET DE CONFIGURACIONES & CREACIÓN DE PROYECTO
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "⚙️ Configuración del Proyecto",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Nombre del Proyecto / App a Crear:", color = Color.White, fontSize = 12.sp)
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Servidor Agente (Antigravity IP):", color = Color.White, fontSize = 12.sp)
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSettingsSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear / Cargar Proyecto", color = Color.White)
                    }
                }
            }
        }
    }
}

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
