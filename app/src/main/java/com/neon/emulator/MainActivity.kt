package com.neon.emulator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Base64
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private lateinit var webViewCanvas: WebView
    private lateinit var tvStatus: TextView
    private lateinit var btnReload: Button

    private var agentServer: AgentServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webViewCanvas = findViewById(R.id.webViewCanvas)
        tvStatus = findViewById(R.id.tvStatus)
        btnReload = findViewById(R.id.btnReload)

        setupWebView()
        startAgentServer()

        btnReload.setOnClickListener {
            webViewCanvas.reload()
        }
    }

    private fun setupWebView() {
        val settings: WebSettings = webViewCanvas.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        webViewCanvas.webViewClient = WebViewClient()
        webViewCanvas.webChromeClient = WebChromeClient()

        // HTML inicial de bienvenida del Emulador
        val welcomeHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        background: #0F172A;
                        color: #F8FAFC;
                        font-family: system-ui, sans-serif;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 100vh;
                        margin: 0;
                        text-align: center;
                    }
                    .card {
                        background: #1E293B;
                        border: 2px solid #38BDF8;
                        border-radius: 16px;
                        padding: 24px;
                        box-shadow: 0 0 20px rgba(56, 189, 248, 0.3);
                        max-width: 80%;
                    }
                    h1 { color: #38BDF8; margin-bottom: 8dp; }
                    p { color: #94A3B8; }
                    .badge {
                        background: #0EA5E9;
                        color: white;
                        padding: 6px 14px;
                        border-radius: 20px;
                        font-size: 12px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <span class="badge">AGENT READY</span>
                    <h1>Neon Agent Emulator</h1>
                    <p>Esperando comandos de sincronización en tiempo real...</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        webViewCanvas.loadDataWithBase64(welcomeHtml)
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

        tvStatus.text = "🤖 Agente Escuchando en http://$ipAddress:$port"
    }

    private fun handleAgentCommand(command: String, payload: String) {
        when (command) {
            "load_url" -> webViewCanvas.loadUrl(payload)
            "load_html" -> webViewCanvas.loadDataWithBase64(payload)
            "eval_js" -> webViewCanvas.evaluateJavascript(payload, null)
            "reload" -> webViewCanvas.reload()
        }
    }

    // Permite capturar la pantalla actual del Canvas para enviarla a la IA
    fun captureCanvasBase64(): String {
        val bitmap = Bitmap.createBitmap(webViewCanvas.width, webViewCanvas.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webViewCanvas.draw(canvas)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
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
