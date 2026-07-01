# AIDE Pro ProGuard Rules

# Keep build tools classes
-keep class com.android.tools.r8.** { *; }
-keep class org.eclipse.jdt.** { *; }
-keep class org.jetbrains.kotlin.** { *; }
-keep class com.android.apksigner.** { *; }

# Keep Sora Editor
-keep class io.github.rosemoe.sora.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Keep Room
-keep class androidx.room.** { *; }

# Keep data classes
-keep class com.aidepro.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
