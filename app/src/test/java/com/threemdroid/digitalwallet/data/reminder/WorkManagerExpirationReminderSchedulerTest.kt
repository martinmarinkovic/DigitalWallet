package com.threemdroid.digitalwallet.data.reminder

import com.threemdroid.digitalwallet.data.BaseRepositoryTest
import com.threemdroid.digitalwallet.core.database.entity.ExpirationReminderStateEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkManagerExpirationReminderSchedulerTest : BaseRepositoryTest() {
    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-16T08:00:00Z"),
        ZoneOffset.UTC
    )
    private val reminderStateDao by lazy { database.expirationReminderStateDao() }
    private val backend = FakeExpirationReminderWorkBackend()
    private val scheduler by lazy {
        WorkManagerExpirationReminderScheduler(
            reminderStateDao = reminderStateDao,
            workBackend = backend,
            clock = clock
        )
    }

    @Test
    fun syncReminders_enabled_enqueuesReminderAndPersistsUndeliveredState() = runBlocking {
        val reminder = reminderRequest(
            cardId = "card-1",
            scheduledAt = Instant.parse("2026-03-18T09:00:00Z")
        )

        scheduler.syncReminders(
            reminders = listOf(reminder),
            schedulingEnabled = true
        )

        val state = reminderStateDao.getState(reminder.cardId)
        assertNotNull(state)
        assertEquals(reminder.scheduleKey, state?.scheduleKey)
        assertEquals(reminder.scheduledAt, state?.scheduledAt)
        assertEquals(null, state?.deliveredAt)
        assertEquals(1, backend.enqueued.size)
        assertEquals(reminder.cardId, backend.enqueued.single().reminder.cardId)
        assertEquals(176_400_000L, backend.enqueued.single().initialDelayMillis)
    }

    @Test
    fun syncReminders_sameDeliveredReminderDoesNotReschedule() = runBlocking {
        val reminder = reminderRequest(cardId = "card-1")
        reminderStateDao.upsertState(
            ExpirationReminderStateEntity(
                cardId = reminder.cardId,
                scheduleKey = reminder.scheduleKey,
                scheduledAt = reminder.scheduledAt,
                deliveredAt = Instant.parse("2026-03-17T09:05:00Z")
            )
        )

        scheduler.syncReminders(
            reminders = listOf(reminder),
            schedulingEnabled = true
        )

        assertTrue(backend.enqueued.isEmpty())
        assertEquals(listOf(reminder.cardId), backend.cancelled)
        assertEquals(
            Instant.parse("2026-03-17T09:05:00Z"),
            reminderStateDao.getState(reminder.cardId)?.deliveredAt
        )
    }

    @Test
    fun syncReminders_deletedCard_cancelsReminderAndRemovesState() = runBlocking {
        val deletedReminder = reminderRequest(cardId = "card-1")
        reminderStateDao.upsertState(
            ExpirationReminderStateEntity(
                cardId = deletedReminder.cardId,
                scheduleKey = deletedReminder.scheduleKey,
                scheduledAt = deletedReminder.scheduledAt,
                deliveredAt = null
            )
        )

        scheduler.syncReminders(
            reminders = emptyList(),
            schedulingEnabled = true
        )

        assertEquals(listOf(deletedReminder.cardId), backend.cancelled)
        assertNull(reminderStateDao.getState(deletedReminder.cardId))
    }

    @Test
    fun syncReminders_disabledAndRemovedCards_cancelWorkAndPruneStaleState() = runBlocking {
        val activeReminder = reminderRequest(cardId = "card-1")
        val staleReminder = reminderRequest(cardId = "card-2")
        reminderStateDao.upsertState(
            ExpirationReminderStateEntity(
                cardId = activeReminder.cardId,
                scheduleKey = "old-key",
                scheduledAt = Instant.parse("2026-03-17T09:00:00Z"),
                deliveredAt = null
            )
        )
        reminderStateDao.upsertState(
            ExpirationReminderStateEntity(
                cardId = staleReminder.cardId,
                scheduleKey = staleReminder.scheduleKey,
                scheduledAt = staleReminder.scheduledAt,
                deliveredAt = null
            )
        )

        scheduler.syncReminders(
            reminders = listOf(activeReminder),
            schedulingEnabled = false
        )

        assertTrue(backend.enqueued.isEmpty())
        assertEquals(
            listOf(staleReminder.cardId, activeReminder.cardId),
            backend.cancelled
        )
        assertNull(reminderStateDao.getState(staleReminder.cardId))
        assertEquals(activeReminder.scheduleKey, reminderStateDao.getState(activeReminder.cardId)?.scheduleKey)
        assertEquals(null, reminderStateDao.getState(activeReminder.cardId)?.deliveredAt)
    }

    private fun reminderRequest(
        cardId: String,
        scheduledAt: Instant = Instant.parse("2026-03-17T09:00:00Z")
    ): ExpirationReminderRequest =
        ExpirationReminderRequest(
            cardId = cardId,
            cardName = "Card $cardId",
            expirationDate = LocalDate.parse("2026-03-20"),
            scheduleKey = "$cardId|2026-03-20|THREE_DAYS_BEFORE",
            scheduledAt = scheduledAt
        )

    private class FakeExpirationReminderWorkBackend : ExpirationReminderWorkBackend {
        val enqueued = mutableListOf<EnqueuedReminder>()
        val cancelled = mutableListOf<String>()

        override fun enqueueReminder(
            reminder: ExpirationReminderRequest,
            initialDelayMillis: Long
        ) {
            enqueued += EnqueuedReminder(
                reminder = reminder,
                initialDelayMillis = initialDelayMillis
            )
        }

        override fun cancelReminder(cardId: String) {
            cancelled += cardId
        }
    }

    private data class EnqueuedReminder(
        val reminder: ExpirationReminderRequest,
        val initialDelayMillis: Long
    )
}
