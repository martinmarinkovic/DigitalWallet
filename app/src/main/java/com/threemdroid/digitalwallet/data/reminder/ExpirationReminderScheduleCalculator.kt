package com.threemdroid.digitalwallet.data.reminder

import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.WalletCard
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class ExpirationReminderScheduleCalculator @Inject constructor(
    private val clock: Clock
) {
    fun calculate(card: WalletCard, reminderTiming: ReminderTiming): ExpirationReminderRequest? {
        val expirationDate = card.expirationDate ?: return null
        if (expirationDate.isBefore(LocalDate.now(clock))) {
            return null
        }

        val scheduledAt = expirationDate
            .minusDays(reminderTiming.daysBefore.toLong())
            .atTime(REMINDER_TIME)
            .atZone(clock.zone)
            .toInstant()

        return ExpirationReminderRequest(
            cardId = card.id,
            cardName = card.name,
            expirationDate = expirationDate,
            scheduleKey = buildScheduleKey(card.id, expirationDate, reminderTiming),
            scheduledAt = scheduledAt
        )
    }

    companion object {
        private val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

        fun buildScheduleKey(
            cardId: String,
            expirationDate: LocalDate,
            reminderTiming: ReminderTiming
        ): String = "$cardId|$expirationDate|${reminderTiming.name}"
    }
}

private val ReminderTiming.daysBefore: Int
    get() = when (this) {
        ReminderTiming.ON_DAY -> 0
        ReminderTiming.ONE_DAY_BEFORE -> 1
        ReminderTiming.THREE_DAYS_BEFORE -> 3
        ReminderTiming.SEVEN_DAYS_BEFORE -> 7
    }
