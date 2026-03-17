package com.threemdroid.digitalwallet.feature.addcard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.navigation.encodeRouteValue
import com.threemdroid.digitalwallet.core.navigation.TopLevelDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object AddCardRoutes {
    const val categoryIdArg = "categoryId"
    const val chooserFromCategory = "add_card/category/{$categoryIdArg}"

    fun chooserFromCategory(categoryId: String): String =
        "add_card/category/${encodeRouteValue(categoryId)}"
}

enum class AddCardMethod(
    val titleRes: Int
) {
    SCAN_BARCODE_QR(
        titleRes = R.string.add_card_method_scan_barcode_qr
    ),
    SCAN_CARD_PHOTO(
        titleRes = R.string.add_card_method_scan_card_photo
    ),
    MANUAL_ENTRY(
        titleRes = R.string.add_card_method_manual_entry
    ),
    SMART_SCANNING(
        titleRes = R.string.add_card_method_smart_scanning
    ),
    IMPORT_GOOGLE_WALLET(
        titleRes = R.string.add_card_method_import_google_wallet
    );

    fun destinationRoute(preselectedCategoryId: String?): String =
        when (this) {
            SCAN_BARCODE_QR -> ScanBarcodeRoutes.scan(preselectedCategoryId)
            SCAN_CARD_PHOTO -> PhotoScanRoutes.photoScan(preselectedCategoryId)
            MANUAL_ENTRY -> ManualEntryRoutes.manualEntry(preselectedCategoryId)
            SMART_SCANNING -> SmartScanRoutes.smartScan(preselectedCategoryId)
            IMPORT_GOOGLE_WALLET -> GoogleWalletImportRoutes.googleWalletImport(preselectedCategoryId)
        }
}

data class AddCardUiState(
    val titleRes: Int = R.string.nav_add_card,
    val methods: List<AddCardMethodUiModel> = AddCardMethod.entries.map { method ->
        AddCardMethodUiModel(
            method = method,
            titleRes = method.titleRes
        )
    }
)

data class AddCardMethodUiModel(
    val method: AddCardMethod,
    val titleRes: Int
)

sealed interface AddCardEvent {
    data object OnBackClicked : AddCardEvent

    data class OnMethodClicked(val method: AddCardMethod) : AddCardEvent
}

sealed interface AddCardEffect {
    data object NavigateBack : AddCardEffect

    data class OpenMethod(val route: String) : AddCardEffect
}

@HiltViewModel
class AddCardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val preselectedCategoryId: String? = savedStateHandle[AddCardRoutes.categoryIdArg]

    private val mutableUiState = MutableStateFlow(AddCardUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<AddCardEffect>()
    val effects = mutableEffects.asSharedFlow()

    fun onEvent(event: AddCardEvent) {
        when (event) {
            AddCardEvent.OnBackClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(AddCardEffect.NavigateBack)
                }
            }

            is AddCardEvent.OnMethodClicked -> {
                viewModelScope.launch {
                    mutableEffects.emit(
                        AddCardEffect.OpenMethod(
                            event.method.destinationRoute(preselectedCategoryId)
                        )
                    )
                }
            }
        }
    }
}

fun NavGraphBuilder.addCardGraph(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onCardSaved: (String) -> Unit
) {
    composable(route = TopLevelDestination.ADD_CARD.startRoute) {
        AddCardRoute(
            showBackButton = false,
            onNavigateBack = onNavigateBack,
            onNavigateToRoute = onNavigateToRoute
        )
    }

    composable(
        route = AddCardRoutes.chooserFromCategory,
        arguments = listOf(
            navArgument(AddCardRoutes.categoryIdArg) {
                type = NavType.StringType
            }
        )
    ) {
        AddCardRoute(
            showBackButton = true,
            onNavigateBack = onNavigateBack,
            onNavigateToRoute = onNavigateToRoute
        )
    }

    manualEntryScreen(
        onNavigateBack = onNavigateBack,
        onCardSaved = onCardSaved
    )

    scanBarcodeScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToConfirmation = onNavigateToRoute
    )

    photoScanScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToConfirmation = onNavigateToRoute
    )

    smartScanScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToConfirmation = onNavigateToRoute
    )

    googleWalletImportScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToConfirmation = onNavigateToRoute
    )
}

@Composable
private fun AddCardRoute(
    showBackButton: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    viewModel: AddCardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                AddCardEffect.NavigateBack -> onNavigateBack()
                is AddCardEffect.OpenMethod -> onNavigateToRoute(effect.route)
            }
        }
    }

    AddCardScreen(
        uiState = uiState,
        showBackButton = showBackButton,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddCardScreen(
    uiState: AddCardUiState,
    showBackButton: Boolean,
    onEvent: (AddCardEvent) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = uiState.titleRes)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { onEvent(AddCardEvent.OnBackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigate_back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.methods,
                    key = { method -> method.method.name }
                ) { method ->
                    AddCardMethodItem(
                        title = stringResource(id = method.titleRes),
                        onClick = {
                            onEvent(AddCardEvent.OnMethodClicked(method.method))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCardMethodItem(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
