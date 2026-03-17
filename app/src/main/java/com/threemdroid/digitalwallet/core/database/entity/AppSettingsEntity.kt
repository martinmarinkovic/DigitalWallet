package com.threemdroid.digitalwallet.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "theme_mode")
    val themeMode: ThemeMode,
    @ColumnInfo(name = "auto_brightness_enabled")
    val autoBrightnessEnabled: Boolean,
    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean,
    @ColumnInfo(name = "reminder_timing")
    val reminderTiming: ReminderTiming,
    @ColumnInfo(name = "cloud_sync_enabled")
    val cloudSyncEnabled: Boolean
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
