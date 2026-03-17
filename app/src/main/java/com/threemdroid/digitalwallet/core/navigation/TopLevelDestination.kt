package com.threemdroid.digitalwallet.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.threemdroid.digitalwallet.R

enum class TopLevelDestination(
    val graphRoute: String,
    val startRoute: String,
    @param:StringRes @field:StringRes val labelRes: Int,
    private val selectedIcon: ImageVector,
    private val unselectedIcon: ImageVector
) {
    HOME(
        graphRoute = "home_graph",
        startRoute = "home",
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    ADD_CARD(
        graphRoute = "add_card_graph",
        startRoute = "add_card",
        labelRes = R.string.nav_add_card,
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline
    ),
    SETTINGS(
        graphRoute = "settings_graph",
        startRoute = "settings",
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    );

    fun iconVector(selected: Boolean): ImageVector =
        if (selected) selectedIcon else unselectedIcon
}
