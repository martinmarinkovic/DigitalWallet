package com.threemdroid.digitalwallet.data.reminder

interface ExpirationReminderWorkBackend {
    fun enqueueReminder(
        reminder: ExpirationReminderRequest,
        initialDelayMillis: Long
    )

    fun cancelReminder(cardId: String)
}
