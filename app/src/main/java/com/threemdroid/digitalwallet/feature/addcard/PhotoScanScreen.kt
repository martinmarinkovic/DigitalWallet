package com.threemdroid.digitalwallet.feature.addcard

import android.content.Context
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.threemdroid.digitalwallet.R
import java.io.File
import kotlinx.coroutines.flow.collectLatest

fun NavGraphBuilder.photoScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit
) {
    composable(
        route = PhotoScanRoutes.routePattern,
        arguments = listOf(
            navArgument(AddCardRoutes.categoryIdArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(PhotoScanRoutes.launchActionArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        PhotoScanRoute(
            launchAction = PhotoScanLaunchAction.fromRouteValue(
                backStackEntry.arguments?.getString(PhotoScanRoutes.launchActionArg)
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToConfirmation = onNavigateToConfirmation
        )
    }
}

@Composable
private fun PhotoScanRoute(
    launchAction: PhotoScanLaunchAction?,
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit,
    viewModel: PhotoScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { didCapturePhoto ->
        val imageUri = pendingCaptureUri
        pendingCaptureUri = null
        if (didCapturePhoto && imageUri != null) {
            viewModel.onEvent(PhotoScanEvent.OnImageSelected(imageUri))
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        if (imageUri != null) {
            viewModel.onEvent(PhotoScanEvent.OnImageSelected(imageUri))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                PhotoScanEffect.NavigateBack -> onNavigateBack()
                PhotoScanEffect.LaunchCameraCapture -> {
                    runCatching {
                        val captureUri = context.createPhotoScanImageUri()
                        pendingCaptureUri = captureUri
                        cameraLauncher.launch(captureUri)
                    }.onFailure {
                        pendingCaptureUri = null
                        viewModel.onEvent(PhotoScanEvent.OnLaunchFailed)
                    }
                }

                PhotoScanEffect.LaunchImagePicker -> {
                    runCatching {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }.onFailure {
                        viewModel.onEvent(PhotoScanEvent.OnLaunchFailed)
                    }
                }

                is PhotoScanEffect.OpenConfirmation -> onNavigateToConfirmation(effect.route)
            }
        }
    }

    LaunchedEffect(launchAction) {
        when (launchAction) {
            PhotoScanLaunchAction.TAKE_PHOTO -> {
                viewModel.onEvent(PhotoScanEvent.OnTakePhotoClicked)
            }

            PhotoScanLaunchAction.CHOOSE_IMAGE -> {
                viewModel.onEvent(PhotoScanEvent.OnChooseImageClicked)
            }

            null -> Unit
        }
    }

    PhotoScanScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PhotoScanScreen(
    uiState: PhotoScanUiState,
    onEvent: (PhotoScanEvent) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.add_card_method_scan_card_photo)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(PhotoScanEvent.OnBackClicked) }) {
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
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.status.showProgress) {
                            CircularProgressIndicator()
                        }
                        Text(
                            text = stringResource(id = uiState.status.titleRes),
                            modifier = Modifier.padding(top = if (uiState.status.showProgress) 20.dp else 0.dp),
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

                if (uiState.status.showActions) {
                    item {
                        Button(
                            onClick = { onEvent(PhotoScanEvent.OnTakePhotoClicked) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp)
                        ) {
                            Text(text = stringResource(id = R.string.scan_card_photo_take_photo))
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { onEvent(PhotoScanEvent.OnChooseImageClicked) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(text = stringResource(id = R.string.scan_card_photo_choose_image))
                        }
                    }
                }
            }
        }
    }
}

private fun Context.createPhotoScanImageUri(): Uri {
    val imageDirectory = File(cacheDir, "images").apply {
        mkdirs()
    }
    val photoFile = File.createTempFile(
        "photo_scan_",
        ".jpg",
        imageDirectory
    )

    return FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        photoFile
    )
}
