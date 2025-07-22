package io.availe.kreplicasandbox.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.availe.kreplicasandbox.model.CompileRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class SocketHandler(
    private val objectMapper: ObjectMapper,
    private val jobProcessor: JobProcessorService
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        println("Client: Connected to server: ${session.id}")
        jobProcessor.registerSession(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val request = objectMapper.readValue(message.payload, CompileRequest::class.java)
        println("Client: Received and queued compilation job ${request.jobId}")
        jobProcessor.submitJob(request)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        println("Client: Disconnected from server: ${session.id}")
        jobProcessor.deregisterSession()
    }
}