package com.threemdroid.digitalwallet.feature.fullscreencode

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.threemdroid.digitalwallet.R
import kotlinx.coroutines.flow.collectLatest

object FullscreenCodeRoutes {
    const val cardIdArg = "cardId"
    const val fullscreenCode = "home/card-details/{$cardIdArg}/fullscreen-code"

    fun fullscreenCode(cardId: String): String = "home/card-details/$cardId/fullscreen-code"
}

@Composable
fun FullscreenCodeRoute(
    onNavigateBack: () -> Unit,
    viewModel: FullscreenCodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val brightnessManager = remember(activity) {
        FullscreenBrightnessManager(
            activity?.window?.let(::WindowBrightnessController) ?: NoOpBrightnessController()
        )
    }

    BackHandler {
        viewModel.onEvent(FullscreenCodeEvent.OnBackClicked)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                FullscreenCodeEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    DisposableEffect(
        uiState.isContentVisible,
        uiState.shouldMaximizeBrightness,
        brightnessManager
    ) {
        if (uiState.isContentVisible) {
            brightnessManager.onVisible(uiState.shouldMaximizeBrightness)
        } else {
            brightnessManager.onHidden()
        }

        onDispose {
            brightnessManager.onHidden()
        }
    }

    FullscreenCodeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FullscreenCodeScreen(
    uiState: FullscreenCodeUiState,
    onEvent: (FullscreenCodeEvent) -> Unit
) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onEvent(FullscreenCodeEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back),
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            uiState.isCardMissing -> {
                FullscreenCodeStatusState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(id = R.string.fullscreen_code_missing_title),
                    message = stringResource(id = R.string.fullscreen_code_missing_message)
                )
            }

            else -> {
                FullscreenCodeContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    uiState = uiState
                )
            }
        }
    }
}

@Composable
private fun FullscreenCodeContent(
    modifier: Modifier = Modifier,
    uiState: FullscreenCodeUiState
) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = uiState.cardName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        uiState.cardNumber?.let { cardNumber ->
            Text(
                text = cardNumber,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = uiState.codeTypeLabel,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.74f)
        )

        FullscreenCodeBitmap(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            uiState = uiState
        )

        SelectionContainer {
            Text(
                text = uiState.codeValue,
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FullscreenCodeBitmap(
    modifier: Modifier = Modifier,
    uiState: FullscreenCodeUiState,
    renderer: FullscreenCodeBitmapRenderer = remember { FullscreenCodeBitmapRenderer() }
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val widthDp = if (uiState.presentation == FullscreenCodePresentation.MATRIX) {
            maxWidth.coerceAtMost(320.dp)
        } else {
            maxWidth.coerceAtMost(420.dp)
        }
        val heightDp = if (uiState.presentation == FullscreenCodePresentation.MATRIX) {
            widthDp
        } else {
            widthDp * 0.42f
        }
        val widthPx = with(density) { widthDp.roundToPx() }.coerceAtLeast(1)
        val heightPx = with(density) { heightDp.roundToPx() }.coerceAtLeast(1)
        val renderState by produceState(
            initialValue = FullscreenCodeRenderState(),
            uiState.codeValue,
            uiState.codeType,
            widthPx,
            heightPx,
            renderer
        ) {
            value = FullscreenCodeRenderState(
                bitmap = renderer.render(
                    codeValue = uiState.codeValue,
                    codeType = uiState.codeType,
                    width = widthPx,
                    height = heightPx
                ),
                isLoaded = true
            )
        }

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp)
        ) {
            when {
                !renderState.isLoaded -> {
                    Box(
                        modifier = Modifier
                            .size(widthDp, heightDp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                }

                renderState.bitmap != null -> {
                    val bitmap = renderState.bitmap
                    androidx.compose.foundation.Image(
                        bitmap = checkNotNull(bitmap).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(widthDp, heightDp)
                            .padding(18.dp)
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .size(widthDp, heightDp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.codeValue,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class FullscreenCodeRenderState(
    val bitmap: Bitmap? = null,
    val isLoaded: Boolean = false
)

@Composable
private fun FullscreenCodeStatusState(
    modifier: Modifier = Modifier,
    title: String,
    message: String
) {
    Column(
        modifier = modifier.background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 10.dp).padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f),
            textAlign = TextAlign.Center
        )
    }
}

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
