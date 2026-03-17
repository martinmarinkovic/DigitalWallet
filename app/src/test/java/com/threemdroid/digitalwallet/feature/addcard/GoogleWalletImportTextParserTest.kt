package com.threemdroid.digitalwallet.feature.addcard

import com.threemdroid.digitalwallet.core.model.CardCodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleWalletImportTextParserTest {
    private val parser = GoogleWalletImportTextParser()

    @Test
    fun parse_withCardTextExtractsHintsAndKeepsRawNotes() {
        val result = parser.parse(
            """
            City Library
            Member number: LIB-7788
            Barcode: 1234567890
            https://pay.google.com/gp/v/save/test-pass
            """.trimIndent()
        )

        assertEquals(CardCodeType.OTHER, result.codeType)
        assertEquals("1234567890", result.codeValue)
        assertEquals("LIB-7788", result.cardNumber)
        assertEquals("City Library", result.cardName)
        assertEquals(
            """
            City Library
            Member number: LIB-7788
            Barcode: 1234567890
            https://pay.google.com/gp/v/save/test-pass
            """.trimIndent(),
            result.notes
        )
    }

    @Test
    fun parse_withOnlyShareLinkDoesNotInventCardFields() {
        val result = parser.parse("https://pay.google.com/gp/v/save/test-pass")

        assertNull(result.codeType)
        assertNull(result.codeValue)
        assertNull(result.cardNumber)
        assertNull(result.cardName)
        assertEquals("https://pay.google.com/gp/v/save/test-pass", result.notes)
    }

    @Test
    fun parse_withPartialShareTextKeepsOnlyAvailableHints() {
        val result = parser.parse(
            """
            Gym
            https://pay.google.com/gp/v/save/gym-pass
            """.trimIndent()
        )

        assertNull(result.codeType)
        assertNull(result.codeValue)
        assertNull(result.cardNumber)
        assertEquals("Gym", result.cardName)
        assertEquals(
            """
            Gym
            https://pay.google.com/gp/v/save/gym-pass
            """.trimIndent(),
            result.notes
        )
    }
}
