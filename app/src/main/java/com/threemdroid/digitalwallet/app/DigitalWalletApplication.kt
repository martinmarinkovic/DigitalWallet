package com.threemdroid.digitalwallet.app

import android.app.Application
import android.os.Build
import com.threemdroid.digitalwallet.data.reminder.ExpirationReminderSyncer
import com.threemdroid.digitalwallet.data.sync.CloudSyncSyncer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class DigitalWalletApplication : Application() {
    @Inject
    lateinit var expirationReminderSyncerProvider: Provider<ExpirationReminderSyncer>
    @Inject
    lateinit var cloudSyncSyncerProvider: Provider<CloudSyncSyncer>

    override fun onCreate() {
        super.onCreate()
        if (Build.FINGERPRINT != "robolectric") {
            expirationReminderSyncerProvider.get().start()
            cloudSyncSyncerProvider.get().start()
        }
    }
}
