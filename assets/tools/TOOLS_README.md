# AIDE Pro — Bundled Build Tools

All binaries are **ARM64 (aarch64) Android native** — they run directly on Android devices.
JAR files require a JVM (Android Runtime / ART).

## Binaries

| File | Source | Description |
|------|--------|-------------|
| `aapt` | Android 13 (Termux) | Resource packaging tool v1 |
| `aapt2` | Android 13 (Termux) | Resource packaging tool v2 (preferred) |
| `zipalign` | Android 13 (Termux) | APK zip alignment tool |

## JVM Tools (JAR files)

| File | Version | Description |
|------|---------|-------------|
| `ecj.jar` | 4.12 | Eclipse Java Compiler (javac replacement) |
| `d8.jar` | 35.x | D8 dexer — converts .class → .dex |
| `apksigner.jar` | 35.x | Signs APKs (v1/v2/v3 signature schemes) |
| `kotlin-compiler.jar` | 2.x | Kotlin compiler (kotlinc) |

## Android SDK

| File | Description |
|------|-------------|
| `android.jar` | Android API 35 stubs (compile-time classpath) |

## Keystores

| File | Alias | Password | Use |
|------|-------|----------|-----|
| `debug.keystore` | `androiddebugkey` | `android` | Debug builds (standard Android debug key) |
| `release.keystore` | `aidepro_release` | `aidepro123` | Release builds |

> ⚠️ **For production apps**: Generate your own release keystore and keep it secret.
> The bundled `release.keystore` is for development/testing only.

## Wrapper Scripts

| File | Description |
|------|-------------|
| `javac` | Wrapper — delegates to ECJ (`ecj.jar`) |
| `keytool` | Info script — keystores are pre-bundled |

## Build Pipeline (AIDE Pro)

```
.kt / .java  ──► kotlinc / ecj ──► .class files
.class       ──► d8.jar         ──► classes.dex
res/ + .dex  ──► aapt2          ──► unsigned.apk
unsigned.apk ──► zipalign       ──► aligned.apk
aligned.apk  ──► apksigner      ──► signed.apk  ✅
```
