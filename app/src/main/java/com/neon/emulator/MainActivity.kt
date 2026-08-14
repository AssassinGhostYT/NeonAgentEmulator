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
        settings.allowUniversalAccessFromFileURLs = true

        webViewCanvas.webViewClient = WebViewClient()
        webViewCanvas.webChromeClient = WebChromeClient()

        // HTML Universal Launcher: Soporta JS, HTML5, CSS, React, Vue, Python Pyodide, WASM, Lua, PHP WebAssembly
        val welcomeHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Neon Agent Universal Engine</title>
                <style>
                    body {
                        background: #0F172A;
                        color: #F8FAFC;
                        font-family: system-ui, -apple-system, sans-serif;
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
                        max-width: 85%;
                    }
                    h1 { color: #38BDF8; margin-bottom: 8px; font-size: 20px; }
                    p { color: #94A3B8; font-size: 13px; margin-bottom: 16px; }
                    .tags {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 6px;
                        justify-content: center;
                    }
                    .tag {
                        background: #0EA5E9;
                        color: white;
                        padding: 4px 10px;
                        border-radius: 12px;
                        font-size: 11px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>🚀 Neon Agent Universal Engine</h1>
                    <p>Motor Multi-Lenguaje Listo (JS, HTML5, Python/Pyodide, PHP/WASM, Lua, React)</p>
                    <div class="tags">
                        <span class="tag">JavaScript</span>
                        <span class="tag">Python</span>
                        <span class="tag">PHP</span>
                        <span class="tag">WASM</span>
                        <span class="tag">Lua</span>
                        <span class="tag">HTML/CSS</span>
                    </div>
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

        tvStatus.text = "🤖 Servidor Multi-Lenguaje: http://$ipAddress:$port"
    }

    private fun handleAgentCommand(command: String, payload: String) {
        when (command) {
            "load_url" -> webViewCanvas.loadUrl(payload)
            "load_html" -> webViewCanvas.loadDataWithBase64(payload)
            "eval_js" -> webViewCanvas.evaluateJavascript(payload, null)
            "reload" -> webViewCanvas.reload()
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
