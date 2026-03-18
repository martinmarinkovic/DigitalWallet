package com.threemdroid.digitalwallet.data.reminder

import com.threemdroid.digitalwallet.data.card.CardRepository
import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Singleton
class ExpirationReminderSyncer @Inject constructor(
    private val cardRepository: CardRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduleCalculator: ExpirationReminderScheduleCalculator,
    private val reminderScheduler: ExpirationReminderScheduler,
    private val notificationAvailabilityMonitor: ReminderNotificationAvailabilityMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var syncJob: Job? = null

    fun start() {
        if (syncJob != null) {
            return
        }

        syncJob = scope.launch {
            combine(
                settingsRepository.observeSettings(),
                cardRepository.observeAllCards(),
                notificationAvailabilityMonitor.observeCanPostNotifications()
            ) { settings, cards, canPostNotifications ->
                ExpirationReminderSyncPlan(
                    schedulingEnabled = settings.reminderEnabled && canPostNotifications,
                    reminders = cards
                        .mapNotNull { card ->
                            scheduleCalculator.calculate(
                                card = card,
                                reminderTiming = settings.reminderTiming
                            )
                        }
                        .sortedBy { reminder -> reminder.cardId }
                )
            }
                .distinctUntilChanged()
                .collect { plan ->
                    reminderScheduler.syncReminders(
                        reminders = plan.reminders,
                        schedulingEnabled = plan.schedulingEnabled
                    )
                }
        }
    }
}

private data class ExpirationReminderSyncPlan(
    val schedulingEnabled: Boolean,
    val reminders: List<ExpirationReminderRequest>
)
