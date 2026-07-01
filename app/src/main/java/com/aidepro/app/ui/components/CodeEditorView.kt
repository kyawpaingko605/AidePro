package com.aidepro.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.aidepro.app.viewmodel.EditorViewModel
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.java.JavaLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import kotlinx.coroutines.launch

@Composable
fun CodeEditorView(
    filePath: String,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.toArgb() < 0xFF808080.toInt()
    val fileContent by viewModel.getFileContent(filePath).collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { context ->
            CodeEditor(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Configure editor appearance
                isWordwrap = false
                isLineNumberEnabled = true
                isHighlightCurrentLine = true
                typefaceText = android.graphics.Typeface.MONOSPACE
                setTextSize(14f)
                setTabWidth(4)

                // Set color scheme
                colorScheme = if (isDarkTheme) SchemeDarcula() else SchemeEclipse()

                // Enable auto-completion
                getComponent(EditorAutoCompletion::class.java).isEnabled = true

                // Set language based on file extension
                val language = when {
                    filePath.endsWith(".java") -> JavaLanguage()
                    filePath.endsWith(".kt") || filePath.endsWith(".kts") ->
                        createTextMateLanguage(context, "kotlin")
                    filePath.endsWith(".xml") ->
                        createTextMateLanguage(context, "xml")
                    filePath.endsWith(".json") ->
                        createTextMateLanguage(context, "json")
                    filePath.endsWith(".md") ->
                        createTextMateLanguage(context, "markdown")
                    filePath.endsWith(".gradle") ->
                        createTextMateLanguage(context, "groovy")
                    else -> null
                }
                language?.let { setEditorLanguage(it) }

                // Listen for content changes
                subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
                    scope.launch {
                        viewModel.onFileContentChanged(filePath, text.toString())
                    }
                }

                // Listen for cursor position changes
                subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
                    val cursor = this.cursor
                    viewModel.updateCursorPosition(cursor.leftLine + 1, cursor.leftColumn + 1)
                }
            }
        },
        update = { editor ->
            // Update content when file changes
            if (editor.text.toString() != fileContent && fileContent.isNotEmpty()) {
                val cursor = editor.cursor
                val line = cursor.leftLine
                val col = cursor.leftColumn
                editor.setText(fileContent)
                // Restore cursor position
                try {
                    editor.setSelection(line, col)
                } catch (e: Exception) {
                    // Ignore if position is out of bounds
                }
            }
        },
        modifier = modifier
    )
}

private fun createTextMateLanguage(
    context: android.content.Context,
    languageId: String
): TextMateLanguage? {
    return try {
        // Initialize TextMate registry with bundled grammars
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(context.assets)
        )
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        TextMateLanguage.create(languageId, true)
    } catch (e: Exception) {
        null
    }
}
