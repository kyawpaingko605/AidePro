package com.aidepro.app.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidepro.app.ui.screens.ProjectTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class NewProjectViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating

    fun createProject(
        name: String,
        packageName: String,
        template: ProjectTemplate,
        language: String,
        minSdk: Int,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isCreating.value = true
            try {
                val projectPath = withContext(Dispatchers.IO) {
                    generateProject(name, packageName, template, language, minSdk)
                }
                // Save to recent projects
                settingsRepository.addRecentProject(
                    ProjectInfo(
                        name = name,
                        path = projectPath,
                        type = language.lowercase(),
                        lastOpened = System.currentTimeMillis()
                    )
                )
                onSuccess(projectPath)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create project")
            } finally {
                _isCreating.value = false
            }
        }
    }

    private fun generateProject(
        name: String,
        packageName: String,
        template: ProjectTemplate,
        language: String,
        minSdk: Int
    ): String {
        val projectsDir = File(
            Environment.getExternalStorageDirectory(),
            "AidePro/Projects"
        ).also { it.mkdirs() }

        val projectDir = File(projectsDir, name)
        if (projectDir.exists()) projectDir.deleteRecursively()

        val packagePath = packageName.replace(".", "/")
        val isKotlin = language == "Kotlin"
        val ext = if (isKotlin) "kt" else "java"

        // Create directory structure
        val dirs = listOf(
            "app/src/main/java/$packagePath",
            "app/src/main/res/layout",
            "app/src/main/res/values",
            "app/src/main/res/drawable",
            "app/src/main/res/mipmap-hdpi",
            "app/src/main/assets",
            "gradle/wrapper"
        )
        dirs.forEach { File(projectDir, it).mkdirs() }

        // Generate files based on template
        generateManifest(projectDir, packageName, name)
        generateMainActivity(projectDir, packageName, name, isKotlin, template)
        generateStringsXml(projectDir, name)
        generateColorsXml(projectDir)
        generateThemeXml(projectDir, name)
        generateAppBuildGradle(projectDir, packageName, minSdk, isKotlin)
        generateRootBuildGradle(projectDir)
        generateSettingsGradle(projectDir, name)
        generateGradleWrapper(projectDir)
        generateGitignore(projectDir)
        generateReadme(projectDir, name)

        // Template-specific files
        when (template) {
            ProjectTemplate.COMPOSE_ACTIVITY -> generateComposeMainActivity(
                projectDir, packageName, name, isKotlin
            )
            ProjectTemplate.BOTTOM_NAV -> generateBottomNavActivity(
                projectDir, packageName, name, isKotlin
            )
            else -> {} // Empty activity already generated above
        }

        return projectDir.absolutePath
    }

    private fun generateManifest(dir: File, packageName: String, appName: String) {
        File(dir, "app/src/main/AndroidManifest.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.$appName">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
        """.trimIndent())
    }

    private fun generateMainActivity(
        dir: File, packageName: String, appName: String,
        isKotlin: Boolean, template: ProjectTemplate
    ) {
        val packagePath = packageName.replace(".", "/")
        val ext = if (isKotlin) "kt" else "java"
        val content = if (isKotlin) {
            """
package $packageName

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
            """.trimIndent()
        } else {
            """
package $packageName;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
            """.trimIndent()
        }
        File(dir, "app/src/main/java/$packagePath/MainActivity.$ext").writeText(content)

        // Generate layout
        File(dir, "app/src/main/res/layout/activity_main.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello, $appName!"
        android:textSize="24sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
        """.trimIndent())
    }

    private fun generateComposeMainActivity(
        dir: File, packageName: String, appName: String, isKotlin: Boolean
    ) {
        val packagePath = packageName.replace(".", "/")
        File(dir, "app/src/main/java/$packagePath/MainActivity.kt").writeText("""
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ${packageName}.ui.theme.${appName}Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ${appName}Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello, ${'$'}name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ${appName}Theme {
        Greeting("Android")
    }
}
        """.trimIndent())
    }

    private fun generateBottomNavActivity(
        dir: File, packageName: String, appName: String, isKotlin: Boolean
    ) {
        // Bottom navigation template - simplified version
        generateComposeMainActivity(dir, packageName, appName, isKotlin)
    }

    private fun generateStringsXml(dir: File, appName: String) {
        File(dir, "app/src/main/res/values/strings.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$appName</string>
</resources>
        """.trimIndent())
    }

    private fun generateColorsXml(dir: File) {
        File(dir, "app/src/main/res/values/colors.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
        """.trimIndent())
    }

    private fun generateThemeXml(dir: File, appName: String) {
        File(dir, "app/src/main/res/values/themes.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.$appName" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
    </style>
</resources>
        """.trimIndent())
    }

    private fun generateAppBuildGradle(dir: File, packageName: String, minSdk: Int, isKotlin: Boolean) {
        val kotlinPlugin = if (isKotlin) """
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")""" else ""

        File(dir, "app/build.gradle.kts").writeText("""
plugins {
    id("com.android.application")$kotlinPlugin
}

android {
    namespace = "$packageName"
    compileSdk = 35

    defaultConfig {
        applicationId = "$packageName"
        minSdk = $minSdk
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    ${if (isKotlin) """kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }""" else ""}
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    ${if (isKotlin) """implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")""" else ""}
}
        """.trimIndent())
    }

    private fun generateRootBuildGradle(dir: File) {
        File(dir, "build.gradle.kts").writeText("""
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}
        """.trimIndent())
    }

    private fun generateSettingsGradle(dir: File, name: String) {
        File(dir, "settings.gradle.kts").writeText("""
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "$name"
include(":app")
        """.trimIndent())
    }

    private fun generateGradleWrapper(dir: File) {
        File(dir, "gradle/wrapper/gradle-wrapper.properties").writeText("""
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
        """.trimIndent())
    }

    private fun generateGitignore(dir: File) {
        File(dir, ".gitignore").writeText("""
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
        """.trimIndent())
    }

    private fun generateReadme(dir: File, name: String) {
        File(dir, "README.md").writeText("""
# $name

An Android application created with **AIDE Pro** — the advanced on-device Android IDE.

## Getting Started

This project was generated using AIDE Pro with the following configuration:
- Build System: Gradle (Kotlin DSL)
- Language: Kotlin
- Min SDK: 21+

## Building

Open this project in AIDE Pro and tap the **Build** button, or use the terminal:

```bash
./gradlew assembleDebug
```

## License

This project is open source. See [LICENSE](LICENSE) for details.
        """.trimIndent())
    }
}
