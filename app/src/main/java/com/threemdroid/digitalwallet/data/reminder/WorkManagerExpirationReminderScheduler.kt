package com.threemdroid.digitalwallet.data.reminder

import com.threemdroid.digitalwallet.core.database.dao.ExpirationReminderStateDao
import com.threemdroid.digitalwallet.core.database.entity.ExpirationReminderStateEntity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkManagerExpirationReminderScheduler @Inject constructor(
    private val reminderStateDao: ExpirationReminderStateDao,
    private val workBackend: ExpirationReminderWorkBackend,
    private val clock: Clock
) : ExpirationReminderScheduler {

    override suspend fun syncReminders(
        reminders: List<ExpirationReminderRequest>,
        schedulingEnabled: Boolean
    ) = withContext(Dispatchers.IO) {
        val statesByCardId = reminderStateDao.getAllStates().associateBy { it.cardId }
        val remindersByCardId = reminders.associateBy { it.cardId }

        val staleCardIds = statesByCardId.keys - remindersByCardId.keys
        staleCardIds.forEach { cardId ->
            workBackend.cancelReminder(cardId)
            reminderStateDao.deleteState(cardId)
        }

        reminders.forEach { reminder ->
            val existingState = statesByCardId[reminder.cardId]
            if (!schedulingEnabled) {
                persistDisabledState(reminder, existingState)
                workBackend.cancelReminder(reminder.cardId)
                return@forEach
            }

            if (existingState?.scheduleKey == reminder.scheduleKey && existingState.deliveredAt != null) {
                workBackend.cancelReminder(reminder.cardId)
                return@forEach
            }

            reminderStateDao.upsertState(
                ExpirationReminderStateEntity(
                    cardId = reminder.cardId,
                    scheduleKey = reminder.scheduleKey,
                    scheduledAt = reminder.scheduledAt,
                    deliveredAt = null
                )
            )

            val delayMillis = Duration.between(Instant.now(clock), reminder.scheduledAt)
                .toMillis()
                .coerceAtLeast(0L)

            workBackend.enqueueReminder(
                reminder = reminder,
                initialDelayMillis = delayMillis
            )
        }
    }

    private suspend fun persistDisabledState(
        reminder: ExpirationReminderRequest,
        existingState: ExpirationReminderStateEntity?
    ) {
        val nextState = when {
            existingState == null -> {
                ExpirationReminderStateEntity(
                    cardId = reminder.cardId,
                    scheduleKey = reminder.scheduleKey,
                    scheduledAt = reminder.scheduledAt,
                    deliveredAt = null
                )
            }

            existingState.scheduleKey == reminder.scheduleKey -> {
                existingState
            }

            else -> {
                existingState.copy(
                    scheduleKey = reminder.scheduleKey,
                    scheduledAt = reminder.scheduledAt,
                    deliveredAt = null
                )
            }
        }

        reminderStateDao.upsertState(nextState)
    }
}
