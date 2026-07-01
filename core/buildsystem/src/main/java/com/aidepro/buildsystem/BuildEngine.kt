package com.aidepro.buildsystem

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.io.File

/**
 * BuildEngine: The core on-device build orchestrator for AIDE Pro.
 *
 * Build Pipeline:
 * 1. AAPT2 Compile  → Compiles resources (.xml) to flat binary format (.flat)
 * 2. AAPT2 Link     → Links resources, generates R.java and base APK
 * 3. ECJ/Kotlinc    → Compiles Java/Kotlin sources to .class files
 * 4. D8             → Dexes .class files to classes.dex (DEX bytecode)
 * 5. APK Package    → Assembles the final APK (zip resources + dex)
 * 6. Apksigner      → Signs the APK with a debug/release keystore
 */
class BuildEngine(private val context: Context) {

    private val toolsManager = BuildToolsManager(context)
    private val buildLogger = BuildLogger()

    /**
     * Full build pipeline. Emits [BuildEvent] as the build progresses.
     */
    fun buildProject(config: BuildConfig): Flow<BuildEvent> = flow {
        emit(BuildEvent.Started("Starting build for ${config.projectName}..."))

        try {
            // Step 1: Validate tools
            emit(BuildEvent.Progress(5, "Validating build tools..."))
            toolsManager.ensureToolsExtracted()

            // Step 2: Clean build directory
            emit(BuildEvent.Progress(10, "Cleaning build directory..."))
            cleanBuildDir(config)

            // Step 3: AAPT2 Compile resources
            emit(BuildEvent.Progress(20, "Compiling resources (AAPT2)..."))
            val aapt2Result = Aapt2Compiler(toolsManager).compile(config)
            if (!aapt2Result.success) {
                emit(BuildEvent.Failed("AAPT2 compile failed:\n${aapt2Result.output}"))
                return@flow
            }
            emit(BuildEvent.Log(aapt2Result.output, BuildLogLevel.INFO))

            // Step 4: AAPT2 Link
            emit(BuildEvent.Progress(35, "Linking resources (AAPT2 Link)..."))
            val aapt2LinkResult = Aapt2Linker(toolsManager).link(config)
            if (!aapt2LinkResult.success) {
                emit(BuildEvent.Failed("AAPT2 link failed:\n${aapt2LinkResult.output}"))
                return@flow
            }
            emit(BuildEvent.Log(aapt2LinkResult.output, BuildLogLevel.INFO))

            // Step 5: Compile Java/Kotlin sources
            emit(BuildEvent.Progress(50, "Compiling sources..."))
            val compileResult = when (config.language) {
                ProjectLanguage.KOTLIN -> KotlinCompiler(toolsManager).compile(config)
                ProjectLanguage.JAVA -> JavaCompiler(toolsManager).compile(config)
                ProjectLanguage.MIXED -> {
                    // Compile Java first, then Kotlin with Java classpath
                    val javaResult = JavaCompiler(toolsManager).compile(config)
                    if (!javaResult.success) return@flow emit(BuildEvent.Failed(javaResult.output))
                    KotlinCompiler(toolsManager).compile(config)
                }
            }
            if (!compileResult.success) {
                emit(BuildEvent.Failed("Compilation failed:\n${compileResult.output}"))
                return@flow
            }
            emit(BuildEvent.Log(compileResult.output, BuildLogLevel.INFO))

            // Step 6: D8 Dexing
            emit(BuildEvent.Progress(70, "Dexing with D8..."))
            val d8Result = D8Dexer(toolsManager).dex(config)
            if (!d8Result.success) {
                emit(BuildEvent.Failed("D8 dexing failed:\n${d8Result.output}"))
                return@flow
            }
            emit(BuildEvent.Log(d8Result.output, BuildLogLevel.INFO))

            // Step 7: Package APK
            emit(BuildEvent.Progress(85, "Packaging APK..."))
            val packager = ApkPackager()
            val apkPath = packager.packageApk(config)

            // Step 8: Sign APK
            emit(BuildEvent.Progress(95, "Signing APK..."))
            val signer = ApkSigner(toolsManager)
            val signedApkPath = signer.sign(apkPath, config)

            emit(BuildEvent.Progress(100, "Build complete!"))
            emit(BuildEvent.Success(signedApkPath))

        } catch (e: Exception) {
            Timber.e(e, "Build failed with exception")
            emit(BuildEvent.Failed("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun cleanBuildDir(config: BuildConfig) {
        val buildDir = File(config.buildDir)
        if (buildDir.exists()) {
            buildDir.deleteRecursively()
        }
        buildDir.mkdirs()
        File(config.buildDir, "gen").mkdirs()        // For R.java
        File(config.buildDir, "classes").mkdirs()    // For .class files
        File(config.buildDir, "dex").mkdirs()        // For .dex files
        File(config.buildDir, "apk").mkdirs()        // For APK files
        File(config.buildDir, "res").mkdirs()        // For compiled resources
    }
}

// ─── Build Configuration ────────────────────────────────────────────────────

data class BuildConfig(
    val projectName: String,
    val projectDir: String,
    val buildDir: String,
    val packageName: String,
    val language: ProjectLanguage = ProjectLanguage.KOTLIN,
    val minSdk: Int = 21,
    val targetSdk: Int = 35,
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val buildType: BuildType = BuildType.DEBUG,
    val extraClasspaths: List<String> = emptyList()
) {
    val srcDir get() = "$projectDir/app/src/main/java"
    val resDir get() = "$projectDir/app/src/main/res"
    val manifestFile get() = "$projectDir/app/src/main/AndroidManifest.xml"
    val assetsDir get() = "$projectDir/app/src/main/assets"
    val genDir get() = "$buildDir/gen"
    val classesDir get() = "$buildDir/classes"
    val dexDir get() = "$buildDir/dex"
    val apkDir get() = "$buildDir/apk"
    val resCompiledDir get() = "$buildDir/res"
}

enum class ProjectLanguage { JAVA, KOTLIN, MIXED }
enum class BuildType { DEBUG, RELEASE }

// ─── Build Events ────────────────────────────────────────────────────────────

sealed class BuildEvent {
    data class Started(val message: String) : BuildEvent()
    data class Progress(val percent: Int, val message: String) : BuildEvent()
    data class Log(val message: String, val level: BuildLogLevel) : BuildEvent()
    data class Success(val apkPath: String) : BuildEvent()
    data class Failed(val error: String) : BuildEvent()
}

enum class BuildLogLevel { INFO, WARNING, ERROR }

data class ToolResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int = 0
)

class BuildLogger {
    private val logs = mutableListOf<String>()
    fun log(message: String) { logs.add(message) }
    fun getLogs(): List<String> = logs.toList()
}
