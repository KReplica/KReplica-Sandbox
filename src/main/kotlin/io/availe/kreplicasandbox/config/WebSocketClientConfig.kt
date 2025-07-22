package io.availe.kreplicasandbox.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.client.WebSocketConnectionManager
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import io.availe.kreplicasandbox.service.SocketHandler

@Configuration
class WebSocketClientConfig {

    @Bean
    fun webSocketConnectionManager(
        webSocketClient: StandardWebSocketClient,
        socketHandler: SocketHandler
    ): WebSocketConnectionManager {
        val manager = WebSocketConnectionManager(
            webSocketClient,
            socketHandler,
            "ws://localhost:8080/ws/status"
        )
        manager.isAutoStartup = true
        return manager
    }

    @Bean
    fun webSocketClient(): StandardWebSocketClient {
        return StandardWebSocketClient()
    }
}