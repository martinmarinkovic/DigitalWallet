package com.threemdroid.digitalwallet.data.reminder

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkManagerExpirationReminderWorkBackend @Inject constructor(
    private val workManager: WorkManager
) : ExpirationReminderWorkBackend {

    override fun enqueueReminder(
        reminder: ExpirationReminderRequest,
        initialDelayMillis: Long
    ) {
        val request = OneTimeWorkRequestBuilder<ExpirationReminderWorker>()
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(ExpirationReminderWorker.INPUT_CARD_ID, reminder.cardId)
                    .putString(ExpirationReminderWorker.INPUT_CARD_NAME, reminder.cardName)
                    .putString(
                        ExpirationReminderWorker.INPUT_EXPIRATION_DATE,
                        reminder.expirationDate.toString()
                    )
                    .putString(
                        ExpirationReminderWorker.INPUT_SCHEDULE_KEY,
                        reminder.scheduleKey
                    )
                    .build()
            )
            .addTag(ExpirationReminderWorker.REMINDER_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(cardId = reminder.cardId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancelReminder(cardId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(cardId))
    }

    companion object {
        fun uniqueWorkName(cardId: String): String = "expiration-reminder-$cardId"
    }
}
