package com.threemdroid.digitalwallet.data.reminder

import java.time.Instant
import java.time.LocalDate

data class ExpirationReminderRequest(
    val cardId: String,
    val cardName: String,
    val expirationDate: LocalDate,
    val scheduleKey: String,
    val scheduledAt: Instant
)
