package com.threemdroid.digitalwallet.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.threemdroid.digitalwallet.core.database.entity.CardEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM wallet_cards ORDER BY created_at ASC, id ASC")
    fun observeAllCards(): Flow<List<CardEntity>>

    @Query(
        """
        SELECT * FROM wallet_cards
        WHERE category_id = :categoryId
        ORDER BY position ASC, created_at ASC
        """
    )
    fun observeCardsByCategory(categoryId: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM wallet_cards WHERE id = :cardId LIMIT 1")
    fun observeCard(cardId: String): Flow<CardEntity?>

    @Query("SELECT id FROM wallet_cards WHERE category_id = :categoryId")
    suspend fun getIdsForCategory(categoryId: String): List<String>

    @Query("SELECT id FROM wallet_cards")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM wallet_cards ORDER BY category_id ASC, position ASC, created_at ASC")
    suspend fun getAllCards(): List<CardEntity>

    @Query("SELECT * FROM wallet_cards WHERE id IN (:cardIds)")
    suspend fun getCardsByIds(cardIds: List<String>): List<CardEntity>

    @Upsert
    suspend fun upsertCard(card: CardEntity)

    @Upsert
    suspend fun upsertCards(cards: List<CardEntity>)

    @Query("DELETE FROM wallet_cards")
    suspend fun deleteAllCards()

    @Query(
        """
        UPDATE wallet_cards
        SET position = :position, updated_at = :updatedAt
        WHERE id = :cardId AND category_id = :categoryId
        """
    )
    suspend fun updateCardPosition(
        cardId: String,
        categoryId: String,
        position: Int,
        updatedAt: Instant
    )

    @Query("DELETE FROM wallet_cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: String)
}
