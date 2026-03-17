package com.threemdroid.digitalwallet.core.database

import androidx.room.TypeConverter
import com.threemdroid.digitalwallet.core.model.CloudSyncPhase
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.data.sync.SyncChangeType
import com.threemdroid.digitalwallet.data.sync.SyncEntityType
import java.time.Instant
import java.time.LocalDate

class DigitalWalletTypeConverters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.let { rawValue ->
            runCatching { LocalDate.parse(rawValue) }.getOrNull()
        }

    @TypeConverter
    fun fromThemeMode(value: ThemeMode?): String? = value?.name

    @TypeConverter
    fun toThemeMode(value: String?): ThemeMode =
        value
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    @TypeConverter
    fun fromReminderTiming(value: ReminderTiming?): String? = value?.name

    @TypeConverter
    fun toReminderTiming(value: String?): ReminderTiming =
        value
            ?.let { runCatching { ReminderTiming.valueOf(it) }.getOrNull() }
            ?: ReminderTiming.ON_DAY

    @TypeConverter
    fun fromCardCodeType(value: CardCodeType?): String? = value?.name

    @TypeConverter
    fun toCardCodeType(value: String?): CardCodeType =
        value
            ?.let { runCatching { CardCodeType.valueOf(it) }.getOrNull() }
            ?: CardCodeType.OTHER

    @TypeConverter
    fun fromCloudSyncPhase(value: CloudSyncPhase?): String? = value?.name

    @TypeConverter
    fun toCloudSyncPhase(value: String?): CloudSyncPhase =
        value
            ?.let { runCatching { CloudSyncPhase.valueOf(it) }.getOrNull() }
            ?: CloudSyncPhase.DISABLED

    @TypeConverter
    fun fromSyncEntityType(value: SyncEntityType?): String? = value?.name

    @TypeConverter
    fun toSyncEntityType(value: String?): SyncEntityType =
        value
            ?.let { runCatching { SyncEntityType.valueOf(it) }.getOrNull() }
            ?: SyncEntityType.CATEGORY

    @TypeConverter
    fun fromSyncChangeType(value: SyncChangeType?): String? = value?.name

    @TypeConverter
    fun toSyncChangeType(value: String?): SyncChangeType =
        value
            ?.let { runCatching { SyncChangeType.valueOf(it) }.getOrNull() }
            ?: SyncChangeType.UPSERT
}
