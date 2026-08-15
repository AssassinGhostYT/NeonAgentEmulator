package com.neon.emulator

import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.emulator.model.OpenTab
import com.neon.emulator.model.PhoneModel
import com.neon.emulator.model.ProjectItem
import com.neon.emulator.ui.drawer.FileExplorerDrawer
import com.neon.emulator.ui.editor.CodeEditorScreen
import com.neon.emulator.ui.editor.ProjectFile
import com.neon.emulator.ui.editor.StorageManager
import com.neon.emulator.ui.emulator.EmulatorCanvasScreen
import com.neon.emulator.ui.navigation.TabsBar
import com.neon.emulator.ui.settings.SettingsBottomSheet
import org.json.JSONObject
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private var agentServer: AgentServer? = null
    private var webViewRef: WebView? = null
    private var pendingHtmlPayload: String? = null
    private var serverStatusText by mutableStateOf("127.0.0.1:8080")
    private var isServerConnected by mutableStateOf(false)

    private var globalProjectsList = mutableStateListOf<ProjectItem>()
    private var globalActiveProject by mutableStateOf<ProjectItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startAgentServer()

        setContent {
            NeonUniversalEmulatorApp(
                statusText = serverStatusText,
                isConnected = isServerConnected,
                projectsState = globalProjectsList,
                activeProjectState = globalActiveProject,
                onActiveProjectChange = { globalActiveProject = it },
                onWebViewCreated = { webView ->
                    webViewRef = webView
                    pendingHtmlPayload?.let { html ->
                        webView.loadDataWithBase64(html)
                        pendingHtmlPayload = null
                    }
                },
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
            "load_html" -> {
                if (webViewRef != null) {
                    webViewRef?.loadDataWithBase64(payload)
                } else {
                    pendingHtmlPayload = payload
                }
            }
            "eval_js" -> webViewRef?.evaluateJavascript(payload, null)
            "reload" -> webViewRef?.reload()
            "create_project" -> {
                try {
                    val json = JSONObject(payload)
                    val name = json.optString("name", "NuevoProyecto")
                    val filesArray = json.optJSONArray("files")

                    val childrenList = mutableListOf<ProjectFile>()
                    if (filesArray != null) {
                        for (i in 0 until filesArray.length()) {
                            val fObj = filesArray.getJSONObject(i)
                            val fName = fObj.optString("name", "File.kt")
                            val fPath = "$name/$fName"
                            val fContent = fObj.optString("content", "")
                            
                            childrenList.add(ProjectFile(fPath, fName, false, content = fContent))
                            StorageManager.saveFileToDevice(this, name, fName, fContent)
                        }
                    }

                    val newRoot = ProjectFile(path = name, name = name, isDirectory = true, children = childrenList)
                    val newProj = ProjectItem(name, name, "Proyecto Creado por IA", newRoot)

                    globalProjectsList.removeAll { it.name == name }
                    globalProjectsList.add(newProj)
                    globalActiveProject = newProj
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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
    projectsState: MutableList<ProjectItem>,
    activeProjectState: ProjectItem?,
    onActiveProjectChange: (ProjectItem?) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onRenderUpdatedCode: (String) -> Unit
) {
    val context = LocalContext.current
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showFileExplorerDrawer by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(PhoneModel.SAMSUNG_A55) }

    val emulatorTab = remember { OpenTab(id = "EMULATOR_TAB", title = "📱 Emulador AVD", isEmulator = true) }
    var openTabs by remember { mutableStateOf(listOf(emulatorTab)) }
    var activeTabId by remember { mutableStateOf(emulatorTab.id) }

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
                        text = if (activeProjectState != null) "📦 Proyecto: ${activeProjectState.name}" else "📦 Workspace Vacio",
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
                            projectName = activeProjectState?.name ?: "Proyecto",
                            onRenderUpdatedCode = onRenderUpdatedCode
                        )
                    }
                }

                // 📂 EXPLORADOR
                if (showFileExplorerDrawer) {
                    FileExplorerDrawer(
                        projectsList = projectsState,
                        activeProject = activeProjectState,
                        onProjectSelected = { selectedProj ->
                            onActiveProjectChange(selectedProj)
                        },
                        onFileSelected = { file ->
                            val tabId = file.path
                            val existingTab = openTabs.find { it.id == tabId }
                            if (existingTab == null) {
                                openTabs = openTabs + OpenTab(id = tabId, title = file.name, isEmulator = false, file = file)
                            }
                            activeTabId = tabId
                            showFileExplorerDrawer = false
                        },
                        onDeleteFile = { proj, fileToDelete ->
                            StorageManager.deleteProjectOrFile(context, fileToDelete.path)
                            val updatedChildren = proj.rootFolder.children.filter { it.path != fileToDelete.path }
                            val updatedRoot = proj.rootFolder.copy(children = updatedChildren)
                            val updatedProj = proj.copy(rootFolder = updatedRoot)
                            
                            val idx = projectsState.indexOfFirst { it.id == proj.id }
                            if (idx >= 0) projectsState[idx] = updatedProj
                            if (activeProjectState?.id == proj.id) onActiveProjectChange(updatedProj)

                            openTabs = openTabs.filter { it.id != fileToDelete.path }
                            if (activeTabId == fileToDelete.path) activeTabId = "EMULATOR_TAB"
                        },
                        onDeleteProject = { projToDelete ->
                            StorageManager.deleteProjectOrFile(context, projToDelete.name)
                            projectsState.removeIf { it.id == projToDelete.id }
                            if (activeProjectState?.id == projToDelete.id) {
                                onActiveProjectChange(projectsState.firstOrNull())
                            }

                            openTabs = openTabs.filter { tab -> tab.isEmulator || !tab.id.startsWith(projToDelete.name) }
                            activeTabId = "EMULATOR_TAB"
                        },
                        onCreateNewProject = {
                            val newId = System.currentTimeMillis().toString()
                            val newName = "MiProyecto_$newId"
                            val newRoot = ProjectFile(
                                path = newName,
                                name = newName,
                                isDirectory = true,
                                children = listOf(
                                    ProjectFile("$newName/MainActivity.kt", "MainActivity.kt", false, content = "package com.mi.app\n\nimport androidx.activity.ComponentActivity\n\nclass MainActivity : ComponentActivity()"),
                                    ProjectFile("$newName/AndroidManifest.xml", "AndroidManifest.xml", false, content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n</manifest>")
                                )
                            )
                            val newProjItem = ProjectItem(newId, newName, "Proyecto Creado", newRoot)
                            projectsState.add(newProjItem)
                            onActiveProjectChange(newProjItem)

                            StorageManager.saveFileToDevice(context, newName, "MainActivity.kt", "package com.mi.app\n\nclass MainActivity")
                        },
                        onClose = { showFileExplorerDrawer = false }
                    )
                }
            }
        }

        if (showSettingsSheet) {
            SettingsBottomSheet(
                statusText = statusText,
                projectName = activeProjectState?.name ?: "Sin Proyecto",
                onProjectNameChange = { newName ->
                    if (activeProjectState != null) {
                        onActiveProjectChange(activeProjectState.copy(name = newName))
                    }
                },
                selectedModel = selectedModel,
                onModelSelected = { selectedModel = it },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}
