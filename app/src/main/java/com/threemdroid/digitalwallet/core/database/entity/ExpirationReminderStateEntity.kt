package com.threemdroid.digitalwallet.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "expiration_reminder_states")
data class ExpirationReminderStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "card_id")
    val cardId: String,
    @ColumnInfo(name = "schedule_key")
    val scheduleKey: String,
    @ColumnInfo(name = "scheduled_at")
    val scheduledAt: Instant,
    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Instant?
)
