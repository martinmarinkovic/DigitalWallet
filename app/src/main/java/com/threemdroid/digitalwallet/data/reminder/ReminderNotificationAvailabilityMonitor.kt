package com.threemdroid.digitalwallet.data.reminder

import kotlinx.coroutines.flow.Flow

interface ReminderNotificationAvailabilityMonitor {
    fun observeCanPostNotifications(): Flow<Boolean>
}
