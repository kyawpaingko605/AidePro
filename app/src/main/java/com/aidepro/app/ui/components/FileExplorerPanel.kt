package com.aidepro.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aidepro.app.viewmodel.EditorViewModel
import com.aidepro.app.viewmodel.FileNode
import java.io.File

@Composable
fun FileExplorerPanel(
    projectPath: String,
    onFileClick: (String) -> Unit,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val fileTree by viewModel.fileTree.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountTree,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Project",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.refreshFileTree() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.showNewFileDialog() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New file",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // File tree
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(fileTree, key = { it.path }) { node ->
                FileTreeNode(
                    node = node,
                    onFileClick = onFileClick,
                    onToggleExpand = { viewModel.toggleExpand(it) },
                    depth = 0
                )
            }
        }
    }
}

@Composable
fun FileTreeNode(
    node: FileNode,
    onFileClick: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    depth: Int
) {
    val indentPadding = (depth * 16 + 8).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isDirectory) {
                    onToggleExpand(node.path)
                } else {
                    onFileClick(node.path)
                }
            }
            .padding(start = indentPadding, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(if (node.isExpanded) 90f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))

        // File/folder icon with color coding
        val (icon, tint) = when {
            node.isDirectory -> Icons.Default.Folder to MaterialTheme.colorScheme.tertiary
            node.name.endsWith(".kt") -> Icons.Default.Code to Color(0xFF7C4DFF)
            node.name.endsWith(".java") -> Icons.Default.Coffee to Color(0xFFFF6D00)
            node.name.endsWith(".xml") -> Icons.Default.DataObject to Color(0xFF00BCD4)
            node.name.endsWith(".gradle") || node.name.endsWith(".kts") ->
                Icons.Default.Build to Color(0xFF4CAF50)
            node.name.endsWith(".json") -> Icons.Default.DataArray to Color(0xFFFFEB3B)
            node.name.endsWith(".md") -> Icons.Default.Description to MaterialTheme.colorScheme.secondary
            else -> Icons.Default.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = node.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }

    // Expanded children
    if (node.isDirectory && node.isExpanded) {
        AnimatedVisibility(
            visible = node.isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                node.children.forEach { child ->
                    FileTreeNode(
                        node = child,
                        onFileClick = onFileClick,
                        onToggleExpand = onToggleExpand,
                        depth = depth + 1
                    )
                }
            }
        }
    }
}

// Import Color for file icon tinting
import androidx.compose.ui.graphics.Color
