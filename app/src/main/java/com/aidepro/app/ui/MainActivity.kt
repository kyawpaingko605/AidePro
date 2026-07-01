package com.aidepro.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aidepro.app.ui.navigation.AideNavGraph
import com.aidepro.app.ui.theme.AideProTheme
import com.aidepro.app.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AideProApp()
        }
    }
}

@Composable
fun AideProApp() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    val useDynamicColor by settingsViewModel.useDynamicColor.collectAsState()

    AideProTheme(
        darkTheme = isDarkTheme,
        dynamicColor = useDynamicColor
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            AideNavGraph(navController = navController)
        }
    }
}
