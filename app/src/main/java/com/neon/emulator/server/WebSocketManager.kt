package com.neon.emulator.server

import com.google.gson.Gson
import com.neon.emulator.protocol.BuildEvent
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

object WebSocketManager {
    private val gson = Gson()
    private val agentSessions = ConcurrentHashMap.newKeySet<DefaultWebSocketSession>()
    private val taskSessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    private val logSessions = ConcurrentHashMap.newKeySet<DefaultWebSocketSession>()

    fun registerAgentSession(session: DefaultWebSocketSession) {
        agentSessions.add(session)
    }

    fun unregisterAgentSession(session: DefaultWebSocketSession) {
        agentSessions.remove(session)
    }

    fun registerTaskSession(taskId: String, session: DefaultWebSocketSession) {
        taskSessions.computeIfAbsent(taskId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unregisterTaskSession(taskId: String, session: DefaultWebSocketSession) {
        taskSessions[taskId]?.remove(session)
    }

    fun registerLogSession(session: DefaultWebSocketSession) {
        logSessions.add(session)
    }

    fun unregisterLogSession(session: DefaultWebSocketSession) {
        logSessions.remove(session)
    }

    suspend fun broadcastEvent(event: BuildEvent) {
        val json = gson.toJson(event)
        val frame = Frame.Text(json)
        
        agentSessions.forEach { session ->
            try { session.send(frame) } catch (_: Exception) {}
        }
        
        taskSessions[event.id]?.forEach { session ->
            try { session.send(frame) } catch (_: Exception) {}
        }
    }

    suspend fun broadcastLog(message: String) {
        val frame = Frame.Text(message)
        logSessions.forEach { session ->
            try { session.send(frame) } catch (_: Exception) {}
        }
    }
}
