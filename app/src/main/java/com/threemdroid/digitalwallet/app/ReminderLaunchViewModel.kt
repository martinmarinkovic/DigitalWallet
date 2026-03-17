package com.threemdroid.digitalwallet.app

import androidx.lifecycle.ViewModel
import com.threemdroid.digitalwallet.data.card.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface ReminderLaunchDestination {
    data object Home : ReminderLaunchDestination

    data class CardDetails(val cardId: String) : ReminderLaunchDestination
}

@HiltViewModel
class ReminderLaunchViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {
    suspend fun resolveReminderDestination(cardId: String): ReminderLaunchDestination =
        if (cardRepository.observeCard(cardId).first() != null) {
            ReminderLaunchDestination.CardDetails(cardId)
        } else {
            ReminderLaunchDestination.Home
        }
}
