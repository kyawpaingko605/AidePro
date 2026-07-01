package com.aidepro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidepro.app.ui.components.*
import com.aidepro.app.viewmodel.EditorViewModel
import com.aidepro.app.viewmodel.BuildState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectPath: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    LaunchedEffect(projectPath) {
        viewModel.loadProject(projectPath)
    }

    val uiState by viewModel.uiState.collectAsState()
    val buildState by viewModel.buildState.collectAsState()
    val openFiles by viewModel.openFiles.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()
    val isAiPanelOpen by viewModel.isAiPanelOpen.collectAsState()
    val isFileExplorerOpen by viewModel.isFileExplorerOpen.collectAsState()
    val isTerminalOpen by viewModel.isTerminalOpen.collectAsState()
    val buildOutput by viewModel.buildOutput.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Build status snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(buildState) {
        when (buildState) {
            is BuildState.Success -> snackbarHostState.showSnackbar(
                "✅ Build successful! APK ready.",
                duration = SnackbarDuration.Short
            )
            is BuildState.Error -> snackbarHostState.showSnackbar(
                "❌ Build failed. Check output.",
                duration = SnackbarDuration.Long
            )
            else -> {}
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                FileExplorerPanel(
                    projectPath = projectPath,
                    onFileClick = { path -> viewModel.openFile(path) },
                    viewModel = viewModel
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                EditorTopBar(
                    projectName = uiState.projectName,
                    buildState = buildState,
                    onBack = onBack,
                    onMenuClick = { /* toggle drawer */ },
                    onBuild = { viewModel.buildProject() },
                    onRun = { viewModel.runProject() },
                    onSettings = onSettings,
                    onToggleAi = { viewModel.toggleAiPanel() },
                    onToggleTerminal = { viewModel.toggleTerminal() }
                )
            },
            bottomBar = {
                EditorStatusBar(
                    activeFile = activeFile,
                    buildState = buildState,
                    cursorPosition = uiState.cursorLine to uiState.cursorColumn
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Open file tabs
                if (openFiles.isNotEmpty()) {
                    FileTabs(
                        openFiles = openFiles,
                        activeFile = activeFile,
                        onTabClick = { viewModel.switchToFile(it) },
                        onTabClose = { viewModel.closeFile(it) }
                    )
                }

                // Main content area
                Row(modifier = Modifier.weight(1f)) {
                    // File explorer sidebar (inline mode for tablets)
                    AnimatedVisibility(
                        visible = isFileExplorerOpen,
                        enter = slideInHorizontally() + fadeIn(),
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(260.dp)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            FileExplorerPanel(
                                projectPath = projectPath,
                                onFileClick = { path -> viewModel.openFile(path) },
                                viewModel = viewModel
                            )
                        }
                    }

                    // Code Editor
                    Box(modifier = Modifier.weight(1f)) {
                        if (activeFile != null) {
                            CodeEditorView(
                                filePath = activeFile!!,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            EditorEmptyState(
                                modifier = Modifier.fillMaxSize(),
                                onOpenFile = { /* open file picker */ }
                            )
                        }
                    }

                    // AI Panel
                    AnimatedVisibility(
                        visible = isAiPanelOpen,
                        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    ) {
                        AiAssistantPanel(
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight(),
                            viewModel = viewModel
                        )
                    }
                }

                // Terminal panel (bottom)
                AnimatedVisibility(
                    visible = isTerminalOpen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    TerminalPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        buildOutput = buildOutput,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    projectName: String,
    buildState: BuildState,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onBuild: () -> Unit,
    onRun: () -> Unit,
    onSettings: () -> Unit,
    onToggleAi: () -> Unit,
    onToggleTerminal: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                projectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // AI Assistant toggle
            IconButton(onClick = onToggleAi) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Terminal toggle
            IconButton(onClick = onToggleTerminal) {
                Icon(Icons.Default.Terminal, contentDescription = "Terminal")
            }

            // Build button
            when (buildState) {
                is BuildState.Building -> {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                else -> {
                    IconButton(onClick = onBuild) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Build",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Run button
            FilledIconButton(
                onClick = onRun,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Run",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            IconButton(onClick = onSettings) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun EditorStatusBar(
    activeFile: String?,
    buildState: BuildState,
    cursorPosition: Pair<Int, Int>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Build status indicator
            val (statusColor, statusText) = when (buildState) {
                is BuildState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant to "Ready"
                is BuildState.Building -> MaterialTheme.colorScheme.tertiary to "Building..."
                is BuildState.Success -> Color(0xFF4CAF50) to "Build OK"
                is BuildState.Error -> MaterialTheme.colorScheme.error to "Build Failed"
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )

            Spacer(modifier = Modifier.weight(1f))

            // File name
            activeFile?.let {
                Text(
                    it.substringAfterLast("/"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Cursor position
            Text(
                "Ln ${cursorPosition.first}, Col ${cursorPosition.second}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FileTabs(
    openFiles: List<String>,
    activeFile: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = openFiles.indexOf(activeFile).coerceAtLeast(0),
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        divider = {}
    ) {
        openFiles.forEach { filePath ->
            val isActive = filePath == activeFile
            Tab(
                selected = isActive,
                onClick = { onTabClick(filePath) },
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    // File type icon
                    val icon = when {
                        filePath.endsWith(".kt") -> Icons.Default.Code
                        filePath.endsWith(".java") -> Icons.Default.Coffee
                        filePath.endsWith(".xml") -> Icons.Default.DataObject
                        filePath.endsWith(".gradle") || filePath.endsWith(".kts") -> Icons.Default.Settings
                        else -> Icons.Default.InsertDriveFile
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        filePath.substringAfterLast("/"),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onTabClose(filePath) },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorEmptyState(
    modifier: Modifier = Modifier,
    onOpenFile: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Code,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No file open",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            "Select a file from the explorer",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}
