package com.threemdroid.digitalwallet.feature.addcard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType
import java.io.File
import kotlinx.coroutines.flow.collectLatest

fun NavGraphBuilder.smartScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit
) {
    composable(
        route = SmartScanRoutes.routePattern,
        arguments = listOf(
            navArgument(AddCardRoutes.categoryIdArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        SmartScanRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToConfirmation = onNavigateToConfirmation
        )
    }
}

@Composable
private fun SmartScanRoute(
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit,
    viewModel: SmartScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val scanner = remember(activity) {
        activity?.let { currentActivity ->
            GmsBarcodeScanning.getClient(
                currentActivity,
                GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_PDF417,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_ITF
                    )
                    .build()
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { didCapturePhoto ->
        val imageUri = pendingCaptureUri
        pendingCaptureUri = null
        if (didCapturePhoto && imageUri != null) {
            viewModel.onEvent(SmartScanEvent.OnImageSelected(imageUri))
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        if (imageUri != null) {
            viewModel.onEvent(SmartScanEvent.OnImageSelected(imageUri))
        }
    }

    LaunchedEffect(viewModel, scanner) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                SmartScanEffect.NavigateBack -> onNavigateBack()
                SmartScanEffect.LaunchCameraCapture -> {
                    runCatching {
                        val captureUri = context.createSmartScanImageUri()
                        pendingCaptureUri = captureUri
                        cameraLauncher.launch(captureUri)
                    }.onFailure {
                        pendingCaptureUri = null
                        viewModel.onEvent(SmartScanEvent.OnLaunchFailed)
                    }
                }

                SmartScanEffect.LaunchImagePicker -> {
                    runCatching {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }.onFailure {
                        viewModel.onEvent(SmartScanEvent.OnLaunchFailed)
                    }
                }

                SmartScanEffect.LaunchScanner -> {
                    val barcodeScanner = scanner
                    if (barcodeScanner == null) {
                        viewModel.onEvent(SmartScanEvent.OnLaunchFailed)
                    } else {
                        runCatching {
                            barcodeScanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val codeValue = barcode.rawValue
                                        ?.takeIf { value -> value.isNotBlank() }
                                        ?: barcode.displayValue?.takeIf { value -> value.isNotBlank() }
                                    if (codeValue == null) {
                                        viewModel.onEvent(SmartScanEvent.OnBarcodeScanFailed)
                                    } else {
                                        viewModel.onEvent(
                                            SmartScanEvent.OnBarcodeScanSucceeded(
                                                codeType = barcode.format.toCardCodeType(),
                                                codeValue = codeValue
                                            )
                                        )
                                    }
                                }
                                .addOnCanceledListener {
                                    viewModel.onEvent(SmartScanEvent.OnBarcodeScanCancelled)
                                }
                                .addOnFailureListener {
                                    viewModel.onEvent(SmartScanEvent.OnBarcodeScanFailed)
                                }
                        }.onFailure {
                            viewModel.onEvent(SmartScanEvent.OnLaunchFailed)
                        }
                    }
                }

                is SmartScanEffect.OpenConfirmation -> onNavigateToConfirmation(effect.route)
            }
        }
    }

    SmartScanScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SmartScanScreen(
    uiState: SmartScanUiState,
    onEvent: (SmartScanEvent) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.add_card_method_smart_scanning)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SmartScanEvent.OnBackClicked) }) {
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
                            onClick = { onEvent(SmartScanEvent.OnScanCodeClicked) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp)
                        ) {
                            Text(text = stringResource(id = R.string.smart_scan_scan_live_code))
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { onEvent(SmartScanEvent.OnTakePhotoClicked) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(text = stringResource(id = R.string.scan_card_photo_take_photo))
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { onEvent(SmartScanEvent.OnChooseImageClicked) },
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

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Int.toCardCodeType(): CardCodeType =
    when (this) {
        Barcode.FORMAT_QR_CODE -> CardCodeType.QR_CODE
        Barcode.FORMAT_AZTEC -> CardCodeType.AZTEC
        Barcode.FORMAT_PDF417 -> CardCodeType.PDF_417
        Barcode.FORMAT_CODE_128 -> CardCodeType.CODE_128
        Barcode.FORMAT_CODE_39 -> CardCodeType.CODE_39
        Barcode.FORMAT_EAN_13 -> CardCodeType.EAN_13
        Barcode.FORMAT_EAN_8 -> CardCodeType.EAN_8
        Barcode.FORMAT_UPC_A -> CardCodeType.UPC_A
        Barcode.FORMAT_UPC_E -> CardCodeType.UPC_E
        Barcode.FORMAT_ITF -> CardCodeType.ITF
        else -> CardCodeType.OTHER
    }

private fun Context.createSmartScanImageUri(): Uri {
    val imageDirectory = File(cacheDir, "images").apply {
        mkdirs()
    }
    val photoFile = File.createTempFile(
        "smart_scan_",
        ".jpg",
        imageDirectory
    )

    return FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        photoFile
    )
}
