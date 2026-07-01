plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aidepro.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aidepro.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Provide AI API Key via local.properties (not hardcoded)
        buildConfigField("String", "AI_API_KEY", "\"${project.findProperty("AI_API_KEY") ?: ""}\"")
        buildConfigField("String", "AI_BASE_URL", "\"${project.findProperty("AI_BASE_URL") ?: "https://api.openai.com/v1/"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    // Include native tools and android.jar as assets
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "../../assets")
        }
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Sora Editor
    implementation(libs.sora.editor)
    implementation(libs.sora.editor.language.java)
    implementation(libs.sora.editor.language.textmate)
    implementation(libs.sora.editor.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Coil
    implementation(libs.coil.compose)

    // Timber
    implementation(libs.timber)

    // Markdown
    implementation(libs.markwon.core)
    implementation(libs.markwon.syntax)

    // Ktor (AI Streaming)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Core modules
    implementation(project(":core:buildsystem"))
    implementation(project(":core:editor"))
    implementation(project(":core:ai"))
    implementation(project(":core:utils"))
    implementation(project(":core:terminal"))
}
