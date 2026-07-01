package com.aidepro.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidepro.app.viewmodel.AiChatMessage
import com.aidepro.app.viewmodel.EditorViewModel

@Composable
fun AiAssistantPanel(
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel
) {
    val chatMessages by viewModel.aiChatMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AI Avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "AI Assistant",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            if (isAiThinking) "Thinking..." else "Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    // Quick action buttons
                    IconButton(
                        onClick = { viewModel.clearAiChat() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ClearAll,
                            contentDescription = "Clear chat",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleAiPanel() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Quick action chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiQuickChip("Fix Error", Icons.Default.BugReport) {
                    viewModel.aiFixCurrentError()
                }
                AiQuickChip("Explain Code", Icons.Default.Help) {
                    viewModel.aiExplainSelectedCode()
                }
                AiQuickChip("Generate", Icons.Default.Add) {
                    inputText = "Generate "
                }
                AiQuickChip("Refactor", Icons.Default.AutoFixHigh) {
                    viewModel.aiRefactorSelectedCode()
                }
                AiQuickChip("Add Docs", Icons.Default.Description) {
                    viewModel.aiGenerateDocs()
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Chat messages
            if (chatMessages.isEmpty()) {
                AiWelcomeState(
                    modifier = Modifier.weight(1f),
                    onSuggestionClick = { suggestion ->
                        inputText = suggestion
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatMessages) { message ->
                        AiChatBubble(message = message)
                    }
                    if (isAiThinking) {
                        item {
                            AiThinkingIndicator()
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    "Ask AI anything...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isAiThinking) {
                            viewModel.sendAiMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAiThinking,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isAiThinking) Icons.Default.HourglassEmpty else Icons.Default.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

@Composable
fun AiQuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    )
}

@Composable
fun AiChatBubble(message: AiChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 260.dp),
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            // Render markdown-like content
            AiMessageContent(
                content = message.content,
                isUser = isUser,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
fun AiMessageContent(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    // Simple rendering: detect code blocks and render them differently
    val parts = parseMessageContent(content)
    Column(modifier = modifier) {
        parts.forEach { part ->
            when (part) {
                is MessagePart.Text -> Text(
                    text = part.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                is MessagePart.Code -> Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    color = Color(0xFF1E1E2E)
                ) {
                    Text(
                        text = part.code,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFE6E1F9)
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AiThinkingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 34.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(600, delayMillis = index * 200),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "alpha$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
fun AiWelcomeState(
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "AI Assistant",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Ask me anything about your code",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        val suggestions = listOf(
            "Create a RecyclerView adapter",
            "Explain this error",
            "Generate a Room database",
            "Write a Retrofit API interface",
            "Create a ViewModel with StateFlow"
        )
        suggestions.forEach { suggestion ->
            OutlinedCard(
                onClick = { onSuggestionClick(suggestion) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Text(
                    suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Message Parsing ─────────────────────────────────────────────────────────

sealed class MessagePart {
    data class Text(val text: String) : MessagePart()
    data class Code(val code: String, val language: String = "") : MessagePart()
}

fun parseMessageContent(content: String): List<MessagePart> {
    val parts = mutableListOf<MessagePart>()
    val codeBlockRegex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
    var lastEnd = 0

    codeBlockRegex.findAll(content).forEach { match ->
        if (match.range.first > lastEnd) {
            val text = content.substring(lastEnd, match.range.first).trim()
            if (text.isNotEmpty()) parts.add(MessagePart.Text(text))
        }
        parts.add(MessagePart.Code(match.groupValues[2].trim(), match.groupValues[1]))
        lastEnd = match.range.last + 1
    }

    if (lastEnd < content.length) {
        val text = content.substring(lastEnd).trim()
        if (text.isNotEmpty()) parts.add(MessagePart.Text(text))
    }

    return if (parts.isEmpty()) listOf(MessagePart.Text(content)) else parts
}
