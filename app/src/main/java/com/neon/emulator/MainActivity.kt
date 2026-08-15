package com.neon.emulator

import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.NetworkInterface

enum class PhoneModel(val displayName: String, val aspectRatioWidth: Float, val cornerRadius: Int) {
    PIXEL_8_PRO("Google Pixel 8 Pro", 1f, 36),
    IPHONE_15_PRO_MAX("iPhone 15 Pro Max", 0.95f, 44),
    SAMSUNG_S24_ULTRA("Samsung Galaxy S24 Ultra", 1.05f, 16),
    IPHONE_SE("iPhone SE / Compact", 0.85f, 24)
}

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
fun NeonUniversalEmulatorApp(
    statusText: String,
    isConnected: Boolean,
    onReload: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAgentChatDialog by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(PhoneModel.PIXEL_8_PRO) }
    var chatMessages by remember { mutableStateOf(listOf("🤖 Agente IA: Hola, estoy conectado a tu emulador. ¿Qué deseas construir o probar?")) }
    var inputMessageText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ⚙️ Botón discreto de Engranaje superior + Selección de Modelo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF10B981) else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedModel.displayName,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuraciones IP & Modelos",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }

            // 📲 MARCO EMULADOR DINÁMICO (Pixel 8 Pro, iPhone 15 Pro Max, Galaxy S24, iPhone SE)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(selectedModel.aspectRatioWidth)
                    .padding(horizontal = 8.dp)
                    .shadow(16.dp, RoundedCornerShape(selectedModel.cornerRadius.dp))
                    .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(selectedModel.cornerRadius.dp))
                    .clip(RoundedCornerShape(selectedModel.cornerRadius.dp))
                    .background(Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Dynamic Island / Notch según el modelo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(Color.Black),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedModel == PhoneModel.IPHONE_15_PRO_MAX) {
                            // Dynamic Island iPhone
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                            )
                        } else {
                            // Punch hole Android
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                            )
                        }
                    }

                    // 🖥️ PANTALLA CENTRADA Y AJUSTADA (WebView Canvas)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
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

                                    val defaultHtml = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                            <style>
                                                body { background: #0F172A; color: #F8FAFC; font-family: system-ui; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }
                                                .card { background: #1E293B; border: 2px solid #38BDF8; border-radius: 20px; padding: 24px; text-align: center; box-shadow: 0 0 20px rgba(56, 189, 248, 0.3); }
                                                h1 { color: #38BDF8; font-size: 18px; margin-bottom: 8px; }
                                                p { color: #94A3B8; font-size: 12px; }
                                            </style>
                                        </head>
                                        <body>
                                            <div class="card">
                                                <h1>🤖 Neon Agent Engine</h1>
                                                <p>Listo para recibir cualquier lenguaje de programación automáticamente.</p>
                                            </div>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    val encodedHtml = Base64.encodeToString(defaultHtml.toByteArray(), Base64.NO_WRAP)
                                    loadData(encodedHtml, "text/html; charset=utf-8", "base64")

                                    onWebViewCreated(this)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Navigation Bar Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // 💬 BOTÓN FLOTANTE DEL AGENTE IA (FAB CHAT)
        FloatingActionButton(
            onClick = { showAgentChatDialog = true },
            containerColor = Color(0xFF0EA5E9),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .shadow(12.dp, CircleShape)
        ) {
            Text(text = "🤖", fontSize = 22.sp)
        }

        // ⚙️ SLIDING BOTTOM SHEET DE CONFIGURACIONES (IP & MODELOS)
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
                        text = "⚙️ Configuración del Emulador",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Dirección IP & Puerto del Servidor:", color = Color(0xFF94A3B8), fontSize = 12.sp)
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
                    Text(text = "Seleccionar Modelo de Dispositivo:", color = Color(0xFF94A3B8), fontSize = 12.sp)

                    PhoneModel.values().forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedModel = model }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedModel == model),
                                onClick = { selectedModel = model },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0EA5E9))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = model.displayName, color = Color.White, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSettingsSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar y Cerrar")
                    }
                }
            }
        }

        // 💬 DIÁLOGO DE CHAT DIRECTO CON LA IA (AGENTE FLOTANTE EN LA APP)
        if (showAgentChatDialog) {
            AlertDialog(
                onDismissRequest = { showAgentChatDialog = false },
                containerColor = Color(0xFF1E293B),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖 Agente IA Integrado", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.height(280.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            chatMessages.forEach { msg ->
                                Text(
                                    text = msg,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = inputMessageText,
                                onValueChange = { inputMessageText = it },
                                placeholder = { Text("Escribe a la IA...", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = {
                                if (inputMessageText.isNotBlank()) {
                                    chatMessages = chatMessages + "👤 Tú: $inputMessageText"
                                    chatMessages = chatMessages + "🤖 Agente IA: Recibido. Procesando en el emulador..."
                                    inputMessageText = ""
                                }
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color(0xFF0EA5E9))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAgentChatDialog = false }) {
                        Text("Cerrar Chat", color = Color(0xFF38BDF8))
                    }
                }
            )
        }
    }
}
