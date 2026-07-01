# AIDE Pro — Advanced Android IDE for Mobile

<p align="center">
  <img src="docs/banner.png" alt="AIDE Pro Banner" width="600"/>
</p>

<p align="center">
  <a href="https://github.com/aidepro/aide-pro/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"/></a>
  <img src="https://img.shields.io/badge/Platform-Android%206.0%2B-green.svg" alt="Platform"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-purple.svg" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/UI-Material%20Design%203-indigo.svg" alt="M3"/>
  <img src="https://img.shields.io/badge/Build-On--Device-orange.svg" alt="On-Device Build"/>
</p>

> **AIDE Pro** is a fully open-source, on-device Android IDE that brings the power of Android Studio to your phone or tablet. Write, build, and run Android apps entirely on your Android device — no PC required.

---

## ✨ Features

| Feature | Description |
|---|---|
| **Advanced Code Editor** | Sora Editor with syntax highlighting for Kotlin, Java, XML, JSON, Gradle |
| **On-Device Build System** | Full pipeline: AAPT2 → ECJ/Kotlinc → D8 → Apksigner |
| **System AI Assistant** | Built-in AI for code generation, error fixing, explanation, refactoring |
| **Material Design 3** | Beautiful M3 UI with dynamic colors, dark/light themes |
| **File Explorer** | Full project tree with file type icons and context menus |
| **Integrated Terminal** | Shell access, build output, Logcat viewer |
| **Project Templates** | Empty Activity, Compose, Bottom Nav, Settings, Login, Maps, Library |
| **Multi-file Editing** | Tabbed editor with multiple open files |
| **Git Integration** | Basic Git operations via terminal |
| **Auto-save** | Automatic file saving with configurable intervals |

---

## 🏗️ Architecture

AIDE Pro follows **Clean Architecture** with **MVVM** pattern:

```
AidePro/
├── app/                          # Main application module
│   └── src/main/java/com/aidepro/app/
│       ├── ui/
│       │   ├── theme/            # Material Design 3 theme (colors, typography, shapes)
│       │   ├── screens/          # Full-screen composables (Welcome, Home, Editor, Settings)
│       │   ├── components/       # Reusable UI components (Editor, FileExplorer, Terminal, AI)
│       │   └── navigation/       # NavGraph and Screen routes
│       ├── viewmodel/            # ViewModels + Repositories
│       └── di/                   # Hilt dependency injection modules
│
├── core/
│   ├── buildsystem/              # Build pipeline (AAPT2, D8, ECJ, Apksigner)
│   ├── editor/                   # Sora Editor integration
│   ├── ai/                       # AI Assistant (streaming chat, code analysis)
│   ├── utils/                    # File I/O, command execution utilities
│   └── terminal/                 # Terminal emulator integration
│
└── assets/
    └── tools/                    # Bundled build tools (aapt2, android.jar, d8.jar, etc.)
```

---

## 🔨 Build Pipeline

The on-device build pipeline mimics Android Studio's build process:

```
Source Files (.java / .kt)
        │
        ▼
  ┌─────────────┐
  │  AAPT2      │  Compile & Link resources → R.java + resources.apk
  └─────────────┘
        │
        ▼
  ┌─────────────┐
  │  ECJ / K2   │  Compile Java/Kotlin → .class files
  └─────────────┘
        │
        ▼
  ┌─────────────┐
  │     D8      │  Dex .class files → classes.dex
  └─────────────┘
        │
        ▼
  ┌─────────────┐
  │  Packager   │  Zip resources + dex → unsigned.apk
  └─────────────┘
        │
        ▼
  ┌─────────────┐
  │  Apksigner  │  Sign APK with debug/release keystore
  └─────────────┘
        │
        ▼
   signed.apk  ✅
```

---

## 🤖 AI Assistant

The built-in System AI is powered by any OpenAI-compatible API:

- **Code Generation** — Describe what you need in natural language
- **Error Analysis** — Automatically analyzes build errors and suggests fixes
- **Code Explanation** — Explains complex code in plain language
- **Refactoring** — Suggests improvements for cleaner, more efficient code
- **Documentation** — Generates KDoc/Javadoc for your code
- **Streaming Responses** — Real-time token streaming for instant feedback

**Supported AI Providers:**
- OpenAI (GPT-4o, GPT-4o-mini, etc.)
- Google Gemini (via OpenAI-compatible endpoint)
- Ollama (local models — completely offline AI)
- Any OpenAI-compatible API

---

## 🚀 Getting Started

### Prerequisites
- Android 8.0+ (API 26+)
- 2GB+ RAM recommended for building
- Storage: ~200MB for app + build tools

### Setup
1. Clone this repository
2. Open in Android Studio (for initial setup)
3. Add your build tools to `assets/tools/`:
   - `aapt2` (native ARM64 binary)
   - `android.jar` (Android SDK stubs)
   - `d8.jar` (from Android SDK build-tools)
   - `apksigner.jar` (from Android SDK build-tools)
   - `ecj.jar` (Eclipse Compiler for Java)
   - `kotlin-compiler.jar` (Kotlin compiler)
   - `debug.keystore` (Android debug keystore)
4. Build and install the APK

### Getting Build Tools
```bash
# From Android SDK (already installed on your device via AIDE)
# Copy from: $ANDROID_SDK/build-tools/<version>/
cp $ANDROID_SDK/build-tools/35.0.0/aapt2 assets/tools/
cp $ANDROID_SDK/build-tools/35.0.0/d8.jar assets/tools/
cp $ANDROID_SDK/build-tools/35.0.0/apksigner.jar assets/tools/
cp $ANDROID_SDK/platforms/android-35/android.jar assets/tools/

# Debug keystore (from ~/.android/)
cp ~/.android/debug.keystore assets/tools/

# ECJ (Eclipse Compiler for Java)
# Download from: https://download.eclipse.org/eclipse/downloads/
# kotlin-compiler.jar: https://github.com/JetBrains/kotlin/releases
```

### Configure AI
1. Open AIDE Pro → Settings → AI Assistant
2. Enter your API key
3. Set the base URL (default: OpenAI)
4. Choose your model

---

## 📦 Technology Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.1 |
| UI Framework | Jetpack Compose + Material Design 3 |
| Code Editor | [Sora Editor](https://github.com/Rosemoe/sora-editor) |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Networking | Ktor (AI streaming) + Retrofit |
| Async | Kotlin Coroutines + Flow |
| Logging | Timber |
| Java Compiler | ECJ (Eclipse Compiler for Java) |
| Kotlin Compiler | Kotlin Compiler Embeddable |
| Dexer | D8 (Google R8) |
| Resource Compiler | AAPT2 |
| APK Signer | Apksigner |

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

```
Copyright 2024 AIDE Pro Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 🙏 Acknowledgments

- [Sora Editor](https://github.com/Rosemoe/sora-editor) — The powerful code editor component
- [AIDE](https://www.aide.traum.de/) — The original inspiration for on-device Android development
- [Termux](https://github.com/termux/termux-app) — Terminal emulator inspiration
- Google Android Team — AAPT2, D8, Apksigner tools
- JetBrains — Kotlin compiler
- Eclipse Foundation — ECJ Java compiler
