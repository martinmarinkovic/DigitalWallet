package com.threemdroid.digitalwallet.feature.addcard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.model.CardCodeType
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.collectLatest

fun NavGraphBuilder.scanBarcodeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit
) {
    composable(
        route = ScanBarcodeRoutes.routePattern,
        arguments = listOf(
            navArgument(AddCardRoutes.categoryIdArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        ScanBarcodeRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToConfirmation = onNavigateToConfirmation
        )
    }
}

@Composable
private fun ScanBarcodeRoute(
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (String) -> Unit,
    viewModel: ScanBarcodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnNavigateToConfirmation by rememberUpdatedState(onNavigateToConfirmation)

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val shouldShowRationale = activity?.let { currentActivity ->
            ActivityCompat.shouldShowRequestPermissionRationale(
                currentActivity,
                Manifest.permission.CAMERA
            )
        } == true
        viewModel.onEvent(
            ScanBarcodeEvent.OnPermissionRequestResult(
                granted = granted,
                shouldShowRationale = shouldShowRationale
            )
        )
    }

    LaunchedEffect(viewModel, context) {
        viewModel.onEvent(
            ScanBarcodeEvent.OnPermissionStateResolved(
                granted = context.hasCameraPermission()
            )
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                ScanBarcodeEffect.NavigateBack -> currentOnNavigateBack()
                is ScanBarcodeEffect.OpenConfirmation -> {
                    currentOnNavigateToConfirmation(effect.route)
                }
                ScanBarcodeEffect.RequestCameraPermission -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                ScanBarcodeEffect.OpenAppSettings -> {
                    openAppSettings(context)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(
                    ScanBarcodeEvent.OnPermissionStateResolved(
                        granted = context.hasCameraPermission()
                    )
                )
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ScanBarcodeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScanBarcodeScreen(
    uiState: ScanBarcodeUiState,
    onEvent: (ScanBarcodeEvent) -> Unit
) {
    val context = LocalContext.current
    val effectiveStatus =
        if (
            !context.hasCameraPermission() &&
                uiState.status == ScanBarcodeStatus.INITIALIZING
        ) {
            ScanBarcodeStatus.PERMISSION_REQUIRED
        } else {
            uiState.status
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.add_card_method_scan_barcode_qr)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ScanBarcodeEvent.OnBackClicked) }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (effectiveStatus.showScannerPreview) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .background(
                                color = Color.Black,
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ScanBarcodeCameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onInitialized = {
                                onEvent(ScanBarcodeEvent.OnScannerInitialized)
                            },
                            onInitializationFailed = {
                                onEvent(ScanBarcodeEvent.OnScannerInitializationFailed)
                            },
                            onCodeDetected = { codeType, codeValue ->
                                onEvent(
                                    ScanBarcodeEvent.OnScanSucceeded(
                                        codeType = codeType,
                                        codeValue = codeValue
                                    )
                                )
                            }
                        )

                        if (effectiveStatus.showProgress) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                } else if (effectiveStatus.showProgress) {
                    CircularProgressIndicator()
                }

                Text(
                    text = stringResource(id = effectiveStatus.titleRes),
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = effectiveStatus.messageRes),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                when {
                    effectiveStatus.showPermissionButton -> {
                        Button(
                            onClick = { onEvent(ScanBarcodeEvent.OnPermissionButtonClicked) },
                            modifier = Modifier.padding(top = 20.dp)
                        ) {
                            Text(text = stringResource(id = R.string.scan_barcode_allow_camera))
                        }
                    }

                    effectiveStatus.showOpenSettingsButton -> {
                        OutlinedButton(
                            onClick = { onEvent(ScanBarcodeEvent.OnOpenSettingsClicked) },
                            modifier = Modifier.padding(top = 20.dp)
                        ) {
                            Text(text = stringResource(id = R.string.scan_barcode_open_settings))
                        }
                    }

                    effectiveStatus.showRetryButton -> {
                        Button(
                            onClick = { onEvent(ScanBarcodeEvent.OnRetryClicked) },
                            modifier = Modifier.padding(top = 20.dp)
                        ) {
                            Text(text = stringResource(id = R.string.scan_barcode_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanBarcodeCameraPreview(
    modifier: Modifier = Modifier,
    onInitialized: () -> Unit,
    onInitializationFailed: () -> Unit,
    onCodeDetected: (CardCodeType, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
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
    val currentOnInitialized by rememberUpdatedState(onInitialized)
    val currentOnInitializationFailed by rememberUpdatedState(onInitializationFailed)
    val currentOnCodeDetected by rememberUpdatedState(onCodeDetected)

    DisposableEffect(barcodeScanner, analysisExecutor) {
        onDispose {
            barcodeScanner.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    DisposableEffect(lifecycleOwner, previewView, context, barcodeScanner, analysisExecutor) {
        val detectedCode = AtomicBoolean(false)
        val isProcessingFrame = AtomicBoolean(false)
        val isDisposed = AtomicBoolean(false)
        var boundCameraProvider: ProcessCameraProvider? = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val listener = Runnable {
            if (isDisposed.get()) {
                return@Runnable
            }

            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                boundCameraProvider = cameraProvider

                val preview = Preview.Builder()
                    .build()
                    .also { cameraPreview ->
                        cameraPreview.surfaceProvider = previewView.surfaceProvider
                    }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            if (detectedCode.get()) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            if (!isProcessingFrame.compareAndSet(false, true)) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val mediaImage = imageProxy.image
                            if (mediaImage == null) {
                                isProcessingFrame.set(false)
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val barcode = barcodes.firstOrNull { candidate ->
                                        !candidate.rawValue.isNullOrBlank() ||
                                            !candidate.displayValue.isNullOrBlank()
                                    }
                                    val codeValue = barcode?.rawValue
                                        ?.takeIf { value -> value.isNotBlank() }
                                        ?: barcode?.displayValue?.takeIf { value ->
                                            value.isNotBlank()
                                        }

                                    if (
                                        !isDisposed.get() &&
                                        barcode != null &&
                                            codeValue != null &&
                                            detectedCode.compareAndSet(false, true)
                                    ) {
                                        currentOnCodeDetected(
                                            barcode.format.toCardCodeType(),
                                            codeValue
                                        )
                                    }
                                }
                                .addOnFailureListener {
                                    // Ignore per-frame processing failures and keep the scanner active.
                                }
                                .addOnCompleteListener {
                                    isProcessingFrame.set(false)
                                    imageProxy.close()
                                }
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                if (!isDisposed.get()) {
                    currentOnInitialized()
                }
            }.onFailure {
                if (!isDisposed.get()) {
                    currentOnInitializationFailed()
                }
            }
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            isDisposed.set(true)
            boundCameraProvider?.unbindAll()
        }
    }
}

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

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
