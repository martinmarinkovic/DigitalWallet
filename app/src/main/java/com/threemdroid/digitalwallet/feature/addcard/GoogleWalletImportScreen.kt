package com.threemdroid.digitalwallet.feature.addcard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.threemdroid.digitalwallet.R
import kotlinx.coroutines.flow.collectLatest

fun NavGraphBuilder.googleWalletImportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit
) {
    composable(
        route = GoogleWalletImportRoutes.routePattern,
        arguments = listOf(
            navArgument(AddCardRoutes.categoryIdArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        GoogleWalletImportRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToConfirmation = onNavigateToConfirmation
        )
    }
}

@Composable
private fun GoogleWalletImportRoute(
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit,
    viewModel: GoogleWalletImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { imageUri: Uri? ->
        if (imageUri != null) {
            viewModel.onEvent(GoogleWalletImportEvent.OnImageSelected(imageUri))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                GoogleWalletImportEffect.NavigateBack -> onNavigateBack()
                GoogleWalletImportEffect.LaunchImagePicker -> {
                    runCatching {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }.onFailure {
                        viewModel.onEvent(GoogleWalletImportEvent.OnLaunchFailed)
                    }
                }

                is GoogleWalletImportEffect.OpenConfirmation -> onNavigateToConfirmation(effect.route)
            }
        }
    }

    GoogleWalletImportScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GoogleWalletImportScreen(
    uiState: GoogleWalletImportUiState,
    onEvent: (GoogleWalletImportEvent) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.add_card_method_import_google_wallet)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(GoogleWalletImportEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back)
                        )
                    }
                }
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
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.status == GoogleWalletImportStatus.PROCESSING) {
                            CircularProgressIndicator()
                        }
                        Text(
                            text = stringResource(id = uiState.status.titleRes),
                            modifier = Modifier.padding(top = if (uiState.status == GoogleWalletImportStatus.PROCESSING) 20.dp else 0.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(id = uiState.status.messageRes),
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(id = R.string.google_wallet_import_supported_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Text(
                        text = stringResource(id = R.string.google_wallet_import_supported_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    Text(
                        text = stringResource(id = R.string.google_wallet_import_not_supported_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Text(
                        text = stringResource(id = R.string.google_wallet_import_not_supported_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.sharedTextInput,
                        onValueChange = { onEvent(GoogleWalletImportEvent.OnSharedTextChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.google_wallet_import_shared_text_label)) },
                        placeholder = { Text(text = stringResource(id = R.string.google_wallet_import_shared_text_placeholder)) },
                        enabled = uiState.status != GoogleWalletImportStatus.PROCESSING,
                        minLines = 4
                    )
                }

                item {
                    Button(
                        onClick = { onEvent(GoogleWalletImportEvent.OnImportTextClicked) },
                        enabled = uiState.sharedTextInput.isNotBlank() &&
                            uiState.status != GoogleWalletImportStatus.PROCESSING,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.google_wallet_import_shared_text_action))
                    }
                }

                item {
                    Button(
                        onClick = { onEvent(GoogleWalletImportEvent.OnChooseImageClicked) },
                        enabled = uiState.status != GoogleWalletImportStatus.PROCESSING,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.google_wallet_import_choose_image_action))
                    }
                }
            }
        }
    }
}
