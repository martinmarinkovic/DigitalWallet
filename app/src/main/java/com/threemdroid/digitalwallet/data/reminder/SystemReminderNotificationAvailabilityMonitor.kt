package com.threemdroid.digitalwallet.data.reminder

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SystemReminderNotificationAvailabilityMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ReminderNotificationAvailabilityMonitor {
    private val canPostNotifications = MutableStateFlow(context.canPostReminderNotifications())

    init {
        (context as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    canPostNotifications.value = context.canPostReminderNotifications()
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }

    override fun observeCanPostNotifications(): Flow<Boolean> = canPostNotifications.asStateFlow()
}

private fun Context.canPostReminderNotifications(): Boolean {
    if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
        return false
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }

    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}
