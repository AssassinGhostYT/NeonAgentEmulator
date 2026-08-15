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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PhoneAndroid
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

enum class PhoneModel(val displayName: String, val aspectRatioWidth: Float, val cornerRadius: Int) {
    SAMSUNG_A55("Samsung Galaxy A55 5G", 0.98f, 28),
    SAMSUNG_A50("Samsung Galaxy A50", 0.94f, 24),
    SAMSUNG_S24_ULTRA("Samsung Galaxy S24 Ultra", 1.05f, 16),
    PIXEL_8_PRO("Google Pixel 8 Pro", 1f, 36),
    IPHONE_15_PRO_MAX("iPhone 15 Pro Max", 0.95f, 44),
    IPHONE_SE("iPhone SE / Compact", 0.85f, 20)
}

data class OpenTab(
    val id: String,
    val title: String,
    val isEmulator: Boolean = false,
    val file: ProjectFile? = null
)

data class ProjectFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    var content: String = "",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonIDEStudioApp(
    statusText: String,
    isConnected: Boolean,
    onReload: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onRenderUpdatedCode: (String) -> Unit
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showFileExplorerDrawer by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("MusicApp") }
    var selectedModel by remember { mutableStateOf(PhoneModel.SAMSUNG_A55) }

    // Pestaña fija del Emulador (No se puede cerrar con X)
    val emulatorTab = remember { OpenTab(id = "EMULATOR_TAB", title = "📱 Emulador AVD", isEmulator = true) }
    
    // Lista de Pestañas Abiertas
    var openTabs by remember { mutableStateOf(listOf(emulatorTab)) }
    var activeTabId by remember { mutableStateOf(emulatorTab.id) }

    // Proyecto Jetpack Compose en memoria
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
            
            // ⚙️ Header Discreto con Botón de Explorador 📂 y Engranaje ⚙️
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

            // 📌 BARRA DE PESTAÑAS (TABS BAR): Emulador Fijo + Archivos con Botón (X)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(openTabs) { tab ->
                    val isActive = (tab.id == activeTabId)
                    Surface(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { activeTabId = tab.id },
                        color = if (isActive) Color(0xFF1E293B) else Color(0xFF0B0F19),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tab.isEmulator) Icons.Default.PhoneAndroid else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (tab.isEmulator) Color(0xFF10B981) else Color(0xFFEC4899),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )

                            // ❌ Botón X solo para archivos (El Emulador NO tiene X)
                            if (!tab.isEmulator) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar Pestaña",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            openTabs = openTabs.filter { it.id != tab.id }
                                            if (activeTabId == tab.id) {
                                                activeTabId = "EMULATOR_TAB" // Regresa automáticamente al emulador
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // 🔄 CONTENIDO SEGÚN LA PESTAÑA SELECCIONADA
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val activeTab = openTabs.find { it.id == activeTabId } ?: emulatorTab

                if (activeTab.isEmulator) {
                    // 📲 PESTAÑA EMULADOR (Permanece siempre activa)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(selectedModel.cornerRadius.dp))
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
                } else {
                    // ✏️ PESTAÑA EDITOR DE CÓDIGO (Para el archivo seleccionado)
                    val activeFile = activeTab.file
                    if (activeFile != null) {
                        var codeContent by remember(activeFile.id) { mutableStateOf(activeFile.content) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D1117))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✏️ Editando: ${activeFile.name}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Button(
                                    onClick = {
                                        activeFile.content = codeContent
                                        // Renderizar los cambios al instante en el emulador
                                        onRenderUpdatedCode(codeContent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Guardar", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aplicar Cambios", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = codeContent,
                                onValueChange = { codeContent = it },
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

                // 📂 PANEL EXPLORADOR DESLIZABLE
                if (showFileExplorerDrawer) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(260.dp)
                            .align(Alignment.CenterStart),
                        color = Color(0xFF0F172A),
                        shadowElevation = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📁 Archivos de $projectName", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showFileExplorerDrawer = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn {
                                items(flattenFiles(sampleMusicProject)) { file ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!file.isDirectory) {
                                                    val tabId = file.path
                                                    val existingTab = openTabs.find { it.id == tabId }
                                                    if (existingTab == null) {
                                                        val newTab = OpenTab(id = tabId, title = file.name, isEmulator = false, file = file)
                                                        openTabs = openTabs + newTab
                                                    }
                                                    activeTabId = tabId
                                                    showFileExplorerDrawer = false
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = if (file.isDirectory) Color(0xFF38BDF8) else Color(0xFFEC4899),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = file.name, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ⚙️ SLIDING BOTTOM SHEET DE CONFIGURACIONES
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
                        text = "⚙️ Configuración del Emulador & Dispositivo",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Nombre del Proyecto Activo:", color = Color.White, fontSize = 12.sp)
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Seleccionar Modelo de Dispositivo:", color = Color.White, fontSize = 12.sp)

                    PhoneModel.values().forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedModel = model }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedModel == model),
                                onClick = { selectedModel = model },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0EA5E9))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = model.displayName, color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSettingsSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar y Aplicar", color = Color.White)
                    }
                }
            }
        }
    }
}
