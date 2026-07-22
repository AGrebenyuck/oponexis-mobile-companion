package com.oponexis.companion.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Troubleshoot
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
) {
    data object Splash : Destination("splash", "Splash")
    data object Onboarding : Destination("onboarding", "Onboarding")
    data object Dashboard : Destination("dashboard", "Home", Icons.Rounded.GridView)
    data object Calls : Destination("calls", "Calls", Icons.Rounded.Call)
    data object Diagnostics : Destination("diagnostics", "Diagnostics", Icons.Rounded.Troubleshoot)
    data object Settings : Destination("settings", "Settings", Icons.Rounded.Settings)

    companion object {
        val bottomBarItems = listOf(Dashboard, Calls, Diagnostics, Settings)
    }
}

