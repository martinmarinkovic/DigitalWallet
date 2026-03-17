package com.threemdroid.digitalwallet.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.threemdroid.digitalwallet.MainActivity
import com.threemdroid.digitalwallet.R
import com.threemdroid.digitalwallet.core.database.dao.AppSettingsDao
import com.threemdroid.digitalwallet.core.database.dao.ExpirationReminderStateDao
import com.threemdroid.digitalwallet.core.database.mapper.asExternalModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter

class ExpirationReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val cardId = inputData.getString(INPUT_CARD_ID) ?: return Result.failure()
        val cardName = inputData.getString(INPUT_CARD_NAME) ?: return Result.failure()
        val expirationDateValue = inputData.getString(INPUT_EXPIRATION_DATE) ?: return Result.failure()
        val scheduleKey = inputData.getString(INPUT_SCHEDULE_KEY) ?: return Result.failure()
        val expirationDate = runCatching { LocalDate.parse(expirationDateValue) }.getOrNull()
            ?: return Result.failure()

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ExpirationReminderWorkerEntryPoint::class.java
        )
        val reminderStateDao = entryPoint.expirationReminderStateDao()
        val appSettingsDao = entryPoint.appSettingsDao()

        val reminderState = reminderStateDao.getState(cardId)
        if (
            reminderState == null ||
            reminderState.scheduleKey != scheduleKey ||
            reminderState.deliveredAt != null
        ) {
            return Result.success()
        }

        val reminderEnabled = appSettingsDao.getSettings()
            ?.asExternalModel()
            ?.reminderEnabled
            ?: true
        if (!reminderEnabled || !canPostNotifications()) {
            return Result.success()
        }

        createNotificationChannel()
        NotificationManagerCompat.from(applicationContext).notify(
            cardId.hashCode(),
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(
                    applicationContext.getString(
                        R.string.expiration_reminder_notification_title,
                        cardName
                    )
                )
                .setContentText(
                    applicationContext.getString(
                        R.string.expiration_reminder_notification_message,
                        expirationDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    )
                )
                .setContentIntent(createContentIntent(cardId))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )

        reminderStateDao.markDelivered(
            cardId = cardId,
            scheduleKey = scheduleKey,
            deliveredAt = Instant.now()
        )
        return Result.success()
    }

    private fun createContentIntent(cardId: String): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = ExpirationReminderIntents.ACTION_OPEN_REMINDER_CARD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ExpirationReminderIntents.EXTRA_CARD_ID, cardId)
        }
        return PendingIntent.getActivity(
            applicationContext,
            cardId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            applicationContext.getString(R.string.expiration_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = applicationContext.getString(
                R.string.expiration_reminder_channel_description
            )
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ExpirationReminderWorkerEntryPoint {
        fun expirationReminderStateDao(): ExpirationReminderStateDao
        fun appSettingsDao(): AppSettingsDao
    }

    companion object {
        const val INPUT_CARD_ID = "input_card_id"
        const val INPUT_CARD_NAME = "input_card_name"
        const val INPUT_EXPIRATION_DATE = "input_expiration_date"
        const val INPUT_SCHEDULE_KEY = "input_schedule_key"
        const val REMINDER_WORK_TAG = "expiration_reminder_work"

        private const val NOTIFICATION_CHANNEL_ID = "expiration_reminders"
    }
}
