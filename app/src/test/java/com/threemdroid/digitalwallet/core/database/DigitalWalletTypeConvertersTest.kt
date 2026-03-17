package com.threemdroid.digitalwallet.core.database

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DigitalWalletTypeConvertersTest {
    private val converters = DigitalWalletTypeConverters()

    @Test
    fun toLocalDate_withInvalidStoredValue_returnsNullInsteadOfThrowing() {
        assertNull(converters.toLocalDate("not-a-date"))
    }

    @Test
    fun toLocalDate_withValidStoredValue_returnsParsedDate() {
        assertEquals(
            LocalDate.parse("2026-03-17"),
            converters.toLocalDate("2026-03-17")
        )
    }
}
