package com.threemdroid.digitalwallet.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.threemdroid.digitalwallet.core.navigation.DigitalWalletNavHost
import com.threemdroid.digitalwallet.core.navigation.TopLevelDestination
import com.threemdroid.digitalwallet.feature.carddetails.CardDetailsRoutes

@Composable
fun DigitalWalletApp(
    reminderCardId: String? = null,
    onReminderCardIdHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val reminderLaunchViewModel: ReminderLaunchViewModel = hiltViewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val topLevelStartRoutes = TopLevelDestination.entries
        .map { destination -> destination.startRoute }
        .toSet()
    val isBottomBarVisible = currentDestination
        ?.hierarchy
        ?.any { destination -> destination.route in topLevelStartRoutes } == true

    LaunchedEffect(reminderCardId) {
        val pendingCardId = reminderCardId ?: return@LaunchedEffect
        when (val destination = reminderLaunchViewModel.resolveReminderDestination(pendingCardId)) {
            ReminderLaunchDestination.Home -> {
                navController.navigateToTopLevelDestination(TopLevelDestination.HOME)
            }

            is ReminderLaunchDestination.CardDetails -> {
                navController.navigate(CardDetailsRoutes.cardDetails(destination.cardId)) {
                    launchSingleTop = true
                }
            }
        }
        onReminderCardIdHandled()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { navDestination -> navDestination.route == destination.graphRoute } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigateToTopLevelDestination(destination)
                            },
                            icon = { destination.Icon(selected = selected) },
                            label = { Text(text = stringResource(id = destination.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        DigitalWalletNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}

private fun NavHostController.navigateToTopLevelDestination(destination: TopLevelDestination) {
    navigate(destination.graphRoute) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
