package com.aidepro.ai

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * AiAssistant: The System AI engine for AIDE Pro.
 *
 * Capabilities:
 * - Code generation (generate classes, functions, layouts from natural language)
 * - Code explanation (explain selected code)
 * - Error fixing (analyze build errors and suggest fixes)
 * - Code completion (context-aware suggestions)
 * - Refactoring suggestions
 * - Documentation generation
 *
 * Supports multiple AI providers: OpenAI, Gemini, Ollama (local), and custom endpoints.
 */
class AiAssistant(private val config: AiConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        engine {
            connectTimeout = 30_000
            socketTimeout = 60_000
        }
    }

    // System prompt that gives the AI its IDE assistant persona
    private val systemPrompt = """
        You are AIDE Pro's built-in AI assistant — an expert Android developer with deep knowledge of:
        - Kotlin and Java for Android development
        - Jetpack Compose and XML layouts
        - Android SDK APIs (minSdk 21+)
        - Gradle build system (Kotlin DSL and Groovy)
        - Android architecture patterns (MVVM, Clean Architecture)
        - Material Design 3 guidelines
        - Common Android libraries (Hilt, Room, Retrofit, Coroutines, Flow)
        
        Your role is to help the developer write, fix, and understand Android code.
        Always provide concise, practical, and correct code examples.
        When fixing errors, explain the root cause clearly.
        Format code blocks with appropriate language tags.
        Keep responses focused and avoid unnecessary verbosity.
    """.trimIndent()

    /**
     * Streams a chat response from the AI.
     * Returns a Flow of text chunks for real-time display.
     */
    fun chat(
        messages: List<AiMessage>,
        context: AiContext? = null
    ): Flow<AiStreamChunk> = flow {
        val fullMessages = buildMessageList(messages, context)

        try {
            val response = httpClient.post(config.baseUrl + "/chat/completions") {
                header("Authorization", "Bearer ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        model = config.model,
                        messages = fullMessages,
                        stream = true,
                        temperature = 0.7f,
                        max_tokens = 4096
                    )
                )
            }

            // Parse SSE stream
            response.bodyAsChannel().let { channel ->
                val buffer = StringBuilder()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") {
                            emit(AiStreamChunk.Done)
                            break
                        }
                        try {
                            val chunk = json.decodeFromString<StreamChunkResponse>(data)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (content != null) {
                                emit(AiStreamChunk.Text(content))
                            }
                        } catch (e: Exception) {
                            // Skip malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "AI request failed")
            emit(AiStreamChunk.Error("AI request failed: ${e.message}"))
        }
    }

    /**
     * Analyzes build errors and returns fix suggestions.
     */
    suspend fun analyzeBuildError(
        errorLog: String,
        relevantCode: String? = null
    ): String {
        val prompt = buildString {
            append("Analyze this Android build error and provide a clear fix:\n\n")
            append("**Build Error:**\n```\n$errorLog\n```\n\n")
            if (relevantCode != null) {
                append("**Relevant Code:**\n```kotlin\n$relevantCode\n```\n\n")
            }
            append("Provide: 1) Root cause, 2) Step-by-step fix, 3) Fixed code if applicable.")
        }

        return getCompletion(prompt)
    }

    /**
     * Generates code based on a natural language description.
     */
    suspend fun generateCode(
        description: String,
        language: String = "kotlin",
        context: AiContext? = null
    ): String {
        val prompt = buildString {
            append("Generate Android $language code for: $description\n\n")
            context?.let {
                append("Current file: ${it.currentFileName}\n")
                append("Package: ${it.packageName}\n")
                if (it.currentFileContent.isNotBlank()) {
                    append("Existing code context:\n```$language\n${it.currentFileContent.take(2000)}\n```\n")
                }
            }
            append("\nProvide complete, production-ready code with proper imports.")
        }

        return getCompletion(prompt)
    }

    /**
     * Explains selected code.
     */
    suspend fun explainCode(code: String, language: String = "kotlin"): String {
        val prompt = "Explain this Android $language code clearly and concisely:\n\n```$language\n$code\n```"
        return getCompletion(prompt)
    }

    /**
     * Suggests refactoring improvements.
     */
    suspend fun suggestRefactoring(code: String, language: String = "kotlin"): String {
        val prompt = "Suggest refactoring improvements for this Android $language code. Focus on readability, performance, and best practices:\n\n```$language\n$code\n```"
        return getCompletion(prompt)
    }

    /**
     * Generates documentation (KDoc/Javadoc) for a function or class.
     */
    suspend fun generateDocumentation(code: String, language: String = "kotlin"): String {
        val prompt = "Generate proper ${if (language == "kotlin") "KDoc" else "Javadoc"} documentation for:\n\n```$language\n$code\n```\nReturn only the documented version."
        return getCompletion(prompt)
    }

    private suspend fun getCompletion(prompt: String): String {
        return try {
            val response = httpClient.post(config.baseUrl + "/chat/completions") {
                header("Authorization", "Bearer ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        model = config.model,
                        messages = listOf(
                            AiMessageDto("system", systemPrompt),
                            AiMessageDto("user", prompt)
                        ),
                        stream = false,
                        temperature = 0.7f,
                        max_tokens = 4096
                    )
                )
            }
            val result = response.body<ChatResponse>()
            result.choices.firstOrNull()?.message?.content ?: "No response from AI."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun buildMessageList(
        messages: List<AiMessage>,
        context: AiContext?
    ): List<AiMessageDto> {
        val result = mutableListOf<AiMessageDto>()

        // System prompt
        var systemContent = systemPrompt
        context?.let {
            systemContent += "\n\nCurrent context:\n" +
                    "- File: ${it.currentFileName}\n" +
                    "- Package: ${it.packageName}\n" +
                    "- Language: ${it.language}"
            if (it.currentFileContent.isNotBlank()) {
                systemContent += "\n- Current file content (first 1500 chars):\n```\n${it.currentFileContent.take(1500)}\n```"
            }
        }
        result.add(AiMessageDto("system", systemContent))

        // Chat history
        messages.forEach { msg ->
            result.add(AiMessageDto(
                role = if (msg.isUser) "user" else "assistant",
                content = msg.content
            ))
        }

        return result
    }

    fun close() {
        httpClient.close()
    }
}

// ─── Data Models ─────────────────────────────────────────────────────────────

data class AiConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini"
)

data class AiMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiContext(
    val currentFileName: String = "",
    val currentFileContent: String = "",
    val packageName: String = "",
    val language: String = "kotlin",
    val cursorLine: Int = 0,
    val selectedText: String = ""
)

sealed class AiStreamChunk {
    data class Text(val content: String) : AiStreamChunk()
    object Done : AiStreamChunk()
    data class Error(val message: String) : AiStreamChunk()
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<AiMessageDto>,
    val stream: Boolean = false,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 4096
)

@Serializable
data class AiMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: AiMessageDto,
    val finish_reason: String? = null
)

@Serializable
data class StreamChunkResponse(
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val delta: DeltaContent
)

@Serializable
data class DeltaContent(
    val content: String? = null
)
