package com.threemdroid.digitalwallet.feature.carddetails

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import com.threemdroid.digitalwallet.core.navigation.encodeRouteValue
import com.threemdroid.digitalwallet.feature.fullscreencode.FullscreenBrightnessManager
import com.threemdroid.digitalwallet.feature.fullscreencode.FullscreenCodeBitmapRenderer
import com.threemdroid.digitalwallet.feature.fullscreencode.FullscreenCodePresentation
import com.threemdroid.digitalwallet.feature.fullscreencode.NoOpBrightnessController
import com.threemdroid.digitalwallet.feature.fullscreencode.WindowBrightnessController
import com.threemdroid.digitalwallet.feature.fullscreencode.toFullscreenPresentation
import kotlinx.coroutines.flow.collectLatest

object CardDetailsRoutes {
    const val cardIdArg = "cardId"
    const val cardDetails = "home/card-details/{$cardIdArg}"

    fun cardDetails(cardId: String): String =
        "home/card-details/${encodeRouteValue(cardId)}"
}

object EditCardRoutes {
    const val cardIdArg = CardDetailsRoutes.cardIdArg
    const val editCard = "home/card-details/{$cardIdArg}/edit"

    fun editCard(cardId: String): String =
        "home/card-details/${encodeRouteValue(cardId)}/edit"
}

object FullscreenCodeRoutes {
    const val cardIdArg = CardDetailsRoutes.cardIdArg
    const val fullscreenCode = "home/card-details/{$cardIdArg}/fullscreen-code"

    fun fullscreenCode(cardId: String): String =
        "home/card-details/${encodeRouteValue(cardId)}/fullscreen-code"
}

@Composable
fun CardDetailsRoute(
    onNavigateBack: () -> Unit,
    onOpenEdit: (String) -> Unit,
    onOpenFullscreenCode: (String) -> Unit,
    viewModel: CardDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarHostState = remember { SnackbarHostState() }
    val brightnessManager = remember(activity) {
        FullscreenBrightnessManager(
            activity?.window?.let(::WindowBrightnessController) ?: NoOpBrightnessController()
        )
    }

    LaunchedEffect(viewModel, context, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                CardDetailsEffect.NavigateBack -> onNavigateBack()
                is CardDetailsEffect.OpenEdit -> onOpenEdit(effect.cardId)
                is CardDetailsEffect.OpenShareSheet -> {
                    if (!context.openShareSheet(effect.title, effect.shareText)) {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.card_details_share_failed_message)
                        )
                    }
                }
                is CardDetailsEffect.OpenFullscreenCode -> onOpenFullscreenCode(effect.cardId)
            }
        }
    }

    DisposableEffect(uiState.isContentVisible, brightnessManager) {
        if (uiState.isContentVisible) {
            brightnessManager.onVisible(shouldMaximizeBrightness = true)
        } else {
            brightnessManager.onHidden()
        }

        onDispose {
            brightnessManager.onHidden()
        }
    }

    CardDetailsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CardDetailsScreen(
    uiState: CardDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (CardDetailsEvent) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.title.isBlank()) {
                            stringResource(id = R.string.card_details_title)
                        } else {
                            uiState.title
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CardDetailsEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    if (uiState.isContentVisible) {
                        IconButton(
                            onClick = { onEvent(CardDetailsEvent.OnFavoriteClicked) },
                            enabled = !uiState.isFavoriteUpdating && !uiState.isDeleteInProgress
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = if (uiState.isFavorite) {
                                    stringResource(id = R.string.card_details_remove_favorite)
                                } else {
                                    stringResource(id = R.string.card_details_add_favorite)
                                }
                            )
                        }
                        IconButton(onClick = { onEvent(CardDetailsEvent.OnEditClicked) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(id = R.string.card_details_edit)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
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
                    CircularProgressIndicator()
                }
            }

            uiState.isCardMissing -> {
                CardDetailsStatusState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(id = R.string.card_details_missing_title),
                    message = stringResource(id = R.string.card_details_missing_message)
                )
            }

            else -> {
                CardDetailsContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }

    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(CardDetailsEvent.OnDeleteDismissed) },
            title = {
                Text(text = stringResource(id = R.string.card_details_delete_title))
            },
            text = {
                Text(text = stringResource(id = R.string.card_details_delete_message))
            },
            confirmButton = {
                TextButton(onClick = { onEvent(CardDetailsEvent.OnDeleteConfirmed) }) {
                    Text(text = stringResource(id = R.string.card_details_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(CardDetailsEvent.OnDeleteDismissed) }) {
                    Text(text = stringResource(id = R.string.create_category_cancel))
                }
            }
        )
    }
}

