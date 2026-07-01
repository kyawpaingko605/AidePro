package com.aidepro.buildsystem

import timber.log.Timber
import java.io.File

/**
 * AAPT2 Compiler: Compiles Android resources to flat binary format.
 *
 * Command: aapt2 compile -o <output_dir> <res_dir>
 */
class Aapt2Compiler(private val tools: BuildToolsManager) {

    fun compile(config: BuildConfig): ToolResult {
        val resDir = File(config.resDir)
        if (!resDir.exists()) {
            return ToolResult(success = true, output = "No resources directory found, skipping.")
        }

        val outputDir = File(config.resCompiledDir)
        outputDir.mkdirs()

        val cmd = arrayOf(
            tools.aapt2Binary,
            "compile",
            "--dir", resDir.absolutePath,
            "-o", outputDir.absolutePath,
            "-v"
        )

        Timber.d("AAPT2 Compile: ${cmd.joinToString(" ")}")
        return tools.execute(cmd)
    }
}

/**
 * AAPT2 Linker: Links compiled resources and generates R.java + base APK.
 *
 * Command: aapt2 link -o <output.apk> -I android.jar
 *          --manifest AndroidManifest.xml
 *          --java <gen_dir>
 *          <compiled_resources...>
 */
class Aapt2Linker(private val tools: BuildToolsManager) {

    fun link(config: BuildConfig): ToolResult {
        val compiledResDir = File(config.resCompiledDir)
        val flatFiles = compiledResDir.walkTopDown()
            .filter { it.extension == "flat" }
            .map { it.absolutePath }
            .toList()

        val outputApk = File(config.apkDir, "resources.apk")
        val genDir = File(config.genDir)
        genDir.mkdirs()

        val cmd = mutableListOf(
            tools.aapt2Binary,
            "link",
            "-o", outputApk.absolutePath,
            "-I", tools.androidJar,
            "--manifest", config.manifestFile,
            "--java", genDir.absolutePath,
            "--auto-add-overlay",
            "--min-sdk-version", config.minSdk.toString(),
            "--target-sdk-version", config.targetSdk.toString(),
            "--version-code", config.versionCode.toString(),
            "--version-name", config.versionName,
            "-v"
        )

        // Add compiled flat resource files
        cmd.addAll(flatFiles)

        // Add assets directory if it exists
        val assetsDir = File(config.assetsDir)
        if (assetsDir.exists()) {
            cmd.addAll(listOf("-A", assetsDir.absolutePath))
        }

        Timber.d("AAPT2 Link: ${cmd.joinToString(" ")}")
        return tools.execute(cmd.toTypedArray())
    }
}
