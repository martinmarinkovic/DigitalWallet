package com.threemdroid.digitalwallet.data.reminder

import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import com.threemdroid.digitalwallet.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpirationReminderSyncerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-16T08:00:00Z"),
        ZoneOffset.UTC
    )

    @Test
    fun start_schedulesOnlyExpirationAwareCards() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                reminderCard(
                    id = "expiring-card",
                    expirationDate = LocalDate.parse("2026-03-20")
                ),
                reminderCard(
                    id = "no-expiration-card",
                    expirationDate = null
                )
            )
        )
        val scheduler = FakeExpirationReminderScheduler()
        val notificationAvailabilityMonitor = FakeReminderNotificationAvailabilityMonitor()
        val syncer = ExpirationReminderSyncer(
            cardRepository = cardRepository,
            settingsRepository = FakeSettingsRepository(),
            scheduleCalculator = ExpirationReminderScheduleCalculator(clock),
            reminderScheduler = scheduler,
            notificationAvailabilityMonitor = notificationAvailabilityMonitor
        )

        syncer.start()
        advanceUntilIdle()

        val call = scheduler.calls.single()
        assertTrue(call.schedulingEnabled)
        assertEquals(listOf("expiring-card"), call.reminders.map { it.cardId })
    }

    @Test
    fun start_withOnlyCardsWithoutExpiration_emitsEmptyReminderPlan() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                reminderCard(
                    id = "card-1",
                    expirationDate = null
                ),
                reminderCard(
                    id = "card-2",
                    expirationDate = null
                )
            )
        )
        val scheduler = FakeExpirationReminderScheduler()
        val notificationAvailabilityMonitor = FakeReminderNotificationAvailabilityMonitor()
        val syncer = ExpirationReminderSyncer(
            cardRepository = cardRepository,
            settingsRepository = FakeSettingsRepository(),
            scheduleCalculator = ExpirationReminderScheduleCalculator(clock),
            reminderScheduler = scheduler,
            notificationAvailabilityMonitor = notificationAvailabilityMonitor
        )

        syncer.start()
        advanceUntilIdle()

        val call = scheduler.calls.single()
        assertTrue(call.schedulingEnabled)
        assertTrue(call.reminders.isEmpty())
    }

    @Test
    fun disabledReminders_keepEligibleCardsButDoNotScheduleThem() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                reminderCard(
                    id = "expiring-card",
                    expirationDate = LocalDate.parse("2026-03-20")
                )
            )
        )
        val scheduler = FakeExpirationReminderScheduler()
        val notificationAvailabilityMonitor = FakeReminderNotificationAvailabilityMonitor()
        val syncer = ExpirationReminderSyncer(
            cardRepository = cardRepository,
            settingsRepository = FakeSettingsRepository(
                initialSettings = AppSettings(
                    themeMode = ThemeMode.SYSTEM,
                    autoBrightnessEnabled = true,
                    reminderEnabled = false,
                    reminderTiming = ReminderTiming.ONE_DAY_BEFORE,
                    cloudSyncEnabled = false
                )
            ),
            scheduleCalculator = ExpirationReminderScheduleCalculator(clock),
            reminderScheduler = scheduler,
            notificationAvailabilityMonitor = notificationAvailabilityMonitor
        )

        syncer.start()
        advanceUntilIdle()

        val call = scheduler.calls.single()
        assertFalse(call.schedulingEnabled)
        assertEquals(listOf("expiring-card"), call.reminders.map { it.cardId })
        assertEquals(
            Instant.parse("2026-03-19T09:00:00Z"),
            call.reminders.single().scheduledAt
        )
    }

    @Test
    fun settingsAndCardChanges_updateAndRemoveReminderRequests() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                reminderCard(
                    id = "membership-card",
                    expirationDate = LocalDate.parse("2026-03-20")
                )
            )
        )
        val settingsRepository = FakeSettingsRepository()
        val scheduler = FakeExpirationReminderScheduler()
        val notificationAvailabilityMonitor = FakeReminderNotificationAvailabilityMonitor()
        val syncer = ExpirationReminderSyncer(
            cardRepository = cardRepository,
            settingsRepository = settingsRepository,
            scheduleCalculator = ExpirationReminderScheduleCalculator(clock),
            reminderScheduler = scheduler,
            notificationAvailabilityMonitor = notificationAvailabilityMonitor
        )

        syncer.start()
        advanceUntilIdle()
        assertEquals(1, scheduler.calls.last().reminders.size)
        assertTrue(scheduler.calls.last().schedulingEnabled)

        settingsRepository.settingsFlow.value = settingsRepository.settingsFlow.value.copy(
            reminderEnabled = false,
            reminderTiming = ReminderTiming.THREE_DAYS_BEFORE
        )
        advanceUntilIdle()

        val disabledCall = scheduler.calls.last()
        assertFalse(disabledCall.schedulingEnabled)
        assertEquals(
            "membership-card|2026-03-20|THREE_DAYS_BEFORE",
            disabledCall.reminders.single().scheduleKey
        )

        settingsRepository.settingsFlow.value = settingsRepository.settingsFlow.value.copy(
            reminderEnabled = true
        )
        cardRepository.cardsFlow.value = listOf(
            reminderCard(
                id = "membership-card",
                expirationDate = LocalDate.parse("2026-03-24")
            )
        )
        advanceUntilIdle()

        val updatedCall = scheduler.calls.last()
        assertTrue(updatedCall.schedulingEnabled)
        assertEquals(
            Instant.parse("2026-03-21T09:00:00Z"),
            updatedCall.reminders.single().scheduledAt
        )

        cardRepository.cardsFlow.value = emptyList()
        advanceUntilIdle()

        assertTrue(scheduler.calls.last().reminders.isEmpty())
    }

    @Test
    fun start_withNotificationsUnavailable_disablesScheduling() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                reminderCard(
                    id = "expiring-card",
                    expirationDate = LocalDate.parse("2026-03-20")
                )
            )
        )
        val scheduler = FakeExpirationReminderScheduler()
        val notificationAvailabilityMonitor = FakeReminderNotificationAvailabilityMonitor(
            initiallyAvailable = false
        )
        val syncer = ExpirationReminderSyncer(
            cardRepository = cardRepository,
            settingsRepository = FakeSettingsRepository(),
            scheduleCalculator = ExpirationReminderScheduleCalculator(clock),
            reminderScheduler = scheduler,
            notificationAvailabilityMonitor = notificationAvailabilityMonitor
        )

        syncer.start()
        advanceUntilIdle()

        val call = scheduler.calls.single()
        assertFalse(call.schedulingEnabled)
        assertEquals(listOf("expiring-card"), call.reminders.map { it.cardId })
    }

    @Test
    fun notificationAvailabilityChanges_reschedulesEligibleReminders() = runTest {
        val cardRepository = FakeCardRepository(
            cards = listOf(
                reminderCard(
                    id = "expiring-card",
                    expirationDate = LocalDate.parse("2026-03-20")
                )
            )
        )
        val scheduler = FakeExpirationReminderScheduler()
        val notificationAvailabilityMonitor = FakeReminderNotificationAvailabilityMonitor(
            initiallyAvailable = false
        )
        val syncer = ExpirationReminderSyncer(
            cardRepository = cardRepository,
            settingsRepository = FakeSettingsRepository(),
            scheduleCalculator = ExpirationReminderScheduleCalculator(clock),
            reminderScheduler = scheduler,
            notificationAvailabilityMonitor = notificationAvailabilityMonitor
        )

        syncer.start()
        advanceUntilIdle()
        assertFalse(scheduler.calls.last().schedulingEnabled)

        notificationAvailabilityMonitor.canPostNotificationsFlow.value = true
        advanceUntilIdle()

        assertTrue(scheduler.calls.last().schedulingEnabled)
        assertEquals(listOf("expiring-card"), scheduler.calls.last().reminders.map { it.cardId })
    }

    private fun reminderCard(
        id: String,
        expirationDate: LocalDate?
    ): WalletCard =
        WalletCard(
            id = id,
            name = "Card $id",
            categoryId = "category-1",
            codeValue = "CODE-$id",
            codeType = com.threemdroid.digitalwallet.core.model.CardCodeType.QR_CODE,
            cardNumber = null,
            expirationDate = expirationDate,
            notes = null,
            isFavorite = false,
            position = 0,
            createdAt = Instant.parse("2026-03-16T08:00:00Z"),
            updatedAt = Instant.parse("2026-03-16T08:00:00Z")
        )

    private class FakeCardRepository(
        cards: List<WalletCard> = emptyList()
    ) : CardRepository {
        val cardsFlow = MutableStateFlow(cards)

        override fun observeAllCards(): Flow<List<WalletCard>> = cardsFlow

        override fun observeCards(categoryId: String): Flow<List<WalletCard>> =
            cardsFlow.map { cards -> cards.filter { it.categoryId == categoryId } }

        override fun observeCard(cardId: String): Flow<WalletCard?> =
            cardsFlow.map { cards -> cards.firstOrNull { it.id == cardId } }

        override suspend fun upsertCard(card: WalletCard) = Unit

        override suspend fun upsertCards(cards: List<WalletCard>) = Unit

        override suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>) = Unit

        override suspend fun deleteCard(cardId: String) = Unit
    }

    private class FakeSettingsRepository(
        initialSettings: AppSettings = AppSettings(
            themeMode = ThemeMode.SYSTEM,
            autoBrightnessEnabled = true,
            reminderEnabled = true,
            reminderTiming = ReminderTiming.ON_DAY,
            cloudSyncEnabled = false
        )
    ) : SettingsRepository {
        val settingsFlow = MutableStateFlow(initialSettings)

        override fun observeSettings(): Flow<AppSettings> = settingsFlow

        override suspend fun updateSettings(settings: AppSettings) {
            settingsFlow.value = settings
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit

        override suspend fun setAutoBrightnessEnabled(enabled: Boolean) = Unit

        override suspend fun setReminderEnabled(enabled: Boolean) = Unit

        override suspend fun setReminderTiming(reminderTiming: ReminderTiming) = Unit

        override suspend fun setCloudSyncEnabled(enabled: Boolean) = Unit
    }

    private class FakeExpirationReminderScheduler : ExpirationReminderScheduler {
        val calls = mutableListOf<SchedulerCall>()

        override suspend fun syncReminders(
            reminders: List<ExpirationReminderRequest>,
            schedulingEnabled: Boolean
        ) {
            calls += SchedulerCall(
                reminders = reminders,
                schedulingEnabled = schedulingEnabled
            )
        }
    }

    private data class SchedulerCall(
        val reminders: List<ExpirationReminderRequest>,
        val schedulingEnabled: Boolean
    )

    private class FakeReminderNotificationAvailabilityMonitor(
        initiallyAvailable: Boolean = true
    ) : ReminderNotificationAvailabilityMonitor {
        val canPostNotificationsFlow = MutableStateFlow(initiallyAvailable)

        override fun observeCanPostNotifications(): Flow<Boolean> =
            canPostNotificationsFlow.asStateFlow()
    }
}
