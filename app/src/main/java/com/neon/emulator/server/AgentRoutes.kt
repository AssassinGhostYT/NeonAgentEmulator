package com.neon.emulator.server

import android.content.Context
import com.google.gson.Gson
import com.neon.emulator.protocol.CapabilityResponse
import com.neon.emulator.runtime.RuntimeDetector
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Routing.configureAgentRoutes(
    context: Context,
    onCommandReceived: (command: String, payload: String) -> Unit
) {
    val gson = Gson()
    val runtimeDetector = RuntimeDetector(context)

    get("/") {
        call.respondText("🤖 Neon Agent Server v1.0 Active", ContentType.Text.Plain)
    }

    get("/status") {
        call.respondText(
            """{"status": "online", "server": "Ktor Netty", "version": "1.0.0"}""",
            ContentType.Application.Json
        )
    }

    // ⚡ ENDPOINT REAL DE DETECCIÓN DE CAPACIDADES /api/capabilities
    get("/api/capabilities") {
        try {
            val deviceInfo = runtimeDetector.getDeviceInfo()
            val storage = runtimeDetector.getStorageMetrics()
            val memory = runtimeDetector.getMemoryMetrics()
            val capabilities = runtimeDetector.detectCapabilities()

            val response = CapabilityResponse(
                success = true,
                android = deviceInfo,
                storage = storage,
                memory = memory,
                capabilities = capabilities
            )

            call.respondText(gson.toJson(response), ContentType.Application.Json)
        } catch (e: Exception) {
            val errorJson = mapOf(
                "success" to false,
                "error" to (e.localizedMessage ?: "Error inesperado al detectar capacidades")
            )
            call.respondText(gson.toJson(errorJson), ContentType.Application.Json, HttpStatusCode.InternalServerError)
        }
    }

    post("/api/command") {
        val body = call.receiveText()
        val parts = body.split(":", limit = 2)
        if (parts.size == 2) {
            onCommandReceived(parts[0], parts[1])
            call.respondText("""{"result": "ok"}""", ContentType.Application.Json)
        } else {
            onCommandReceived("create_project", body)
            call.respondText("""{"result": "created"}""", ContentType.Application.Json)
        }
    }

    post("/api/create_project") {
        val jsonPayload = call.receiveText()
        onCommandReceived("create_project", jsonPayload)
        call.respondText("""{"result": "project_created"}""", ContentType.Application.Json)
    }

    post("/api/render") {
        val htmlContent = call.receiveText()
        onCommandReceived("load_html", htmlContent)
        call.respondText("""{"result": "rendered"}""", ContentType.Application.Json)
    }

    webSocket("/ws/agent") {
        send(Frame.Text("""{"type": "connected", "message": "Canal WebSocket IA Establecido"}"""))
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
}
