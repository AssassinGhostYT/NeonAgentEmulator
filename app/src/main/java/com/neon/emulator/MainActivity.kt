package com.neon.emulator

import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.emulator.model.OpenTab
import com.neon.emulator.model.PhoneModel
import com.neon.emulator.ui.drawer.FileExplorerDrawer
import com.neon.emulator.ui.editor.CodeEditorScreen
import com.neon.emulator.ui.editor.ProjectFile
import com.neon.emulator.ui.emulator.EmulatorCanvasScreen
import com.neon.emulator.ui.navigation.TabsBar
import com.neon.emulator.ui.settings.SettingsBottomSheet
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private var agentServer: AgentServer? = null
    private var webViewRef: WebView? = null
    private var serverStatusText by mutableStateOf("127.0.0.1:8080")
    private var isServerConnected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startAgentServer()

        setContent {
            NeonUniversalEmulatorApp(
                statusText = serverStatusText,
                isConnected = isServerConnected,
                onWebViewCreated = { webViewRef = it },
                onRenderUpdatedCode = { updatedCode ->
                    webViewRef?.loadDataWithBase64(updatedCode)
                }
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

@Composable
fun NeonUniversalEmulatorApp(
    statusText: String,
    isConnected: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onRenderUpdatedCode: (String) -> Unit
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showFileExplorerDrawer by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("MusicApp") }
    var selectedModel by remember { mutableStateOf(PhoneModel.SAMSUNG_A55) }

    val emulatorTab = remember { OpenTab(id = "EMULATOR_TAB", title = "📱 Emulador AVD", isEmulator = true) }
    var openTabs by remember { mutableStateOf(listOf(emulatorTab)) }
    var activeTabId by remember { mutableStateOf(emulatorTab.id) }

    val sampleMusicProject = remember(projectName) {
        ProjectFile(
            path = projectName,
            name = projectName,
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

                        class MainActivity : ComponentActivity() {
                            override fun onCreate(savedInstanceState: Bundle?) {
                                super.onCreate(savedInstanceState)
                                setContent {
                                    // Renderizando App de Música
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
                            val artist: String
                        )
                    """.trimIndent()
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
            
            // Header Discreto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showFileExplorerDrawer = !showFileExplorerDrawer },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Explorador de Archivos", tint = Color(0xFF38BDF8))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "📦 $projectName",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Configuración", tint = Color(0xFF38BDF8))
                }
            }

            // Pestañas (TabsBar)
            TabsBar(
                openTabs = openTabs,
                activeTabId = activeTabId,
                onTabSelected = { activeTabId = it },
                onCloseTab = { closedId ->
                    openTabs = openTabs.filter { it.id != closedId }
                    if (activeTabId == closedId) {
                        activeTabId = "EMULATOR_TAB"
                    }
                }
            )

            // Contenido Principal
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val activeTab = openTabs.find { it.id == activeTabId } ?: emulatorTab

                if (activeTab.isEmulator) {
                    EmulatorCanvasScreen(
                        selectedModel = selectedModel,
                        onWebViewCreated = onWebViewCreated
                    )
                } else {
                    val activeFile = activeTab.file
                    if (activeFile != null) {
                        CodeEditorScreen(
                            activeFile = activeFile,
                            onRenderUpdatedCode = onRenderUpdatedCode
                        )
                    }
                }

                if (showFileExplorerDrawer) {
                    FileExplorerDrawer(
                        projectName = projectName,
                        sampleMusicProject = sampleMusicProject,
                        onFileSelected = { file ->
                            val tabId = file.path
                            val existingTab = openTabs.find { it.id == tabId }
                            if (existingTab == null) {
                                openTabs = openTabs + OpenTab(id = tabId, title = file.name, isEmulator = false, file = file)
                            }
                            activeTabId = tabId
                            showFileExplorerDrawer = false
                        },
                        onClose = { showFileExplorerDrawer = false }
                    )
                }
            }
        }

        if (showSettingsSheet) {
            SettingsBottomSheet(
                statusText = statusText,
                projectName = projectName,
                onProjectNameChange = { projectName = it },
                selectedModel = selectedModel,
                onModelSelected = { selectedModel = it },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}
