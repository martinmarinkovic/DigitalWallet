package com.threemdroid.digitalwallet.data.reminder

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpirationReminderIntentsTest {
    @Test
    fun consumeReminderCardId_returnsCardIdAndRemovesExtra() {
        val intent = Intent().apply {
            action = ExpirationReminderIntents.ACTION_OPEN_REMINDER_CARD
            putExtra(ExpirationReminderIntents.EXTRA_CARD_ID, "card-123")
        }

        val cardId = ExpirationReminderIntents.consumeReminderCardId(intent)

        assertEquals("card-123", cardId)
        assertFalse(intent.hasExtra(ExpirationReminderIntents.EXTRA_CARD_ID))
    }

    @Test
    fun consumeReminderCardId_returnsNullForMissingOrBlankExtra() {
        assertNull(ExpirationReminderIntents.consumeReminderCardId(null))

        val blankIntent = Intent().apply {
            action = ExpirationReminderIntents.ACTION_OPEN_REMINDER_CARD
            putExtra(ExpirationReminderIntents.EXTRA_CARD_ID, "   ")
        }

        assertNull(ExpirationReminderIntents.consumeReminderCardId(blankIntent))
        assertFalse(blankIntent.hasExtra(ExpirationReminderIntents.EXTRA_CARD_ID))
    }

    @Test
    fun consumeReminderCardId_ignoresUnexpectedLaunchAction() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            putExtra(ExpirationReminderIntents.EXTRA_CARD_ID, "card-123")
        }

        val cardId = ExpirationReminderIntents.consumeReminderCardId(intent)

        assertNull(cardId)
        assertFalse(intent.hasExtra(ExpirationReminderIntents.EXTRA_CARD_ID))
    }
}
