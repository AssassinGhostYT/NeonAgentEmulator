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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayOutputStream
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private var agentServer: AgentServer? = null
    private var webViewRef: WebView? = null
    private var serverStatusText by mutableStateOf("Servidor: Iniciando...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startAgentServer()

        setContent {
            NeonEmulatorScreen(
                statusText = serverStatusText,
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

        serverStatusText = "🤖 Agente Universal: http://$ipAddress:$port"
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

// 🎨 Jetpack Compose UI & @Preview Component
@Composable
fun NeonEmulatorScreen(
    statusText: String,
    onReload: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Status Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = statusText,
                color = Color(0xFF38BDF8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onReload,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
            ) {
                Text(text = "Recargar", fontSize = 12.sp, color = Color.White)
            }
        }

        // Live WebView Canvas
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

                    val welcomeHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Neon Universal Agent</title>
                            <style>
                                body { background: #0F172A; color: #F8FAFC; font-family: system-ui, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
                                .card { background: #1E293B; border: 2px solid #38BDF8; border-radius: 16px; padding: 24px; box-shadow: 0 0 20px rgba(56, 189, 248, 0.3); max-width: 85%; }
                                h1 { color: #38BDF8; margin-bottom: 8px; font-size: 18px; }
                                p { color: #94A3B8; font-size: 12px; margin-bottom: 12px; }
                                .badge { background: #0EA5E9; color: white; padding: 4px 8px; border-radius: 8px; font-size: 10px; font-weight: bold; margin: 2px; display: inline-block; }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <h1>🚀 Neon Universal Agent Engine</h1>
                                <p>Soporte Nativo: Android 36 SDK, minSdk 26 (Android 8.0)</p>
                                <div>
                                    <span class="badge">Jetpack Compose (@Preview)</span>
                                    <span class="badge">Kotlin Console</span>
                                    <span class="badge">Java Console</span>
                                    <span class="badge">Flutter</span>
                                    <span class="badge">Swift / SwiftWasm</span>
                                </div>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    val encodedHtml = Base64.encodeToString(welcomeHtml.toByteArray(), Base64.NO_WRAP)
                    loadData(encodedHtml, "text/html; charset=utf-8", "base64")

                    onWebViewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// 👁️ Compose @Preview Renderer
@Preview(showBackground = true)
@Composable
fun PreviewNeonEmulatorScreen() {
    NeonEmulatorScreen(
        statusText = "🤖 Preview: Jetpack Compose Activo",
        onReload = {},
        onWebViewCreated = {}
    )
}