@Composable
private fun CardDetailsContent(
    modifier: Modifier = Modifier,
    uiState: CardDetailsUiState,
    onEvent: (CardDetailsEvent) -> Unit
) {
    val presentation = uiState.codeType.toFullscreenPresentation()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CardDetailsValueBlock(
                    label = stringResource(id = R.string.card_details_category_label),
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.categoryName.ifBlank {
                        stringResource(id = R.string.card_details_unknown_category)
                    }
                )

                CardDetailsCodeBitmap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    codeValue = uiState.codeValue,
                    codeType = uiState.codeType,
                    presentation = presentation
                )

                CardDetailsValueBlock(
                    label = stringResource(id = R.string.manual_entry_code_type_label),
                    value = uiState.codeTypeLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )

                CardDetailsValueBlock(
                    label = stringResource(id = R.string.manual_entry_code_value_label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = uiState.codeValue,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                uiState.notes?.let { notes ->
                    CardDetailsValueBlock(
                        label = stringResource(id = R.string.card_details_notes_label),
                        value = notes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }
        }

        if (uiState.isActionErrorVisible) {
            Text(
                text = stringResource(id = R.string.card_details_action_error_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = { onEvent(CardDetailsEvent.OnDeleteClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isDeleteInProgress,
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null
            )
            Text(
                text = stringResource(id = R.string.card_details_delete),
                modifier = Modifier.padding(start = 8.dp),
                color = Color.White
            )
        }

        Button(
            onClick = { onEvent(CardDetailsEvent.OnShareClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isDeleteInProgress,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.card_details_share),
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CardDetailsValueBlock(
    label: String,
    value: String? = null,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (content != null) {
            content()
        } else if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun CardDetailsCodeBitmap(
    modifier: Modifier = Modifier,
    codeValue: String,
    codeType: com.threemdroid.digitalwallet.core.model.CardCodeType,
    presentation: FullscreenCodePresentation,
    renderer: FullscreenCodeBitmapRenderer = remember { FullscreenCodeBitmapRenderer() }
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val widthDp = if (presentation == FullscreenCodePresentation.MATRIX) {
            maxWidth.coerceAtMost(280.dp)
        } else {
            maxWidth.coerceAtMost(420.dp)
        }
        val heightDp = if (presentation == FullscreenCodePresentation.MATRIX) {
            widthDp
        } else {
            widthDp * 0.42f
        }
        val widthPx = with(density) { widthDp.toPx().toInt() }
        val heightPx = with(density) { heightDp.toPx().toInt() }
        val renderState by produceState(
            initialValue = CardDetailsCodeRenderState(),
            codeValue,
            codeType,
            widthPx,
            heightPx,
            renderer
        ) {
            value = CardDetailsCodeRenderState(
                bitmap = renderer.render(
                    codeValue = codeValue,
                    codeType = codeType,
                    width = widthPx.coerceAtLeast(1),
                    height = heightPx.coerceAtLeast(1)
                ),
                isLoaded = true
            )
        }

        Surface(
            color = Color.White,
            shape = MaterialTheme.shapes.large
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
                    Image(
                        bitmap = checkNotNull(renderState.bitmap).asImageBitmap(),
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
                            text = codeValue,
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

private data class CardDetailsCodeRenderState(
    val bitmap: Bitmap? = null,
    val isLoaded: Boolean = false
)

@Composable
private fun CardDetailsStatusState(
    modifier: Modifier = Modifier,
    title: String,
    message: String
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
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

private fun Context.openShareSheet(
    title: String,
    shareText: String
): Boolean {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooserIntent = Intent.createChooser(shareIntent, null).apply {
        if (this@openShareSheet !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return runCatching {
        startActivity(chooserIntent)
    }.isSuccess
}
