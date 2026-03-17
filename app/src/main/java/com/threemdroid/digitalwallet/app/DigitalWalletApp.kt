package com.threemdroid.digitalwallet.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isBottomBarVisible) {
                WalletBottomBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        navController.navigateToTopLevelDestination(destination)
                    }
                )
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

@Composable
private fun WalletBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    onDestinationSelected: (TopLevelDestination) -> Unit
) {
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WalletBottomBarItem(
                    destination = TopLevelDestination.HOME,
                    selected = currentDestination
                        ?.hierarchy
                        ?.any { destination -> destination.route == TopLevelDestination.HOME.graphRoute } == true,
                    modifier = Modifier.width(72.dp),
                    onClick = onDestinationSelected
                )

                WalletAddAction(
                    onClick = {
                        onDestinationSelected(TopLevelDestination.ADD_CARD)
                    }
                )

                WalletBottomBarItem(
                    destination = TopLevelDestination.SETTINGS,
                    selected = currentDestination
                        ?.hierarchy
                        ?.any { destination -> destination.route == TopLevelDestination.SETTINGS.graphRoute } == true,
                    modifier = Modifier.width(72.dp),
                    onClick = onDestinationSelected
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun WalletBottomBarItem(
    destination: TopLevelDestination,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (TopLevelDestination) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    val isDarkChrome = colorScheme.surface.red < 0.3f
    val iconTint =
        if (selected) {
            colorScheme.primary
        } else {
            if (isDarkChrome) Color.White else colorScheme.onSurface
        }

    Box(
        modifier = modifier
            .height(80.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick(destination)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = destination.iconVector(selected = selected),
            contentDescription = stringResource(id = destination.labelRes),
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun WalletAddAction(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = colorScheme.primary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = TopLevelDestination.ADD_CARD.labelRes),
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
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
