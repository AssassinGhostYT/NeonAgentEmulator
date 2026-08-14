package com.neon.emulator

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AgentServer(
    private val port: Int,
    private val onCommandReceived: (command: String, payload: String) -> Unit
) {
    private var server: NettyApplicationEngine? = null

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            server = embeddedServer(Netty, port = port) {
                routing {
                    get("/") {
                        call.respondText("🤖 Neon Agent Emulator API v1.0 Activo", ContentType.Text.Plain)
                    }

                    get("/status") {
                        call.respondText("""{"status": "online", "agent": "connected"}""", ContentType.Application.Json)
                    }

                    post("/api/command") {
                        val body = call.receiveText()
                        // Ejemplo formato: COMMAND:payload (ej: load_url:https://google.com)
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
