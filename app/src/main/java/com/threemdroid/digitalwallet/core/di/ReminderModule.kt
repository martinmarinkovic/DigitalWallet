package com.threemdroid.digitalwallet.core.di

import android.content.Context
import androidx.work.WorkManager
import com.threemdroid.digitalwallet.data.reminder.ExpirationReminderScheduler
import com.threemdroid.digitalwallet.data.reminder.ExpirationReminderWorkBackend
import com.threemdroid.digitalwallet.data.reminder.ReminderNotificationAvailabilityMonitor
import com.threemdroid.digitalwallet.data.reminder.SystemReminderNotificationAvailabilityMonitor
import com.threemdroid.digitalwallet.data.reminder.WorkManagerExpirationReminderScheduler
import com.threemdroid.digitalwallet.data.reminder.WorkManagerExpirationReminderWorkBackend
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReminderProvidersModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderBindingsModule {
    @Binds
    @Singleton
    abstract fun bindExpirationReminderScheduler(
        scheduler: WorkManagerExpirationReminderScheduler
    ): ExpirationReminderScheduler

    @Binds
    @Singleton
    abstract fun bindExpirationReminderWorkBackend(
        backend: WorkManagerExpirationReminderWorkBackend
    ): ExpirationReminderWorkBackend

    @Binds
    @Singleton
    abstract fun bindReminderNotificationAvailabilityMonitor(
        monitor: SystemReminderNotificationAvailabilityMonitor
    ): ReminderNotificationAvailabilityMonitor
}
