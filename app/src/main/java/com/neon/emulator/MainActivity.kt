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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private var agentServer: AgentServer? = null
    private var webViewRef: WebView? = null
    private var serverStatusText by mutableStateOf("Iniciando Servidor...")
    private var isServerConnected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startAgentServer()

        setContent {
            AndroidStudioPhoneFrame(
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

// 📱 MARCO DE TELÉFONO DE ANDROID STUDIO (EMULADOR NATIVO REAL CON BEZELS, CAMERA NOTCH & NAV BAR)
@Composable
fun AndroidStudioPhoneFrame(
    statusText: String,
    isConnected: Boolean,
    onReload: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neonGlow")
    val glowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF0EA5E9),
        targetValue = Color(0xFFEC4899),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Android Studio Top Toolbar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF10B981) else Color.Red)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AVD: Pixel 8 Pro (Neon Agent Engine)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = statusText,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // 📲 EL TELÉFONO EMULADO (MARCO FÍSICO ESTILO PIXEL CON NOTCH Y BOTONES)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(36.dp), spotColor = glowColor)
                .border(3.dp, glowColor, RoundedCornerShape(36.dp))
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Speaker Notch & Camera Hole
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(Color(0xFF000000)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF334155))
                    )
                }

                // 🖥️ LA PANTALLA EMULADA DEL TELÉFONO
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
                                            body { background: #0F172A; color: #F8FAFC; font-family: system-ui; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
                                            .card { background: #1E293B; border: 2px solid #38BDF8; border-radius: 16px; padding: 24px; box-shadow: 0 0 20px rgba(56, 189, 248, 0.3); max-width: 85%; }
                                            h1 { color: #38BDF8; margin-bottom: 8px; font-size: 18px; }
                                            p { color: #94A3B8; font-size: 12px; }
                                        </style>
                                    </head>
                                    <body>
                                        <div class="card">
                                            <h1>📱 Android Studio AVD Running</h1>
                                            <p>Conexión establecida. Renderizando en pantalla emulada en tiempo real.</p>
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

                // Bottom Navigation Bar Pill (Android 14/16 Navigation Gesture Bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(Color(0xFF000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}
