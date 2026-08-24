package app.semblance.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.semblance.ui.fleet.FleetScreen
import app.semblance.ui.fleet.FleetViewModel
import app.semblance.ui.maximized.MaximizedScreen
import app.semblance.ui.maximized.MaximizedViewModel
import app.semblance.ui.pulse.PulseScreen
import app.semblance.ui.pulse.PulseViewModel
import app.semblance.ui.settings.SettingsScreen
import app.semblance.ui.settings.SettingsViewModel
import app.semblance.ui.splash.SplashScreen
import app.semblance.ui.tasks.TaskTraceScreen
import app.semblance.ui.tasks.TasksScreen
import app.semblance.ui.tasks.TasksViewModel
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.ConsoleBg
import app.semblance.ui.theme.ConsoleBorder
import app.semblance.ui.theme.ConsoleSurface
import app.semblance.ui.theme.TextMuted
import app.semblance.ui.theme.TextPrimary
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography
import app.semblance.ui.wizard.WizardScreen
import app.semblance.ui.wizard.WizardViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector?) {
    data object Splash : Screen("splash", "Splash", null)
    data object Fleet : Screen("fleet", "FLEET", Icons.Default.GridView)
    data object Tasks : Screen("tasks", "TASKS", Icons.Default.Assignment)
    data object Wizard : Screen("wizard", "PROFILES", Icons.Default.PersonAdd)
    data object Pulse : Screen("pulse", "PULSE", Icons.Default.ShowChart)
    data object Settings : Screen("settings", "SETTINGS", Icons.Default.Settings)
    data object Maximized : Screen("maximized/{profileId}", "MAXIMIZED", null) {
        fun createRoute(profileId: Int) = "maximized/$profileId"
    }
    data object TaskTrace : Screen("trace/{taskId}", "TRACE", null) {
        fun createRoute(taskId: String) = "trace/$taskId"
    }
}

val BottomNavItems = listOf(
    Screen.Fleet,
    Screen.Tasks,
    Screen.Wizard,
    Screen.Pulse,
    Screen.Settings
)

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in BottomNavItems.map { it.route }

    Scaffold(
        containerColor = ConsoleBg,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = ConsoleSurface,
                    modifier = Modifier.border(1.dp, ConsoleBorder)
                ) {
                    BottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    style = Typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = if (isSelected) AccentCyan else TextSecondary
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentCyan,
                                unselectedIconColor = TextSecondary,
                                indicatorColor = AccentCyan.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashComplete = {
                        navController.navigate(Screen.Fleet.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Fleet.route) {
                val viewModel: FleetViewModel = hiltViewModel()
                FleetScreen(
                    viewModel = viewModel,
                    onNavigateToMaximized = { id ->
                        viewModel.openInteractiveBrowser(id)
                    },
                    onNavigateToWizard = {
                        navController.navigate(Screen.Wizard.route)
                    }
                )
            }

            composable(Screen.Tasks.route) {
                val viewModel: TasksViewModel = hiltViewModel()
                TasksScreen(
                    viewModel = viewModel,
                    onNavigateToTrace = { taskId ->
                        navController.navigate(Screen.TaskTrace.createRoute(taskId))
                    }
                )
            }

            composable(Screen.Wizard.route) {
                val viewModel: WizardViewModel = hiltViewModel()
                WizardScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.navigate(Screen.Fleet.route) {
                            popUpTo(Screen.Fleet.route) { inclusive = true }
                        }
                    },
                    onProfileCreated = {
                        navController.navigate(Screen.Fleet.route) {
                            popUpTo(Screen.Fleet.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Pulse.route) {
                val viewModel: PulseViewModel = hiltViewModel()
                PulseScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = viewModel)
            }

            composable(
                route = Screen.Maximized.route,
                arguments = listOf(navArgument("profileId") { type = NavType.IntType })
            ) {
                val viewModel: MaximizedViewModel = hiltViewModel()
                MaximizedScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.TaskTrace.route,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                val viewModel: TasksViewModel = hiltViewModel()
                TaskTraceScreen(
                    taskId = taskId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
