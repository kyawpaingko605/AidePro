package com.aidepro.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidepro.app.viewmodel.EditorViewModel
import com.aidepro.app.viewmodel.TerminalLine
import com.aidepro.app.viewmodel.TerminalLineType

@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    buildOutput: List<TerminalLine>,
    viewModel: EditorViewModel
) {
    var commandInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val activeTab by viewModel.terminalActiveTab.collectAsState()

    // Auto-scroll to bottom when new output arrives
    LaunchedEffect(buildOutput.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF0D0D14),
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Terminal header with tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A2E))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tabs
                listOf("Build Output", "Terminal", "Logcat").forEachIndexed { index, title ->
                    TextButton(
                        onClick = { viewModel.setTerminalTab(index) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (activeTab == index)
                                MaterialTheme.colorScheme.primary
                            else Color.Gray
                        )
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Clear button
                IconButton(
                    onClick = { viewModel.clearTerminal() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ClearAll,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }

                // Close button
                IconButton(
                    onClick = { viewModel.toggleTerminal() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close terminal",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }
            }

            // Output area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                buildOutput.forEach { line ->
                    TerminalLineView(line = line)
                }
            }

            // Command input (for Terminal tab)
            if (activeTab == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A2E))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$ ",
                        color = Color(0xFF4CAF50),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    BasicTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = Color(0xFFE6E1F9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                viewModel.executeCommand(commandInput)
                                commandInput = ""
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Execute",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLineView(line: TerminalLine) {
    val (color, prefix) = when (line.type) {
        TerminalLineType.INFO -> Color(0xFFE6E1F9) to ""
        TerminalLineType.SUCCESS -> Color(0xFF4CAF50) to "✓ "
        TerminalLineType.ERROR -> Color(0xFFFF6B6B) to "✗ "
        TerminalLineType.WARNING -> Color(0xFFFFB74D) to "⚠ "
        TerminalLineType.COMMAND -> Color(0xFF9B84FF) to "$ "
        TerminalLineType.HEADER -> Color(0xFF4DD0E1) to "▶ "
    }

    Text(
        text = "$prefix${line.text}",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}
