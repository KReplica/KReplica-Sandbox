package io.availe.kreplicasandbox.service

import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class SocketHandler : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        println("Sandbox client connected to server: ${session.id}")
        session.sendMessage(TextMessage("Hello from the KReplica Sandbox!"))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        println("Message received from docs server: ${message.payload}")
    }
}