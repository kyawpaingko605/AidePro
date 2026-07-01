package com.aidepro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aidepro.app.ui.screens.EditorScreen
import com.aidepro.app.ui.screens.HomeScreen
import com.aidepro.app.ui.screens.NewProjectScreen
import com.aidepro.app.ui.screens.SettingsScreen
import com.aidepro.app.ui.screens.WelcomeScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object NewProject : Screen("new_project")
    object Editor : Screen("editor/{projectPath}") {
        fun createRoute(projectPath: String): String {
            val encoded = URLEncoder.encode(projectPath, "UTF-8")
            return "editor/$encoded"
        }
    }
    object Settings : Screen("settings")
}

@Composable
fun AideNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onOpenProject = { navController.navigate(Screen.Home.route) },
                onNewProject = { navController.navigate(Screen.NewProject.route) }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenProject = { path ->
                    navController.navigate(Screen.Editor.createRoute(path))
                },
                onNewProject = { navController.navigate(Screen.NewProject.route) },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.NewProject.route) {
            NewProjectScreen(
                onProjectCreated = { path ->
                    navController.navigate(Screen.Editor.createRoute(path)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("projectPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")
            EditorScreen(
                projectPath = projectPath,
                onBack = { navController.popBackStack() },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
