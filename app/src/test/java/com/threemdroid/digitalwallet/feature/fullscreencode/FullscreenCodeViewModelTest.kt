package com.threemdroid.digitalwallet.feature.fullscreencode

import androidx.lifecycle.SavedStateHandle
import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FullscreenCodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_withQrCard_exposesMatrixPresentationAndBrightnessPreference() = runTest {
        val viewModel = FullscreenCodeViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(FullscreenCodeRoutes.cardIdArg to "card_library")
            ),
            cardRepository = FakeCardRepository(
                cards = listOf(
                    walletCard(
                        id = "card_library",
                        name = "City Library",
                        codeType = CardCodeType.QR_CODE,
                        cardNumber = "LIB-7788"
                    )
                )
            ),
            settingsRepository = FakeSettingsRepository(
                settings = AppSettings(autoBrightnessEnabled = true)
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCardMissing)
        assertEquals("City Library", state.cardName)
        assertEquals("LIB-7788", state.cardNumber)
        assertEquals("CODE-card_library", state.codeValue)
        assertEquals(CardCodeType.QR_CODE, state.codeType)
        assertEquals("QR Code", state.codeTypeLabel)
        assertEquals(FullscreenCodePresentation.MATRIX, state.presentation)
        assertTrue(state.shouldMaximizeBrightness)
    }

    @Test
    fun init_withLinearCode_trimsBlankCardNumberAndKeepsBrightnessDisabled() = runTest {
        val viewModel = FullscreenCodeViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(FullscreenCodeRoutes.cardIdArg to "card_gym")
            ),
            cardRepository = FakeCardRepository(
                cards = listOf(
                    walletCard(
                        id = "card_gym",
                        name = "Gym Access",
                        codeType = CardCodeType.CODE_128,
                        cardNumber = "   "
                    )
                )
            ),
            settingsRepository = FakeSettingsRepository(
                settings = AppSettings(autoBrightnessEnabled = false)
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isCardMissing)
        assertNull(state.cardNumber)
        assertEquals("Code 128", state.codeTypeLabel)
        assertEquals(FullscreenCodePresentation.LINEAR, state.presentation)
        assertFalse(state.shouldMaximizeBrightness)
    }

    @Test
    fun init_withMissingCard_exposesMissingState() = runTest {
        val viewModel = FullscreenCodeViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(FullscreenCodeRoutes.cardIdArg to "missing_card")
            ),
            cardRepository = FakeCardRepository(),
            settingsRepository = FakeSettingsRepository(
                settings = AppSettings(autoBrightnessEnabled = false)
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isCardMissing)
        assertFalse(state.shouldMaximizeBrightness)
    }

    @Test
    fun init_withoutCardId_exposesMissingStateInsteadOfCrashing() = runTest {
        val viewModel = FullscreenCodeViewModel(
            savedStateHandle = SavedStateHandle(),
            cardRepository = FakeCardRepository(),
            settingsRepository = FakeSettingsRepository(
                settings = AppSettings(autoBrightnessEnabled = true)
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isCardMissing)
        assertTrue(state.shouldMaximizeBrightness)
    }

    @Test
    fun onBackClicked_emitsNavigateBack() = runTest {
        val viewModel = FullscreenCodeViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(FullscreenCodeRoutes.cardIdArg to "card_access")
            ),
            cardRepository = FakeCardRepository(
                cards = listOf(walletCard(id = "card_access", name = "Office Badge"))
            ),
            settingsRepository = FakeSettingsRepository()
        )

        advanceUntilIdle()

        val deferredEffect = async { viewModel.effects.first() }
        viewModel.onEvent(FullscreenCodeEvent.OnBackClicked)

        assertEquals(FullscreenCodeEffect.NavigateBack, deferredEffect.await())
    }

    private class FakeCardRepository(
        cards: List<WalletCard> = emptyList()
    ) : CardRepository {
        private val cardsFlow = MutableStateFlow(cards)

        override fun observeAllCards(): Flow<List<WalletCard>> = cardsFlow

        override fun observeCards(categoryId: String): Flow<List<WalletCard>> =
            cardsFlow.map { cards ->
                cards.filter { card -> card.categoryId == categoryId }
            }

        override fun observeCard(cardId: String): Flow<WalletCard?> =
            cardsFlow.map { cards ->
                cards.firstOrNull { card -> card.id == cardId }
            }

        override suspend fun upsertCard(card: WalletCard) {
            cardsFlow.value = cardsFlow.value
                .filterNot { existing -> existing.id == card.id } + card
        }

        override suspend fun upsertCards(cards: List<WalletCard>) {
            cards.forEach { card -> upsertCard(card) }
        }

        override suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>) {
            error("Not needed for FullscreenCodeViewModelTest")
        }

        override suspend fun deleteCard(cardId: String) {
            cardsFlow.value = cardsFlow.value.filterNot { card -> card.id == cardId }
        }
    }

    private class FakeSettingsRepository(
        settings: AppSettings = AppSettings()
    ) : SettingsRepository {
        private val settingsFlow = MutableStateFlow(settings)

        override fun observeSettings(): Flow<AppSettings> = settingsFlow

        override suspend fun updateSettings(settings: AppSettings) {
            settingsFlow.value = settings
        }

        override suspend fun setThemeMode(themeMode: com.threemdroid.digitalwallet.core.model.ThemeMode) {
            error("Not needed for FullscreenCodeViewModelTest")
        }

        override suspend fun setAutoBrightnessEnabled(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(autoBrightnessEnabled = enabled)
        }

        override suspend fun setReminderEnabled(enabled: Boolean) {
            error("Not needed for FullscreenCodeViewModelTest")
        }

        override suspend fun setReminderTiming(reminderTiming: com.threemdroid.digitalwallet.core.model.ReminderTiming) {
            error("Not needed for FullscreenCodeViewModelTest")
        }

        override suspend fun setCloudSyncEnabled(enabled: Boolean) {
            error("Not needed for FullscreenCodeViewModelTest")
        }
    }

    private fun walletCard(
        id: String,
        name: String,
        categoryId: String = "default_access",
        codeType: CardCodeType = CardCodeType.QR_CODE,
        cardNumber: String? = null
    ): WalletCard =
        WalletCard(
            id = id,
            name = name,
            categoryId = categoryId,
            codeValue = "CODE-$id",
            codeType = codeType,
            cardNumber = cardNumber,
            expirationDate = null,
            notes = null,
            isFavorite = false,
            position = 0,
            createdAt = Instant.parse("2026-03-13T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-13T10:00:00Z")
        )
}
