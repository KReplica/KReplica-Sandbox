package io.availe.kreplicasandbox.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.availe.kreplicasandbox.model.CompileRequest
import io.availe.kreplicasandbox.model.CompileResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class SocketHandler(private val objectMapper: ObjectMapper) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        println("Client: Connected to server: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val request = objectMapper.readValue(message.payload, CompileRequest::class.java)
        println("Client: Received compilation job ${request.jobId}")

        // placeholder response.
        val response = CompileResponse(
            jobId = request.jobId,
            success = true,
            generatedFiles = mapOf(
                "Placeholder.kt" to "/* Code processed for job ${request.jobId} */",
                "Status.txt" to "This is a placeholder response."
            ),
            message = "Compilation job received and processed."
        )

        val jsonResponse = objectMapper.writeValueAsString(response)
        session.sendMessage(TextMessage(jsonResponse))
        println("Client: Sent placeholder response for job ${request.jobId}")
    }
}