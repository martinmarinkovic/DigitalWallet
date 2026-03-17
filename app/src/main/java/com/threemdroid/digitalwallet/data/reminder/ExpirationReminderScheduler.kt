package com.threemdroid.digitalwallet.data.reminder

interface ExpirationReminderScheduler {
    suspend fun syncReminders(
        reminders: List<ExpirationReminderRequest>,
        schedulingEnabled: Boolean
    )
}
