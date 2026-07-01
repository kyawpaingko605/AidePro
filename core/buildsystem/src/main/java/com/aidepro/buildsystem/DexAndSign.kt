package com.aidepro.buildsystem

import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * D8Dexer: Converts .class files to Dalvik Executable (.dex) format using D8.
 *
 * D8 is Google's dexer that produces smaller, faster DEX files compared to DX.
 * Command: dalvikvm -cp d8.jar com.android.tools.r8.D8
 *          --output <dex_dir> --lib android.jar <classes_dir>/**\/*.class
 */
class D8Dexer(private val tools: BuildToolsManager) {

    fun dex(config: BuildConfig): ToolResult {
        val classesDir = File(config.classesDir)
        val dexDir = File(config.dexDir)
        dexDir.mkdirs()

        // Collect all .class files
        val classFiles = classesDir.walkTopDown()
            .filter { it.extension == "class" }
            .map { it.absolutePath }
            .toList()

        if (classFiles.isEmpty()) {
            return ToolResult(success = false, output = "No .class files found to dex.")
        }

        val cmd = mutableListOf(
            tools.javaCommand,
            "-classpath", tools.d8Jar,
            "com.android.tools.r8.D8",
            "--output", dexDir.absolutePath,
            "--lib", tools.androidJar,
            "--min-api", config.minSdk.toString()
        )

        // Add release mode optimization
        if (config.buildType == BuildType.RELEASE) {
            cmd.add("--release")
        } else {
            cmd.add("--debug")
        }

        cmd.addAll(classFiles)

        Timber.d("D8 Dex: ${classFiles.size} class files")
        return tools.execute(cmd.toTypedArray())
    }
}

/**
 * ApkPackager: Assembles the final APK by combining resources and DEX files.
 *
 * The base APK from AAPT2 already contains resources. We add the DEX file to it.
 */
class ApkPackager {

    fun packageApk(config: BuildConfig): String {
        val resourcesApk = File(config.apkDir, "resources.apk")
        val dexFile = File(config.dexDir, "classes.dex")
        val unalignedApk = File(config.apkDir, "${config.projectName}-unaligned.apk")

        // Copy resources APK as base
        resourcesApk.copyTo(unalignedApk, overwrite = true)

        // Add classes.dex to the APK (ZIP format)
        if (dexFile.exists()) {
            addFileToZip(unalignedApk, dexFile, "classes.dex")
        }

        // Add additional dex files if present (multidex)
        var dexIndex = 2
        while (true) {
            val additionalDex = File(config.dexDir, "classes$dexIndex.dex")
            if (!additionalDex.exists()) break
            addFileToZip(unalignedApk, additionalDex, "classes$dexIndex.dex")
            dexIndex++
        }

        Timber.d("APK packaged: ${unalignedApk.absolutePath}")
        return unalignedApk.absolutePath
    }

    private fun addFileToZip(zipFile: File, fileToAdd: File, entryName: String) {
        val tempFile = File(zipFile.parent, "${zipFile.name}.tmp")
        zipFile.copyTo(tempFile, overwrite = true)

        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            // Copy existing entries
            ZipFile(tempFile).use { existingZip ->
                existingZip.entries().asSequence().forEach { entry ->
                    if (entry.name != entryName) {
                        zos.putNextEntry(ZipEntry(entry.name))
                        existingZip.getInputStream(entry).copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
            // Add new file
            zos.putNextEntry(ZipEntry(entryName))
            fileToAdd.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }

        tempFile.delete()
    }
}

/**
 * ApkSigner: Signs the APK using apksigner.
 *
 * Uses a debug keystore for debug builds, or a provided keystore for release.
 * Command: dalvikvm -cp apksigner.jar com.android.apksigner.ApkSignerTool sign
 *          --ks debug.keystore --ks-pass pass:android
 *          --out signed.apk unsigned.apk
 */
class ApkSigner(private val tools: BuildToolsManager) {

    fun sign(apkPath: String, config: BuildConfig): String {
        val signedApk = File(config.apkDir, "${config.projectName}-debug.apk")

        val keystorePath = tools.debugKeystore
        val keystorePass = "android"
        val keyAlias = "androiddebugkey"
        val keyPass = "android"

        val cmd = arrayOf(
            tools.javaCommand,
            "-classpath", tools.apksignerJar,
            "com.android.apksigner.ApkSignerTool",
            "sign",
            "--ks", keystorePath,
            "--ks-pass", "pass:$keystorePass",
            "--ks-key-alias", keyAlias,
            "--key-pass", "pass:$keyPass",
            "--out", signedApk.absolutePath,
            "--v1-signing-enabled", "true",
            "--v2-signing-enabled", "true",
            apkPath
        )

        val result = tools.execute(cmd)
        if (!result.success) {
            throw BuildException("APK signing failed: ${result.output}")
        }

        Timber.d("APK signed: ${signedApk.absolutePath}")
        return signedApk.absolutePath
    }
}

class BuildException(message: String) : Exception(message)
