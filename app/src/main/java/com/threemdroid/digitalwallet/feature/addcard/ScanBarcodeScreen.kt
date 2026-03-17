package com.threemdroid.digitalwallet.feature.addcard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(viewModel) {
        viewModel.onEvent(ScanBarcodeEvent.OnScreenOpened)
    }

    LaunchedEffect(viewModel, scanner) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                ScanBarcodeEffect.NavigateBack -> onNavigateBack()
                ScanBarcodeEffect.LaunchScanner -> {
                    val barcodeScanner = scanner
                    if (barcodeScanner == null) {
                        viewModel.onEvent(ScanBarcodeEvent.OnScanFailed)
                    } else {
                        barcodeScanner.startScan()
                            .addOnSuccessListener { barcode ->
                                val codeValue = barcode.rawValue
                                    ?.takeIf { value -> value.isNotBlank() }
                                    ?: barcode.displayValue?.takeIf { value -> value.isNotBlank() }
                                if (codeValue == null) {
                                    viewModel.onEvent(ScanBarcodeEvent.OnScanFailed)
                                } else {
                                    viewModel.onEvent(
                                        ScanBarcodeEvent.OnScanSucceeded(
                                            codeType = barcode.format.toCardCodeType(),
                                            codeValue = codeValue
                                        )
                                    )
                                }
                            }
                            .addOnCanceledListener {
                                viewModel.onEvent(ScanBarcodeEvent.OnScanCancelled)
                            }
                            .addOnFailureListener {
                                viewModel.onEvent(ScanBarcodeEvent.OnScanFailed)
                            }
                    }
                }

                is ScanBarcodeEffect.OpenConfirmation -> onNavigateToConfirmation(effect.route)
            }
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
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.status == ScanBarcodeStatus.LAUNCHING) {
                    CircularProgressIndicator()
                }
                Text(
                    text = stringResource(id = uiState.status.titleRes),
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = uiState.status.messageRes),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                if (uiState.status.showRetryButton) {
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
