package com.neon.emulator

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

class AgentServer(
    private val port: Int,
    private val onCommandReceived: (command: String, payload: String) -> Unit
) {
    private var server: NettyApplicationEngine? = null

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            server = embeddedServer(Netty, port = port) {
                // Habilitar WebSockets (Sockets bidireccionales en tiempo real)
                install(WebSockets) {
                    pingPeriod = Duration.ofSeconds(15)
                    timeout = Duration.ofSeconds(15)
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }

                routing {
                    get("/") {
                        call.respondText("🤖 Neon Agent MCP & WebSockets Bridge v1.0", ContentType.Text.Plain)
                    }

                    get("/status") {
                        call.respondText("""{"status": "online", "mcp_protocol": "v1.0", "websockets": true}""", ContentType.Application.Json)
                    }

                    // 🔌 Protocolo MCP (Model Context Protocol) Endpoint para IAs
                    post("/mcp/v1/tools/call") {
                        val requestJson = call.receiveText()
                        // Procesa tool calls de MCP enviadas por la IA
                        onCommandReceived("mcp_tool", requestJson)
                        call.respondText("""{"result": "mcp_tool_executed", "status": "success"}""", ContentType.Application.Json)
                    }

                    // ⚡ Real-Time WebSocket Channel (Para conexión fluida e instantánea IA <-> App)
                    webSocket("/ws/agent") {
                        send(Frame.Text("""{"type": "connected", "message": "Canal WebSocket IA - App Móvil Establecido"}"""))
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val parts = text.split(":", limit = 2)
                                if (parts.size == 2) {
                                    onCommandReceived(parts[0], parts[1])
                                    send(Frame.Text("""{"type": "ack", "command": "${parts[0]}"}"""))
                                }
                            }
                        }
                    }

                    post("/api/command") {
                        val body = call.receiveText()
                        val parts = body.split(":", limit = 2)
                        if (parts.size == 2) {
                            onCommandReceived(parts[0], parts[1])
                            call.respondText("""{"result": "ok"}""", ContentType.Application.Json)
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "Formato invalido")
                        }
                    }

                    post("/api/render") {
                        val htmlContent = call.receiveText()
                        onCommandReceived("load_html", htmlContent)
                        call.respondText("""{"result": "rendered"}""", ContentType.Application.Json)
                    }
                }
            }
            server?.start(wait = false)
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
    }
}
