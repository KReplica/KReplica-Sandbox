package io.availe.kreplicasandbox.service

import io.availe.kreplicasandbox.model.CompileRequest
import org.gradle.tooling.GradleConnector
import io.availe.kreplicasandbox.model.CompileResponse
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.file.Files

@Service
class CompilerService {

    fun compile(request: CompileRequest): CompileResponse {
        val projectDir = Files.createTempDirectory("kreplica-job-${request.jobId}").toFile()

        try {
            createProjectFiles(projectDir, request.sourceCode)

            val stdoutStream = ByteArrayOutputStream()
            val stderrStream = ByteArrayOutputStream()

            try {
                GradleConnector.newConnector()
                    .forProjectDirectory(projectDir)
                    .useDistribution(URI("https://services.gradle.org/distributions/gradle-8.8-bin.zip"))
                    .connect().use { connection ->
                        connection.newBuild()
                            .forTasks("build")
                            .setStandardOutput(stdoutStream)
                            .setStandardError(stderrStream)
                            .run()
                    }

                val outputDir = File(projectDir, "build/generated-src/kotlin-poet")
                val generatedFiles = if (outputDir.exists()) {
                    outputDir.walk()
                        .filter { it.isFile && it.extension == "kt" }
                        .associate { it.name to it.readText() }
                } else {
                    emptyMap()
                }

                return CompileResponse(
                    jobId = request.jobId,
                    success = true,
                    generatedFiles = generatedFiles,
                    message = "Compilation successful.\n${stdoutStream}"
                )

            } catch (e: Exception) {
                return CompileResponse(
                    jobId = request.jobId,
                    success = false,
                    message = "Compilation failed:\n${stderrStream}"
                )
            }
        } finally {
            projectDir.deleteRecursively()
        }
    }

    private fun createProjectFiles(projectDir: File, sourceCode: String) {
        File(projectDir, "settings.gradle.kts").writeText(
            readResource("/templates/gradle/settings.gradle.kts.template")
        )

        File(projectDir, "build.gradle.kts").writeText(
            readResource("/templates/gradle/build.gradle.kts.template")
        )

        val srcDir = File(projectDir, "src/main/kotlin/io/availe/playground")
        srcDir.mkdirs()
        File(srcDir, "Source.kt").writeText(sourceCode)
    }

    private fun readResource(path: String): String {
        return javaClass.getResource(path)?.readText()
            ?: throw IllegalStateException("Cannot find resource: $path")
    }
}