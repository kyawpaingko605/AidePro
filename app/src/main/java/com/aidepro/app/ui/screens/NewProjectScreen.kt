package com.aidepro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidepro.app.viewmodel.NewProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    onProjectCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: NewProjectViewModel = hiltViewModel()
) {
    var projectName by remember { mutableStateOf("MyApp") }
    var packageName by remember { mutableStateOf("com.example.myapp") }
    var selectedTemplate by remember { mutableStateOf(ProjectTemplate.EMPTY_ACTIVITY) }
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var minSdk by remember { mutableStateOf("21") }
    val isCreating by viewModel.isCreating.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Project", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Template Selection
            Text(
                "Choose Template",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ProjectTemplate.values()) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = selectedTemplate == template,
                        onClick = { selectedTemplate = template }
                    )
                }
            }

            HorizontalDivider()

            // Project Configuration
            Text(
                "Configure Project",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = projectName,
                onValueChange = {
                    projectName = it
                    packageName = "com.example.${it.lowercase().replace(" ", "")}"
                },
                label = { Text("Project Name") },
                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it },
                label = { Text("Package Name") },
                leadingIcon = { Icon(Icons.Default.Inventory2, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Language selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Kotlin", "Java").forEach { lang ->
                    FilterChip(
                        selected = selectedLanguage == lang,
                        onClick = { selectedLanguage = lang },
                        label = { Text(lang) },
                        leadingIcon = if (selectedLanguage == lang) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Min SDK selector
            ExposedDropdownMenuBox(
                expanded = false,
                onExpandedChange = {}
            ) {
                OutlinedTextField(
                    value = "API $minSdk (Android ${sdkToAndroidVersion(minSdk)})",
                    onValueChange = {},
                    label = { Text("Minimum SDK") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Android, null) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Create button
            Button(
                onClick = {
                    viewModel.createProject(
                        name = projectName,
                        packageName = packageName,
                        template = selectedTemplate,
                        language = selectedLanguage,
                        minSdk = minSdk.toIntOrNull() ?: 21,
                        onSuccess = onProjectCreated
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !isCreating && projectName.isNotBlank() && packageName.isNotBlank()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Icon(Icons.Default.CreateNewFolder, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Project", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCard(
    template: ProjectTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = template.icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                template.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class ProjectTemplate(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    EMPTY_ACTIVITY("Empty Activity", Icons.Default.CropSquare),
    COMPOSE_ACTIVITY("Compose Activity", Icons.Default.Layers),
    BOTTOM_NAV("Bottom Navigation", Icons.Default.BottomNavigation),
    DRAWER_NAV("Navigation Drawer", Icons.Default.Menu),
    SETTINGS("Settings Activity", Icons.Default.Settings),
    LOGIN("Login Activity", Icons.Default.Login),
    MAPS("Google Maps", Icons.Default.Map),
    LIBRARY("Android Library", Icons.Default.LibraryBooks)
}

private fun sdkToAndroidVersion(sdk: String): String = when (sdk) {
    "21" -> "5.0 Lollipop"
    "23" -> "6.0 Marshmallow"
    "24" -> "7.0 Nougat"
    "26" -> "8.0 Oreo"
    "28" -> "9.0 Pie"
    "29" -> "10"
    "30" -> "11"
    "31" -> "12"
    "33" -> "13"
    "34" -> "14"
    "35" -> "15"
    else -> sdk
}
