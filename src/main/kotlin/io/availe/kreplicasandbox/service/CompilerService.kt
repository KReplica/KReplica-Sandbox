package io.availe.kreplicasandbox.service

import io.availe.kreplicasandbox.model.CompileRequest
import io.availe.kreplicasandbox.model.CompileResponse
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import kotlin.io.path.createTempDirectory

@Service
class CompilerService {
    private val classpath by lazy {
        (Thread.currentThread().contextClassLoader as URLClassLoader).urLs
            .joinToString(File.pathSeparator) { File(it.toURI()).absolutePath }
    }

    fun compile(request: CompileRequest): CompileResponse {
        val tempDir = createTempDirectory("kreplica-job-${request.jobId}").toFile()
        return try {
            val sourceFile = File(tempDir, "Source.kt").apply {
                parentFile?.mkdirs()
                writeText(request.sourceCode)
            }
            val kspGenDir = File(tempDir, "generated/ksp/main/kotlin")
            val buildDir = File(tempDir, "build")
            val args = K2JVMCompilerArguments().apply {
                pluginOptions = arrayOf(
                    "plugin:com.google.devtools.ksp:sources=${sourceFile.absolutePath}",
                    "plugin:com.google.devtools.ksp:output=${kspGenDir.absolutePath}",
                    "plugin:com.google.devtools.ksp:projectBaseDir=${tempDir.absolutePath}"
                )
                freeArgs = listOf(sourceFile.absolutePath)
                destination = buildDir.absolutePath
                classpath = this@CompilerService.classpath
                noStdlib = true
                noReflect = true
                apiVersion = "2.0"
                languageVersion = "2.0"
            }
            val outputStream = ByteArrayOutputStream()
            val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, false)
            val compiler = K2JVMCompiler()
            val result = compiler.exec(messageCollector, org.jetbrains.kotlin.config.Services.EMPTY, args)
            if (result == ExitCode.OK) {
                val generatedFiles = kspGenDir.walk()
                    .filter { it.isFile && it.extension == "kt" }
                    .associate { it.name to it.readText() }
                CompileResponse(
                    jobId = request.jobId,
                    success = true,
                    generatedFiles = generatedFiles,
                    message = "Compilation successful."
                )
            } else {
                CompileResponse(
                    jobId = request.jobId,
                    success = false,
                    message = "Compilation failed:\n${outputStream}"
                )
            }
        } catch (e: Exception) {
            CompileResponse(
                jobId = request.jobId,
                success = false,
                message = "An unexpected error occurred during compilation: ${e.message}"
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
