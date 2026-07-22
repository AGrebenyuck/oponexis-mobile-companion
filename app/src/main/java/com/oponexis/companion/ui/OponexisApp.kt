package com.oponexis.companion.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oponexis.companion.domain.model.ThemePreference
import com.oponexis.companion.ui.navigation.Destination
import com.oponexis.companion.ui.screen.calls.CallsRoute
import com.oponexis.companion.ui.screen.dashboard.DashboardRoute
import com.oponexis.companion.ui.screen.diagnostics.DiagnosticsRoute
import com.oponexis.companion.ui.screen.onboarding.OnboardingRoute
import com.oponexis.companion.ui.screen.settings.SettingsRoute
import com.oponexis.companion.ui.screen.splash.SplashScreen
import com.oponexis.companion.ui.theme.OponexisTheme

@Composable
fun OponexisApp(viewModel: AppViewModel = hiltViewModel()) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val darkTheme = when (preferences?.themePreference) {
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
        ThemePreference.System, null -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    OponexisTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val showBottomBar = Destination.bottomBarItems.any { item ->
            currentDestination?.hierarchy?.any { it.route == item.route } == true
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        Destination.bottomBarItems.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(Destination.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = requireNotNull(destination.icon),
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { contentPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.Splash.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Destination.Splash.route) {
                    SplashScreen(
                        preferences = preferences,
                        onFinished = { onboardingComplete ->
                            val destination = if (onboardingComplete) Destination.Dashboard.route else Destination.Onboarding.route
                            navController.navigate(destination) {
                                popUpTo(Destination.Splash.route) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Destination.Onboarding.route) {
                    OnboardingRoute(
                        onComplete = {
                            navController.navigate(Destination.Dashboard.route) {
                                popUpTo(Destination.Onboarding.route) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Destination.Dashboard.route) { DashboardRoute(contentPadding) }
                composable(Destination.Calls.route) { CallsRoute(contentPadding) }
                composable(Destination.Diagnostics.route) { DiagnosticsRoute(contentPadding) }
                composable(Destination.Settings.route) { SettingsRoute(contentPadding) }
            }
        }
    }
}
