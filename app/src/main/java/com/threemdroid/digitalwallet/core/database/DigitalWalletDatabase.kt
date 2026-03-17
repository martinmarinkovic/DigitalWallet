package com.threemdroid.digitalwallet.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.threemdroid.digitalwallet.core.database.dao.AppSettingsDao
import com.threemdroid.digitalwallet.core.database.dao.CardDao
import com.threemdroid.digitalwallet.core.database.dao.CategoryDao
import com.threemdroid.digitalwallet.core.database.dao.CloudSyncStateDao
import com.threemdroid.digitalwallet.core.database.dao.ExpirationReminderStateDao
import com.threemdroid.digitalwallet.core.database.dao.PendingSyncChangeDao
import com.threemdroid.digitalwallet.core.database.dao.SearchHistoryDao
import com.threemdroid.digitalwallet.core.database.entity.AppSettingsEntity
import com.threemdroid.digitalwallet.core.database.entity.CardEntity
import com.threemdroid.digitalwallet.core.database.entity.CategoryEntity
import com.threemdroid.digitalwallet.core.database.entity.CloudSyncStateEntity
import com.threemdroid.digitalwallet.core.database.entity.ExpirationReminderStateEntity
import com.threemdroid.digitalwallet.core.database.entity.PendingSyncChangeEntity
import com.threemdroid.digitalwallet.core.database.entity.SearchHistoryEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        CardEntity::class,
        SearchHistoryEntity::class,
        AppSettingsEntity::class,
        ExpirationReminderStateEntity::class,
        PendingSyncChangeEntity::class,
        CloudSyncStateEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(DigitalWalletTypeConverters::class)
abstract class DigitalWalletDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun expirationReminderStateDao(): ExpirationReminderStateDao
    abstract fun pendingSyncChangeDao(): PendingSyncChangeDao
    abstract fun cloudSyncStateDao(): CloudSyncStateDao

    companion object {
        const val DATABASE_NAME = "digital_wallet.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expiration_reminder_states` (
                        `card_id` TEXT NOT NULL,
                        `schedule_key` TEXT NOT NULL,
                        `scheduled_at` INTEGER NOT NULL,
                        `delivered_at` INTEGER,
                        PRIMARY KEY(`card_id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_sync_changes` (
                        `change_key` TEXT NOT NULL,
                        `entity_type` TEXT NOT NULL,
                        `entity_id` TEXT NOT NULL,
                        `change_type` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`change_key`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cloud_sync_state` (
                        `id` INTEGER NOT NULL,
                        `phase` TEXT NOT NULL,
                        `last_sync_attempt_at` INTEGER,
                        `last_successful_sync_at` INTEGER,
                        `last_error_message` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
