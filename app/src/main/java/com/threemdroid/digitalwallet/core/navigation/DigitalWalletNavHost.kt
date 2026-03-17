package com.threemdroid.digitalwallet.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navigation
import com.threemdroid.digitalwallet.feature.addcard.AddCardRoutes
import com.threemdroid.digitalwallet.feature.addcard.addCardGraph
import com.threemdroid.digitalwallet.feature.categorydetails.CategoryDetailsRoutes
import com.threemdroid.digitalwallet.feature.carddetails.CardDetailsRoutes
import com.threemdroid.digitalwallet.feature.carddetails.EditCardRoutes
import com.threemdroid.digitalwallet.feature.fullscreencode.FullscreenCodeRoutes
import com.threemdroid.digitalwallet.feature.home.HomeRoutes
import com.threemdroid.digitalwallet.feature.home.homeGraph
import com.threemdroid.digitalwallet.feature.settings.SettingsLegalRoutes
import com.threemdroid.digitalwallet.feature.settings.settingsPrivacyPolicyScreen
import com.threemdroid.digitalwallet.feature.settings.settingsScreen
import com.threemdroid.digitalwallet.feature.settings.settingsTermsScreen

@Composable
fun DigitalWalletNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.graphRoute,
        modifier = modifier
    ) {
        navigation(
            startDestination = TopLevelDestination.HOME.startRoute,
            route = TopLevelDestination.HOME.graphRoute
        ) {
            homeGraph(
                onOpenCategoryDetails = { categoryId ->
                    navController.navigate(CategoryDetailsRoutes.categoryDetails(categoryId))
                },
                onOpenCreateCategory = {
                    navController.navigate(HomeRoutes.createCategory)
                },
                onOpenAddCard = { categoryId ->
                    val route =
                        categoryId?.let(AddCardRoutes::chooserFromCategory)
                            ?: TopLevelDestination.ADD_CARD.startRoute
                    navController.navigate(route)
                },
                onOpenCardDetails = { cardId ->
                    navController.navigate(CardDetailsRoutes.cardDetails(cardId))
                },
                onOpenEditCard = { cardId ->
                    navController.navigate(EditCardRoutes.editCard(cardId))
                },
                onOpenFullscreenCode = { cardId ->
                    navController.navigate(FullscreenCodeRoutes.fullscreenCode(cardId))
                },
                onNavigateBack = navController::navigateUp
            )
        }

        navigation(
            startDestination = TopLevelDestination.ADD_CARD.startRoute,
            route = TopLevelDestination.ADD_CARD.graphRoute
        ) {
            addCardGraph(
                onNavigateBack = navController::navigateUp,
                onNavigateToRoute = { route ->
                    navController.navigate(route)
                },
                onCardSaved = { categoryId ->
                    val categoryDetailsRoute = CategoryDetailsRoutes.categoryDetails(categoryId)
                    val returnedToExistingCategory = navController.popBackStack(
                        categoryDetailsRoute,
                        inclusive = false
                    )
                    if (!returnedToExistingCategory) {
                        navController.navigate(categoryDetailsRoute) {
                            popUpTo(TopLevelDestination.ADD_CARD.graphRoute) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        navigation(
            startDestination = TopLevelDestination.SETTINGS.startRoute,
            route = TopLevelDestination.SETTINGS.graphRoute
        ) {
            settingsScreen(
                onOpenPrivacyPolicy = {
                    navController.navigate(SettingsLegalRoutes.privacyPolicy)
                },
                onOpenTerms = {
                    navController.navigate(SettingsLegalRoutes.terms)
                }
            )
            settingsPrivacyPolicyScreen(onNavigateBack = navController::navigateUp)
            settingsTermsScreen(onNavigateBack = navController::navigateUp)
        }
    }
}
