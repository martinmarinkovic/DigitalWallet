package com.threemdroid.digitalwallet.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.threemdroid.digitalwallet.feature.addcard.editCardScreen
import com.threemdroid.digitalwallet.feature.categorydetails.CategoryDetailsRoute
import com.threemdroid.digitalwallet.feature.categorydetails.CategoryDetailsRoutes
import com.threemdroid.digitalwallet.feature.carddetails.CardDetailsRoute
import com.threemdroid.digitalwallet.feature.carddetails.CardDetailsRoutes
import com.threemdroid.digitalwallet.feature.fullscreencode.FullscreenCodeRoute
import com.threemdroid.digitalwallet.feature.fullscreencode.FullscreenCodeRoutes

object HomeRoutes {
    const val home = "home"
    const val createCategory = "home/create-category"
}

fun NavGraphBuilder.homeGraph(
    onOpenCategoryDetails: (String) -> Unit,
    onOpenCreateCategory: () -> Unit,
    onOpenAddCard: (String?) -> Unit,
    onOpenCardDetails: (String) -> Unit,
    onOpenEditCard: (String) -> Unit,
    onOpenFullscreenCode: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(route = HomeRoutes.home) {
        HomeRoute(
            onOpenCategoryDetails = onOpenCategoryDetails,
            onOpenCreateCategory = onOpenCreateCategory,
            onOpenCardDetails = onOpenCardDetails
        )
    }

    dialog(route = HomeRoutes.createCategory) {
        CreateCategoryDialogRoute(onDismiss = onNavigateBack)
    }

    composable(
        route = CategoryDetailsRoutes.categoryDetails,
        arguments = listOf(
            navArgument(CategoryDetailsRoutes.categoryIdArg) {
                type = NavType.StringType
            }
        )
    ) {
        CategoryDetailsRoute(
            onNavigateBack = onNavigateBack,
            onOpenAddCard = onOpenAddCard,
            onOpenCardDetails = onOpenCardDetails
        )
    }

    composable(
        route = CardDetailsRoutes.cardDetails,
        arguments = listOf(
            navArgument(CardDetailsRoutes.cardIdArg) {
                type = NavType.StringType
            }
        )
    ) {
        CardDetailsRoute(
            onNavigateBack = onNavigateBack,
            onOpenEdit = onOpenEditCard,
            onOpenFullscreenCode = onOpenFullscreenCode
        )
    }

    editCardScreen(onNavigateBack = onNavigateBack)

    composable(
        route = FullscreenCodeRoutes.fullscreenCode,
        arguments = listOf(
            navArgument(FullscreenCodeRoutes.cardIdArg) {
                type = NavType.StringType
            }
        )
    ) {
        FullscreenCodeRoute(
            onNavigateBack = onNavigateBack
        )
    }
}
