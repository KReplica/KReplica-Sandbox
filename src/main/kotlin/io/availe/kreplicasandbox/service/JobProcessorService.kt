package io.availe.kreplicasandbox.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.availe.kreplicasandbox.model.CompileRequest
import io.availe.kreplicasandbox.model.CompileResponse
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

@Service
class JobProcessorService(
    private val compilerService: CompilerService,
    private val objectMapper: ObjectMapper
) {
    private val jobQueue = LinkedBlockingQueue<CompileRequest>()
    private val session = AtomicReference<WebSocketSession>(null)
    private val executor = Executors.newSingleThreadExecutor()

    fun registerSession(session: WebSocketSession) {
        this.session.set(session)
    }

    fun deregisterSession() {
        this.session.set(null)
        jobQueue.clear()
    }

    fun submitJob(request: CompileRequest) {
        jobQueue.offer(request)
    }

    @PostConstruct
    fun startProcessing() {
        executor.submit {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val request = jobQueue.take()
                    val activeSession = session.get()
                    if (activeSession == null || !activeSession.isOpen) {
                        println("No active session, discarding job ${request.jobId}")
                        continue
                    }

                    println("Processing job: ${request.jobId}")
                    val response = compilerService.compile(request)
                    val jsonResponse = objectMapper.writeValueAsString(response)

                    activeSession.sendMessage(TextMessage(jsonResponse))
                    println("Sent response for job: ${request.jobId}")

                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    println("Job processor shutting down.")
                } catch (e: Exception) {
                    println("Error processing job: ${e.message}")
                }
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }
}