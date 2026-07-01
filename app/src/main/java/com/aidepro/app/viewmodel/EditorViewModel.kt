package com.aidepro.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aidepro.ai.AiAssistant
import com.aidepro.ai.AiConfig
import com.aidepro.ai.AiContext
import com.aidepro.ai.AiMessage
import com.aidepro.ai.AiStreamChunk
import com.aidepro.buildsystem.BuildConfig
import com.aidepro.buildsystem.BuildEngine
import com.aidepro.buildsystem.BuildEvent
import com.aidepro.buildsystem.ProjectLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    // ─── UI State ────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _buildState = MutableStateFlow<BuildState>(BuildState.Idle)
    val buildState: StateFlow<BuildState> = _buildState.asStateFlow()

    private val _openFiles = MutableStateFlow<List<String>>(emptyList())
    val openFiles: StateFlow<List<String>> = _openFiles.asStateFlow()

    private val _activeFile = MutableStateFlow<String?>(null)
    val activeFile: StateFlow<String?> = _activeFile.asStateFlow()

    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    private val _buildOutput = MutableStateFlow<List<TerminalLine>>(emptyList())
    val buildOutput: StateFlow<List<TerminalLine>> = _buildOutput.asStateFlow()

    // ─── Panel Visibility ────────────────────────────────────────────────────

    private val _isAiPanelOpen = MutableStateFlow(false)
    val isAiPanelOpen: StateFlow<Boolean> = _isAiPanelOpen.asStateFlow()

    private val _isFileExplorerOpen = MutableStateFlow(true)
    val isFileExplorerOpen: StateFlow<Boolean> = _isFileExplorerOpen.asStateFlow()

    private val _isTerminalOpen = MutableStateFlow(false)
    val isTerminalOpen: StateFlow<Boolean> = _isTerminalOpen.asStateFlow()

    private val _terminalActiveTab = MutableStateFlow(0)
    val terminalActiveTab: StateFlow<Int> = _terminalActiveTab.asStateFlow()

    // ─── AI State ────────────────────────────────────────────────────────────

    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // ─── File Content Cache ──────────────────────────────────────────────────

    private val fileContentCache = mutableMapOf<String, MutableStateFlow<String>>()
    private val unsavedChanges = mutableSetOf<String>()

    // ─── Build Engine & AI ───────────────────────────────────────────────────

    private val buildEngine = BuildEngine(application)
    private var aiAssistant: AiAssistant? = null
    private var currentProjectPath: String = ""

    init {
        viewModelScope.launch {
            val apiKey = settingsRepository.getAiApiKey()
            val baseUrl = settingsRepository.getAiBaseUrl()
            val model = settingsRepository.getAiModel()
            if (apiKey.isNotBlank()) {
                aiAssistant = AiAssistant(AiConfig(apiKey, baseUrl, model))
            }
        }
    }

    // ─── Project Loading ─────────────────────────────────────────────────────

    fun loadProject(projectPath: String) {
        currentProjectPath = projectPath
        val projectDir = File(projectPath)
        _uiState.update { it.copy(projectName = projectDir.name) }
        refreshFileTree()
        addBuildLog("Project loaded: ${projectDir.name}", TerminalLineType.HEADER)
    }

    fun refreshFileTree() {
        viewModelScope.launch(Dispatchers.IO) {
            val tree = buildFileTree(File(currentProjectPath))
            _fileTree.value = tree
        }
    }

    private fun buildFileTree(dir: File, depth: Int = 0): List<FileNode> {
        if (depth > 10) return emptyList()
        return dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.filter { !it.name.startsWith(".") && it.name != "build" }
            ?.map { file ->
                FileNode(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    isExpanded = depth == 0 && file.isDirectory,
                    children = if (file.isDirectory && depth == 0)
                        buildFileTree(file, depth + 1) else emptyList()
                )
            } ?: emptyList()
    }

    fun toggleExpand(path: String) {
        _fileTree.update { nodes ->
            toggleNodeExpand(nodes, path)
        }
    }

    private fun toggleNodeExpand(nodes: List<FileNode>, path: String): List<FileNode> {
        return nodes.map { node ->
            when {
                node.path == path -> {
                    val expanded = !node.isExpanded
                    node.copy(
                        isExpanded = expanded,
                        children = if (expanded && node.children.isEmpty())
                            buildFileTree(File(path), 1) else node.children
                    )
                }
                node.isDirectory -> node.copy(
                    children = toggleNodeExpand(node.children, path)
                )
                else -> node
            }
        }
    }

    // ─── File Operations ─────────────────────────────────────────────────────

    fun openFile(path: String) {
        if (!_openFiles.value.contains(path)) {
            _openFiles.update { it + path }
        }
        _activeFile.value = path

        // Load file content if not cached
        if (!fileContentCache.containsKey(path)) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val content = File(path).readText()
                    getOrCreateFileFlow(path).value = content
                } catch (e: Exception) {
                    Timber.e(e, "Failed to read file: $path")
                }
            }
        }
    }

    fun switchToFile(path: String) {
        _activeFile.value = path
    }

    fun closeFile(path: String) {
        // Save if there are unsaved changes
        if (unsavedChanges.contains(path)) {
            saveFile(path)
        }
        _openFiles.update { it - path }
        if (_activeFile.value == path) {
            _activeFile.value = _openFiles.value.lastOrNull()
        }
        fileContentCache.remove(path)
    }

    fun getFileContent(path: String): StateFlow<String> {
        return getOrCreateFileFlow(path).asStateFlow()
    }

    fun onFileContentChanged(path: String, content: String) {
        getOrCreateFileFlow(path).value = content
        unsavedChanges.add(path)
        // Auto-save after a short delay (debounce handled externally)
    }

    fun saveFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = fileContentCache[path]?.value ?: return@launch
                File(path).writeText(content)
                unsavedChanges.remove(path)
                addBuildLog("Saved: ${File(path).name}", TerminalLineType.SUCCESS)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save file: $path")
            }
        }
    }

    fun saveAllFiles() {
        unsavedChanges.toList().forEach { saveFile(it) }
    }

    private fun getOrCreateFileFlow(path: String): MutableStateFlow<String> {
        return fileContentCache.getOrPut(path) { MutableStateFlow("") }
    }

    fun updateCursorPosition(line: Int, column: Int) {
        _uiState.update { it.copy(cursorLine = line, cursorColumn = column) }
    }

    // ─── Build System ────────────────────────────────────────────────────────

    fun buildProject() {
        viewModelScope.launch {
            saveAllFiles()
            _isTerminalOpen.value = true
            _terminalActiveTab.value = 0
            _buildState.value = BuildState.Building
            clearTerminal()

            val config = createBuildConfig()
            buildEngine.buildProject(config).collect { event ->
                when (event) {
                    is BuildEvent.Started -> addBuildLog(event.message, TerminalLineType.HEADER)
                    is BuildEvent.Progress -> addBuildLog(
                        "[${event.percent}%] ${event.message}", TerminalLineType.INFO
                    )
                    is BuildEvent.Log -> addBuildLog(event.message, TerminalLineType.INFO)
                    is BuildEvent.Success -> {
                        _buildState.value = BuildState.Success(event.apkPath)
                        addBuildLog("BUILD SUCCESSFUL", TerminalLineType.SUCCESS)
                        addBuildLog("APK: ${event.apkPath}", TerminalLineType.SUCCESS)
                    }
                    is BuildEvent.Failed -> {
                        _buildState.value = BuildState.Error(event.error)
                        addBuildLog("BUILD FAILED", TerminalLineType.ERROR)
                        addBuildLog(event.error, TerminalLineType.ERROR)
                        // Auto-trigger AI error analysis
                        if (aiAssistant != null) {
                            aiAnalyzeBuildError(event.error)
                        }
                    }
                }
            }
        }
    }

    fun runProject() {
        val state = _buildState.value
        if (state is BuildState.Success) {
            // Install and run the APK
            viewModelScope.launch {
                installApk(state.apkPath)
            }
        } else {
            // Build first, then run
            buildProject()
        }
    }

    private suspend fun installApk(apkPath: String) {
        withContext(Dispatchers.IO) {
            addBuildLog("Installing APK...", TerminalLineType.INFO)
            // Use PackageManager or shell to install
            // This requires REQUEST_INSTALL_PACKAGES permission
            addBuildLog("APK ready at: $apkPath", TerminalLineType.SUCCESS)
        }
    }

    private fun createBuildConfig(): BuildConfig {
        val projectDir = File(currentProjectPath)
        val buildDir = File(getApplication<android.app.Application>().cacheDir, "build/${projectDir.name}")
        return BuildConfig(
            projectName = projectDir.name,
            projectDir = currentProjectPath,
            buildDir = buildDir.absolutePath,
            packageName = detectPackageName(),
            language = detectProjectLanguage(),
            minSdk = 21,
            targetSdk = 35
        )
    }

    private fun detectPackageName(): String {
        val manifest = File(currentProjectPath, "app/src/main/AndroidManifest.xml")
        return try {
            val content = manifest.readText()
            Regex("package=\"([^\"]+)\"").find(content)?.groupValues?.get(1) ?: "com.example.app"
        } catch (e: Exception) {
            "com.example.app"
        }
    }

    private fun detectProjectLanguage(): ProjectLanguage {
        val srcDir = File(currentProjectPath, "app/src/main/java")
        val hasKotlin = srcDir.walkTopDown().any { it.extension == "kt" }
        val hasJava = srcDir.walkTopDown().any { it.extension == "java" }
        return when {
            hasKotlin && hasJava -> ProjectLanguage.MIXED
            hasKotlin -> ProjectLanguage.KOTLIN
            else -> ProjectLanguage.JAVA
        }
    }

    // ─── Terminal ────────────────────────────────────────────────────────────

    fun toggleTerminal() {
        _isTerminalOpen.update { !it }
    }

    fun setTerminalTab(tab: Int) {
        _terminalActiveTab.value = tab
    }

    fun clearTerminal() {
        _buildOutput.value = emptyList()
    }

    fun executeCommand(command: String) {
        addBuildLog(command, TerminalLineType.COMMAND)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = ProcessBuilder("sh", "-c", command)
                    .directory(File(currentProjectPath))
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                if (output.isNotBlank()) {
                    addBuildLog(output, if (exitCode == 0) TerminalLineType.INFO else TerminalLineType.ERROR)
                }
            } catch (e: Exception) {
                addBuildLog("Error: ${e.message}", TerminalLineType.ERROR)
            }
        }
    }

    private fun addBuildLog(message: String, type: TerminalLineType) {
        _buildOutput.update { it + TerminalLine(message, type) }
    }

    // ─── AI Panel ────────────────────────────────────────────────────────────

    fun toggleAiPanel() {
        _isAiPanelOpen.update { !it }
    }

    fun sendAiMessage(message: String) {
        val userMessage = AiChatMessage(content = message, isUser = true)
        _aiChatMessages.update { it + userMessage }
        _isAiThinking.value = true

        viewModelScope.launch {
            val assistant = aiAssistant ?: run {
                addAiMessage("AI not configured. Please add your API key in Settings.")
                _isAiThinking.value = false
                return@launch
            }

            val context = buildAiContext()
            val history = _aiChatMessages.value.map {
                AiMessage(content = it.content, isUser = it.isUser)
            }

            // Add placeholder for streaming response
            val placeholderId = System.currentTimeMillis()
            _aiChatMessages.update {
                it + AiChatMessage(content = "", isUser = false, id = placeholderId)
            }

            val responseBuilder = StringBuilder()
            assistant.chat(history, context).collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.Text -> {
                        responseBuilder.append(chunk.content)
                        // Update the placeholder message with streamed content
                        _aiChatMessages.update { messages ->
                            messages.map { msg ->
                                if (msg.id == placeholderId)
                                    msg.copy(content = responseBuilder.toString())
                                else msg
                            }
                        }
                    }
                    is AiStreamChunk.Done -> {
                        _isAiThinking.value = false
                    }
                    is AiStreamChunk.Error -> {
                        _aiChatMessages.update { messages ->
                            messages.map { msg ->
                                if (msg.id == placeholderId)
                                    msg.copy(content = "Error: ${chunk.message}")
                                else msg
                            }
                        }
                        _isAiThinking.value = false
                    }
                }
            }
        }
    }

    fun aiFixCurrentError() {
        val errorLog = _buildOutput.value
            .filter { it.type == TerminalLineType.ERROR }
            .joinToString("\n") { it.text }
        if (errorLog.isNotBlank()) {
            sendAiMessage("Fix this build error:\n```\n$errorLog\n```")
        } else {
            sendAiMessage("There are no recent build errors. How can I help?")
        }
        _isAiPanelOpen.value = true
    }

    fun aiExplainSelectedCode() {
        val file = _activeFile.value ?: return
        val content = fileContentCache[file]?.value ?: return
        sendAiMessage("Explain this code:\n```kotlin\n${content.take(2000)}\n```")
        _isAiPanelOpen.value = true
    }

    fun aiRefactorSelectedCode() {
        val file = _activeFile.value ?: return
        val content = fileContentCache[file]?.value ?: return
        sendAiMessage("Suggest refactoring improvements for:\n```kotlin\n${content.take(2000)}\n```")
        _isAiPanelOpen.value = true
    }

    fun aiGenerateDocs() {
        val file = _activeFile.value ?: return
        val content = fileContentCache[file]?.value ?: return
        sendAiMessage("Generate KDoc documentation for:\n```kotlin\n${content.take(2000)}\n```")
        _isAiPanelOpen.value = true
    }

    private fun aiAnalyzeBuildError(error: String) {
        _isAiPanelOpen.value = true
        sendAiMessage("Analyze and fix this build error:\n```\n$error\n```")
    }

    fun clearAiChat() {
        _aiChatMessages.value = emptyList()
    }

    private fun addAiMessage(content: String) {
        _aiChatMessages.update {
            it + AiChatMessage(content = content, isUser = false)
        }
    }

    private fun buildAiContext(): AiContext {
        val file = _activeFile.value
        val content = file?.let { fileContentCache[it]?.value } ?: ""
        return AiContext(
            currentFileName = file?.substringAfterLast("/") ?: "",
            currentFileContent = content,
            packageName = detectPackageName(),
            language = when {
                file?.endsWith(".kt") == true -> "kotlin"
                file?.endsWith(".java") == true -> "java"
                file?.endsWith(".xml") == true -> "xml"
                else -> "kotlin"
            }
        )
    }

    // ─── Misc ────────────────────────────────────────────────────────────────

    fun showNewFileDialog() {
        _uiState.update { it.copy(showNewFileDialog = true) }
    }

    override fun onCleared() {
        super.onCleared()
        aiAssistant?.close()
    }
}

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class EditorUiState(
    val projectName: String = "Project",
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1,
    val showNewFileDialog: Boolean = false
)

sealed class BuildState {
    object Idle : BuildState()
    object Building : BuildState()
    data class Success(val apkPath: String) : BuildState()
    data class Error(val message: String) : BuildState()
}

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isExpanded: Boolean = false,
    val children: List<FileNode> = emptyList()
)

data class TerminalLine(
    val text: String,
    val type: TerminalLineType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TerminalLineType { INFO, SUCCESS, ERROR, WARNING, COMMAND, HEADER }

data class AiChatMessage(
    val content: String,
    val isUser: Boolean,
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)

data class ProjectInfo(
    val name: String,
    val path: String,
    val type: String = "kotlin",
    val lastOpened: Long = 0L
)
