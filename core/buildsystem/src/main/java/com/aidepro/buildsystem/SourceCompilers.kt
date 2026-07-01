package com.aidepro.buildsystem

import timber.log.Timber
import java.io.File

/**
 * JavaCompiler: Compiles Java source files using ECJ (Eclipse Compiler for Java).
 *
 * ECJ is used because it runs on Android's ART runtime without requiring a full JDK.
 * Command: dalvikvm -cp ecj.jar org.eclipse.jdt.internal.compiler.batch.Main
 *          -source 8 -target 8 -classpath android.jar:gen
 *          -d classes src/**\/*.java gen/**\/*.java
 */
class JavaCompiler(private val tools: BuildToolsManager) {

    fun compile(config: BuildConfig): ToolResult {
        val srcDir = File(config.srcDir)
        val genDir = File(config.genDir)
        val classesDir = File(config.classesDir)
        classesDir.mkdirs()

        // Collect all Java source files
        val javaFiles = mutableListOf<String>()
        if (srcDir.exists()) {
            srcDir.walkTopDown()
                .filter { it.extension == "java" }
                .forEach { javaFiles.add(it.absolutePath) }
        }
        // Include generated R.java
        if (genDir.exists()) {
            genDir.walkTopDown()
                .filter { it.extension == "java" }
                .forEach { javaFiles.add(it.absolutePath) }
        }

        if (javaFiles.isEmpty()) {
            return ToolResult(success = true, output = "No Java source files found.")
        }

        // Build classpath
        val classpath = buildClasspath(config)

        val cmd = mutableListOf(
            tools.javaCommand,
            "-classpath", tools.ecjJar,
            "org.eclipse.jdt.internal.compiler.batch.Main",
            "-source", "8",
            "-target", "8",
            "-encoding", "UTF-8",
            "-classpath", classpath,
            "-d", classesDir.absolutePath,
            "-nowarn"
        )
        cmd.addAll(javaFiles)

        Timber.d("ECJ Compile: ${cmd.size} Java files")
        return tools.execute(cmd.toTypedArray())
    }

    private fun buildClasspath(config: BuildConfig): String {
        val paths = mutableListOf(
            tools.androidJar,
            config.genDir,
            config.classesDir
        )
        paths.addAll(config.extraClasspaths)
        return paths.joinToString(File.pathSeparator)
    }
}

/**
 * KotlinCompiler: Compiles Kotlin source files using the embedded Kotlin compiler.
 *
 * Command: dalvikvm -cp kotlin-compiler.jar org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
 *          -classpath android.jar -d classes src/**\/*.kt
 */
class KotlinCompiler(private val tools: BuildToolsManager) {

    fun compile(config: BuildConfig): ToolResult {
        val srcDir = File(config.srcDir)
        val genDir = File(config.genDir)
        val classesDir = File(config.classesDir)
        classesDir.mkdirs()

        // Collect all Kotlin source files
        val kotlinFiles = mutableListOf<String>()
        if (srcDir.exists()) {
            srcDir.walkTopDown()
                .filter { it.extension == "kt" || it.extension == "kts" }
                .forEach { kotlinFiles.add(it.absolutePath) }
        }

        if (kotlinFiles.isEmpty()) {
            return ToolResult(success = true, output = "No Kotlin source files found.")
        }

        // Build classpath
        val classpath = buildClasspath(config)

        val cmd = mutableListOf(
            tools.javaCommand,
            "-classpath", tools.kotlinCompilerJar,
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-classpath", classpath,
            "-d", classesDir.absolutePath,
            "-jvm-target", "1.8",
            "-no-stdlib",
            "-no-reflect"
        )
        // Add source files and generated Java files
        cmd.addAll(kotlinFiles)
        if (genDir.exists()) {
            genDir.walkTopDown()
                .filter { it.extension == "java" }
                .forEach { cmd.add(it.absolutePath) }
        }

        Timber.d("Kotlin Compile: ${kotlinFiles.size} Kotlin files")
        return tools.execute(cmd.toTypedArray())
    }

    private fun buildClasspath(config: BuildConfig): String {
        val paths = mutableListOf(
            tools.androidJar,
            config.genDir,
            config.classesDir
        )
        paths.addAll(config.extraClasspaths)
        return paths.joinToString(File.pathSeparator)
    }
}
