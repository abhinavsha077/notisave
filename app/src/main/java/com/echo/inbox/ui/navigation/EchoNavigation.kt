package com.echo.inbox.ui.navigation

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.echo.inbox.di.AppContainer
import com.echo.inbox.ui.screens.AppDetailScreen
import com.echo.inbox.ui.screens.ConversationScreen
import com.echo.inbox.ui.screens.HealthScreen
import com.echo.inbox.ui.screens.HomeScreen
import com.echo.inbox.ui.screens.PrivacyScreen
import com.echo.inbox.ui.screens.SearchScreen
import com.echo.inbox.ui.screens.SettingsScreen
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.AppDetailViewModel
import com.echo.inbox.ui.viewmodel.ConversationViewModel
import com.echo.inbox.ui.viewmodel.HealthViewModel
import com.echo.inbox.ui.viewmodel.HomeViewModel
import com.echo.inbox.ui.viewmodel.PrivacyViewModel
import com.echo.inbox.ui.viewmodel.SearchViewModel
import com.echo.inbox.ui.viewmodel.SettingsViewModel
import com.echo.inbox.ui.viewmodel.viewModelFactory

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val PRIVACY = "privacy"
    const val HEALTH = "health"
    const val SETTINGS = "settings"
    const val APP = "app/{packageName}"
    const val CONVERSATION = "conversation/{conversationId}"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, "Inbox", Icons.Outlined.Inbox),
    BottomDestination(Routes.SEARCH, "Search", Icons.Outlined.Search),
    BottomDestination(Routes.PRIVACY, "Privacy", Icons.Outlined.PrivacyTip),
    BottomDestination(Routes.HEALTH, "Health", Icons.Outlined.HealthAndSafety),
)

@Composable
fun EchoNavigation(
    container: AppContainer,
    openNotificationAccessSettings: () -> Unit,
    openAppSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomDestinations.map { it.route }

    Scaffold(
        containerColor = EchoTheme.colors.background,
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = EchoTheme.colors.surface,
                    border = BorderStroke(1.dp, EchoTheme.colors.border),
                ) {
                    NavigationBar(
                        containerColor = EchoTheme.colors.surface,
                        contentColor = EchoTheme.colors.primaryText,
                    ) {
                        bottomDestinations.forEach { destination ->
                            val isSelected = currentRoute == destination.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EchoTheme.colors.onMintContainer,
                                    selectedTextColor = EchoTheme.colors.primaryText,
                                    indicatorColor = EchoTheme.colors.mintContainer,
                                    unselectedIconColor = EchoTheme.colors.secondaryText,
                                    unselectedTextColor = EchoTheme.colors.secondaryText,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(outerPadding),
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        HomeViewModel(
                            container.notificationRepository,
                            container.captureHealthRepository,
                            container.notificationAccessManager,
                        )
                    },
                )
                HomeScreen(
                    viewModel = vm,
                    onConversationClick = { navController.navigate("conversation/$it") },
                    onAppClick = { packageName -> navController.navigate("app/${Uri.encode(packageName)}") },
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    openNotificationAccessSettings = openNotificationAccessSettings,
                )
            }
            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(
                    factory = viewModelFactory { SearchViewModel(container.notificationRepository) },
                )
                SearchScreen(vm) { id -> navController.navigate("conversation/$id") }
            }
            composable(Routes.PRIVACY) {
                val vm: PrivacyViewModel = viewModel(
                    factory = viewModelFactory {
                        PrivacyViewModel(
                            container.settingsRepository,
                            container.appSourceRepository,
                            container.notificationRepository,
                            container.jsonExportWriter,
                            container.deviceAuthenticationCapability,
                        )
                    },
                )
                PrivacyScreen(vm)
            }
            composable(Routes.HEALTH) {
                val vm: HealthViewModel = viewModel(
                    factory = viewModelFactory {
                        HealthViewModel(
                            container.captureHealthRepository,
                            container.notificationAccessManager,
                            container.notificationTestManager,
                        )
                    },
                )
                HealthScreen(vm, openNotificationAccessSettings, openAppSettings)
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = viewModelFactory { SettingsViewModel(container.settingsRepository) },
                )
                SettingsScreen(vm, onBack = navController::popBackStack)
            }
            composable(
                route = Routes.APP,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
            ) { entry ->
                val packageName = Uri.decode(requireNotNull(entry.arguments?.getString("packageName")))
                val vm: AppDetailViewModel = viewModel(
                    key = "app-$packageName",
                    factory = viewModelFactory { AppDetailViewModel(packageName, container.notificationRepository) },
                )
                AppDetailScreen(
                    viewModel = vm,
                    onBack = navController::popBackStack,
                    onConversationClick = { navController.navigate("conversation/$it") },
                )
            }
            composable(
                route = Routes.CONVERSATION,
                arguments = listOf(navArgument("conversationId") { type = NavType.LongType }),
            ) { entry ->
                val id = requireNotNull(entry.arguments?.getLong("conversationId"))
                val vm: ConversationViewModel = viewModel(
                    key = "conversation-$id",
                    factory = viewModelFactory { ConversationViewModel(id, container.notificationRepository) },
                )
                ConversationScreen(vm, onBack = navController::popBackStack)
            }
        }
    }
}
