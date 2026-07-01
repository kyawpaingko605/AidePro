plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.aidepro.editor"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.sora.editor)
    implementation(libs.sora.editor.language.java)
    implementation(libs.sora.editor.language.textmate)
}
