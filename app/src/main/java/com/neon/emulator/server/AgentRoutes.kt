package com.neon.emulator.server

import android.content.Context
import com.google.gson.Gson
import com.neon.emulator.protocol.AgentRequest
import com.neon.emulator.protocol.AgentResponse
import com.neon.emulator.protocol.AgentTask
import com.neon.emulator.protocol.BuildEvent
import com.neon.emulator.protocol.CapabilityResponse
import com.neon.emulator.runtime.RuntimeDetector
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

object TaskRegistry {
    val tasks = ConcurrentHashMap<String, AgentTask>()
}

fun Routing.configureAgentRoutes(
    context: Context,
    onCommandReceived: (command: String, payload: String) -> Unit
) {
    val gson = Gson()
    val runtimeDetector = RuntimeDetector(context)

    get("/") {
        call.respondText("🤖 Neon Agent Server v2.0 Active (JSON & WebSocket Protocol)", ContentType.Text.Plain)
    }

    get("/status") {
        call.respondText(
            """{"status": "online", "server": "Ktor Netty", "version": "2.0.0", "activeTasks": ${TaskRegistry.tasks.size}}""",
            ContentType.Application.Json
        )
    }

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
                "error" to (e.localizedMessage ?: "Error al detectar capacidades")
            )
            call.respondText(gson.toJson(errorJson), ContentType.Application.Json, HttpStatusCode.InternalServerError)
        }
    }

    // ⚡ FASE 2: PLANIFICACIÓN DE AGENTE POST /api/agent/plan
    post("/api/agent/plan") {
        try {
            val bodyText = call.receiveText()
            val request = gson.fromJson(bodyText, AgentRequest::class.java)
            val taskId = request.id ?: "task-${System.currentTimeMillis()}"
            val projectId = request.projectId ?: "default-project"

            val task = AgentTask(
                id = taskId,
                action = request.action,
                projectId = projectId,
                status = "accepted",
                stage = "planning",
                message = "Plan recibido y aceptado",
                progress = 10
            )

            TaskRegistry.tasks[taskId] = task
            WebSocketManager.broadcastEvent(BuildEvent(taskId, "progress", "planning", "Planificando cambios", 10))

            val response = AgentResponse(
                id = taskId,
                success = true,
                status = "accepted",
                stage = "planning",
                message = "Plan registrado correctamente",
                result = task
            )

            call.respondText(gson.toJson(response), ContentType.Application.Json)
        } catch (e: Exception) {
            call.respondText(
                gson.toJson(AgentResponse("unknown", false, "failed", "planning", "Error al procesar plan", error = e.localizedMessage)),
                ContentType.Application.Json, HttpStatusCode.BadRequest
            )
        }
    }

    // ⚡ FASE 2: EJECUCIÓN DE TAREA POST /api/agent/execute
    post("/api/agent/execute") {
        try {
            val bodyText = call.receiveText()
            val request = gson.fromJson(bodyText, AgentRequest::class.java)
            val taskId = request.id ?: "task-${System.currentTimeMillis()}"
            val projectId = request.projectId ?: "default-project"

            val task = AgentTask(
                id = taskId,
                action = request.action,
                projectId = projectId,
                status = "creating_files",
                stage = "creating_files",
                message = "Ejecutando acción de proyecto",
                progress = 25
            )

            TaskRegistry.tasks[taskId] = task

            // Dispatch a listener o comando existente
            if (request.payload != null) {
                onCommandReceived(request.action, request.payload)
            } else {
                onCommandReceived(request.action, bodyText)
            }

            WebSocketManager.broadcastEvent(BuildEvent(taskId, "progress", "creating_files", "Archivos en proceso de creación", 25))

            val response = AgentResponse(
                id = taskId,
                success = true,
                status = "creating_files",
                stage = "creating_files",
                message = "Ejecución iniciada exitosamente",
                result = task
            )

            call.respondText(gson.toJson(response), ContentType.Application.Json)
        } catch (e: Exception) {
            call.respondText(
                gson.toJson(AgentResponse("unknown", false, "failed", "creating_files", "Error al ejecutar agente", error = e.localizedMessage)),
                ContentType.Application.Json, HttpStatusCode.BadRequest
            )
        }
    }

    // ⚡ FASE 2: CONSULTAR ESTADO DE TAREA GET /api/tasks/{id}
    get("/api/tasks/{id}") {
        val taskId = call.parameters["id"]
        val task = TaskRegistry.tasks[taskId]

        if (task != null) {
            call.respondText(gson.toJson(task), ContentType.Application.Json)
        } else {
            call.respondText(
                gson.toJson(mapOf("success" to false, "message" to "Tarea no encontrada", "id" to taskId)),
                ContentType.Application.Json, HttpStatusCode.NotFound
            )
        }
    }

    // ⚡ FASE 2: CANCELAR TAREA POST /api/tasks/{id}/cancel
    post("/api/tasks/{id}/cancel") {
        val taskId = call.parameters["id"]
        val task = TaskRegistry.tasks[taskId]

        if (task != null) {
            task.status = "cancelled"
            task.stage = "cancelled"
            task.message = "Tarea cancelada por el usuario"
            task.updatedAt = System.currentTimeMillis()

            WebSocketManager.broadcastEvent(BuildEvent(taskId!!, "cancelled", "cancelled", "Tarea cancelada", task.progress))

            call.respondText(gson.toJson(AgentResponse(taskId, true, "cancelled", "cancelled", "Tarea cancelada con éxito")), ContentType.Application.Json)
        } else {
            call.respondText(
                gson.toJson(mapOf("success" to false, "message" to "Tarea no encontrada para cancelar")),
                ContentType.Application.Json, HttpStatusCode.NotFound
            )
        }
    }

    // Compatibilidad temporal con protocolo comando:payload y renderizado
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

    // ⚡ FASE 2: CANALES WEBSOCKET ESTRUCTURADOS
    webSocket("/ws/agent") {
        WebSocketManager.registerAgentSession(this)
        try {
            send(Frame.Text(gson.toJson(BuildEvent("system", "progress", "connected", "Canal WebSocket Agent listo"))))
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (text.startsWith("{")) {
                        try {
                            val req = gson.fromJson(text, AgentRequest::class.java)
                            onCommandReceived(req.action, text)
                        } catch (_: Exception) {}
                    } else {
                        val parts = text.split(":", limit = 2)
                        if (parts.size == 2) {
                            onCommandReceived(parts[0], parts[1])
                        }
                    }
                }
            }
        } finally {
            WebSocketManager.unregisterAgentSession(this)
        }
    }

    webSocket("/ws/tasks/{id}") {
        val taskId = call.parameters["id"] ?: "unknown"
        WebSocketManager.registerTaskSession(taskId, this)
        try {
            send(Frame.Text(gson.toJson(BuildEvent(taskId, "progress", "listening", "Escuchando eventos de la tarea $taskId"))))
            for (frame in incoming) {
                // Escuchar posibles señales de entrada de la tarea
            }
        } finally {
            WebSocketManager.unregisterTaskSession(taskId, this)
        }
    }

    webSocket("/ws/logs") {
        WebSocketManager.registerLogSession(this)
        try {
            send(Frame.Text("""[LOG] Conectado al canal de logs del sistema"""))
            for (frame in incoming) {}
        } finally {
            WebSocketManager.unregisterLogSession(this)
        }
    }
}
