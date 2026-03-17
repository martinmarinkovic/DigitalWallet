package com.threemdroid.digitalwallet.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.threemdroid.digitalwallet.core.database.entity.ExpirationReminderStateEntity
import java.time.Instant

@Dao
interface ExpirationReminderStateDao {
    @Query("SELECT * FROM expiration_reminder_states")
    suspend fun getAllStates(): List<ExpirationReminderStateEntity>

    @Query("SELECT * FROM expiration_reminder_states WHERE card_id = :cardId LIMIT 1")
    suspend fun getState(cardId: String): ExpirationReminderStateEntity?

    @Upsert
    suspend fun upsertState(state: ExpirationReminderStateEntity)

    @Query("DELETE FROM expiration_reminder_states WHERE card_id = :cardId")
    suspend fun deleteState(cardId: String)

    @Query("DELETE FROM expiration_reminder_states")
    suspend fun deleteAllStates()

    @Query(
        """
        UPDATE expiration_reminder_states
        SET delivered_at = :deliveredAt
        WHERE card_id = :cardId AND schedule_key = :scheduleKey
        """
    )
    suspend fun markDelivered(
        cardId: String,
        scheduleKey: String,
        deliveredAt: Instant
    ): Int
}
