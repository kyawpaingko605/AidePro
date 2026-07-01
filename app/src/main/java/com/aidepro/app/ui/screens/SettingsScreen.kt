package com.aidepro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidepro.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val aiBaseUrl by viewModel.aiBaseUrl.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val fontSize by viewModel.editorFontSize.collectAsState()
    val tabSize by viewModel.editorTabSize.collectAsState()
    val wordWrap by viewModel.wordWrap.collectAsState()
    val autoSave by viewModel.autoSave.collectAsState()

    var showApiKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Appearance ──────────────────────────────────────────────────
            SettingsSection(title = "Appearance") {
                SettingsSwitchItem(
                    title = "Dark Theme",
                    subtitle = "Use dark color scheme",
                    icon = Icons.Default.DarkMode,
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) }
                )
                SettingsSwitchItem(
                    title = "Dynamic Colors",
                    subtitle = "Use Material You wallpaper colors (Android 12+)",
                    icon = Icons.Default.Palette,
                    checked = useDynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            }

            // ── Editor ──────────────────────────────────────────────────────
            SettingsSection(title = "Editor") {
                SettingsSliderItem(
                    title = "Font Size",
                    subtitle = "${fontSize}sp",
                    icon = Icons.Default.FormatSize,
                    value = fontSize.toFloat(),
                    valueRange = 10f..24f,
                    steps = 13,
                    onValueChange = { viewModel.setEditorFontSize(it.toInt()) }
                )
                SettingsSliderItem(
                    title = "Tab Size",
                    subtitle = "$tabSize spaces",
                    icon = Icons.Default.SpaceBar,
                    value = tabSize.toFloat(),
                    valueRange = 2f..8f,
                    steps = 2,
                    onValueChange = { viewModel.setEditorTabSize(it.toInt()) }
                )
                SettingsSwitchItem(
                    title = "Word Wrap",
                    subtitle = "Wrap long lines",
                    icon = Icons.Default.WrapText,
                    checked = wordWrap,
                    onCheckedChange = { viewModel.setWordWrap(it) }
                )
                SettingsSwitchItem(
                    title = "Auto Save",
                    subtitle = "Save files automatically",
                    icon = Icons.Default.Save,
                    checked = autoSave,
                    onCheckedChange = { viewModel.setAutoSave(it) }
                )
            }

            // ── AI Assistant ─────────────────────────────────────────────────
            SettingsSection(title = "AI Assistant") {
                // API Key
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "API Key",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiApiKey,
                        onValueChange = { viewModel.setAiApiKey(it) },
                        placeholder = { Text("sk-...") },
                        visualTransformation = if (showApiKey)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Base URL
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "API Base URL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiBaseUrl,
                        onValueChange = { viewModel.setAiBaseUrl(it) },
                        placeholder = { Text("https://api.openai.com/v1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        "Compatible with OpenAI, Gemini, Ollama, and any OpenAI-compatible API",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Model
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Model",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiModel,
                        onValueChange = { viewModel.setAiModel(it) },
                        placeholder = { Text("gpt-4o-mini") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    // Model presets
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("gpt-4o-mini", "gpt-4o", "gemini-pro").forEach { preset ->
                            SuggestionChip(
                                onClick = { viewModel.setAiModel(preset) },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // ── About ────────────────────────────────────────────────────────
            SettingsSection(title = "About") {
                ListItem(
                    headlineContent = { Text("AIDE Pro") },
                    supportingContent = { Text("Version 1.0.0-alpha • Open Source") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                )
                ListItem(
                    headlineContent = { Text("GitHub Repository") },
                    supportingContent = { Text("github.com/aidepro/aide-pro") },
                    leadingContent = {
                        Icon(Icons.Default.Code, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                )
                ListItem(
                    headlineContent = { Text("License") },
                    supportingContent = { Text("Apache License 2.0") },
                    leadingContent = {
                        Icon(Icons.Default.Gavel, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun SettingsSliderItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
