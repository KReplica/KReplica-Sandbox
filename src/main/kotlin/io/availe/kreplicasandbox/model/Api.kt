package io.availe.kreplicasandbox.model

data class CompileRequest(
    val jobId: String,
    val sourceCode: String
)

data class CompileResponse(
    val jobId: String,
    val success: Boolean,
    val generatedFiles: Map<String, String>? = null,
    val message: String
)