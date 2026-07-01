package com.aidepro.buildsystem

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * BuildToolsManager: Manages extraction and access to build tool binaries.
 *
 * Tools bundled in assets/tools/:
 * - aapt2          (Android Asset Packaging Tool 2, native binary)
 * - android.jar    (Android SDK stubs for compilation)
 * - d8.jar         (D8 dexer, Java-based)
 * - apksigner.jar  (APK signing tool, Java-based)
 * - ecj.jar        (Eclipse Compiler for Java)
 * - kotlin-compiler.jar (Kotlin compiler)
 * - debug.keystore (Debug signing keystore)
 */
class BuildToolsManager(private val context: Context) {

    private val toolsDir: File by lazy {
        File(context.filesDir, "build_tools").also { it.mkdirs() }
    }

    val aapt2Binary: String get() = File(toolsDir, "aapt2").absolutePath
    val androidJar: String get() = File(toolsDir, "android.jar").absolutePath
    val d8Jar: String get() = File(toolsDir, "d8.jar").absolutePath
    val apksignerJar: String get() = File(toolsDir, "apksigner.jar").absolutePath
    val ecjJar: String get() = File(toolsDir, "ecj.jar").absolutePath
    val kotlinCompilerJar: String get() = File(toolsDir, "kotlin-compiler.jar").absolutePath
    val debugKeystore: String get() = File(toolsDir, "debug.keystore").absolutePath

    // Java binary path (Android's embedded JVM via Dalvik/ART)
    val javaCommand: String get() = findJavaCommand()

    /**
     * Ensures all tools are extracted from assets to internal storage.
     * Only extracts if not already present or if version changed.
     */
    fun ensureToolsExtracted() {
        val tools = listOf(
            "tools/aapt2",
            "tools/android.jar",
            "tools/d8.jar",
            "tools/apksigner.jar",
            "tools/ecj.jar",
            "tools/kotlin-compiler.jar",
            "tools/debug.keystore"
        )

        tools.forEach { assetPath ->
            val fileName = assetPath.substringAfterLast("/")
            val destFile = File(toolsDir, fileName)

            if (!destFile.exists()) {
                try {
                    context.assets.open(assetPath).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Make aapt2 executable
                    if (fileName == "aapt2") {
                        destFile.setExecutable(true, false)
                    }
                    Timber.d("Extracted: $fileName")
                } catch (e: Exception) {
                    Timber.w("Could not extract $fileName: ${e.message}")
                }
            }
        }
    }

    /**
     * Executes a shell command and returns the result.
     */
    fun execute(command: Array<String>, workingDir: File? = null): ToolResult {
        return try {
            val processBuilder = ProcessBuilder(*command).apply {
                workingDir?.let { directory(it) }
                redirectErrorStream(true)
                environment()["PATH"] = "${toolsDir.absolutePath}:${System.getenv("PATH")}"
            }

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            Timber.d("Command: ${command.joinToString(" ")}\nExit: $exitCode\nOutput: $output")

            ToolResult(
                success = exitCode == 0,
                output = output.trim(),
                exitCode = exitCode
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute: ${command.joinToString(" ")}")
            ToolResult(success = false, output = "Execution error: ${e.message}", exitCode = -1)
        }
    }

    /**
     * Executes a JAR file using Android's embedded Java runtime.
     */
    fun executeJar(jarPath: String, mainClass: String, args: Array<String>): ToolResult {
        val cmd = mutableListOf<String>().apply {
            add(javaCommand)
            add("-cp")
            add(jarPath)
            add(mainClass)
            addAll(args)
        }
        return execute(cmd.toTypedArray())
    }

    private fun findJavaCommand(): String {
        // On Android, we use the app_process or dalvikvm to run Java
        // For real execution, tools like D8 and ECJ need to be run via
        // ProcessBuilder with the Android runtime
        val possiblePaths = listOf(
            "/system/bin/dalvikvm64",
            "/system/bin/dalvikvm",
            "/system/bin/app_process64",
            "/system/bin/app_process"
        )
        return possiblePaths.firstOrNull { File(it).exists() } ?: "dalvikvm"
    }

    fun getToolsDir(): File = toolsDir
}
