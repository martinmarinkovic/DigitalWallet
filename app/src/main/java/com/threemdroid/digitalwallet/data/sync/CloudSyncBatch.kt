package com.threemdroid.digitalwallet.data.sync

import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.core.model.WalletCard

data class CloudSyncBatch(
    val categoriesToUpsert: List<Category>,
    val categoryIdsToDelete: List<String>,
    val cardsToUpsert: List<WalletCard>,
    val cardIdsToDelete: List<String>,
    val appSettingsToUpsert: SyncableAppSettings?
)

data class SyncableAppSettings(
    val themeMode: ThemeMode,
    val autoBrightnessEnabled: Boolean,
    val reminderEnabled: Boolean,
    val reminderTiming: ReminderTiming
)
