package com.threemdroid.digitalwallet.core.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoBrightnessEnabled: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderTiming: ReminderTiming = ReminderTiming.ON_DAY,
    val cloudSyncEnabled: Boolean = false
)
