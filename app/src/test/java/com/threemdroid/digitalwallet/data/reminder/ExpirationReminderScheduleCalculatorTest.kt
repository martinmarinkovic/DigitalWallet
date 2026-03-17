package com.threemdroid.digitalwallet.data.reminder

import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.WalletCard
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpirationReminderScheduleCalculatorTest {
    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-16T08:00:00Z"),
        ZoneOffset.UTC
    )
    private val calculator = ExpirationReminderScheduleCalculator(clock)

    @Test
    fun calculate_returnsNullForCardsWithoutExpirationOrPastExpiration() {
        val withoutExpiration = baseCard(
            id = "card-1",
            expirationDate = null
        )
        val expiredCard = baseCard(
            id = "card-2",
            expirationDate = LocalDate.parse("2026-03-15")
        )

        assertNull(calculator.calculate(withoutExpiration, ReminderTiming.ON_DAY))
        assertNull(calculator.calculate(expiredCard, ReminderTiming.ON_DAY))
    }

    @Test
    fun calculate_mapsReminderTimingToLocalReminderInstant() {
        val card = baseCard(
            id = "card-3",
            expirationDate = LocalDate.parse("2026-03-20")
        )

        val onDay = calculator.calculate(card, ReminderTiming.ON_DAY)
        val oneDayBefore = calculator.calculate(card, ReminderTiming.ONE_DAY_BEFORE)
        val threeDaysBefore = calculator.calculate(card, ReminderTiming.THREE_DAYS_BEFORE)
        val sevenDaysBefore = calculator.calculate(card, ReminderTiming.SEVEN_DAYS_BEFORE)

        assertEquals(Instant.parse("2026-03-20T09:00:00Z"), onDay?.scheduledAt)
        assertEquals(Instant.parse("2026-03-19T09:00:00Z"), oneDayBefore?.scheduledAt)
        assertEquals(Instant.parse("2026-03-17T09:00:00Z"), threeDaysBefore?.scheduledAt)
        assertEquals(Instant.parse("2026-03-13T09:00:00Z"), sevenDaysBefore?.scheduledAt)
    }

    private fun baseCard(
        id: String,
        expirationDate: LocalDate?
    ): WalletCard =
        WalletCard(
            id = id,
            name = "Card $id",
            categoryId = "category",
            codeValue = "CODE-$id",
            codeType = CardCodeType.QR_CODE,
            cardNumber = null,
            expirationDate = expirationDate,
            notes = null,
            isFavorite = false,
            position = 0,
            createdAt = Instant.parse("2026-03-16T08:00:00Z"),
            updatedAt = Instant.parse("2026-03-16T08:00:00Z")
        )
}
