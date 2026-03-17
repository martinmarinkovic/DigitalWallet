package com.threemdroid.digitalwallet.data.reminder

import android.content.Intent

object ExpirationReminderIntents {
    const val EXTRA_CARD_ID = "extra_card_id"

    fun consumeReminderCardId(intent: Intent?): String? {
        if (intent == null) {
            return null
        }

        val cardId = intent.getStringExtra(EXTRA_CARD_ID)?.trim()?.takeIf { value ->
            value.isNotEmpty()
        }
        intent.removeExtra(EXTRA_CARD_ID)
        return cardId
    }
}
