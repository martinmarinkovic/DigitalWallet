package com.threemdroid.digitalwallet.data.card

import androidx.room.withTransaction
import com.threemdroid.digitalwallet.core.database.DigitalWalletDatabase
import com.threemdroid.digitalwallet.core.database.dao.CardDao
import com.threemdroid.digitalwallet.core.database.dao.CategoryDao
import com.threemdroid.digitalwallet.core.database.mapper.asEntity
import com.threemdroid.digitalwallet.core.database.mapper.asExternalModel
import com.threemdroid.digitalwallet.core.model.WalletCard
import com.threemdroid.digitalwallet.data.sync.SyncMutationRecorder
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstCardRepository @Inject constructor(
    private val database: DigitalWalletDatabase,
    private val cardDao: CardDao,
    private val categoryDao: CategoryDao,
    private val syncMutationRecorder: SyncMutationRecorder = SyncMutationRecorder.NO_OP
) : CardRepository {

    override fun observeAllCards(): Flow<List<WalletCard>> =
        cardDao.observeAllCards().map { cards ->
            cards.map { it.asExternalModel() }
        }

    override fun observeCards(categoryId: String): Flow<List<WalletCard>> =
        cardDao.observeCardsByCategory(categoryId).map { cards ->
            cards.map { it.asExternalModel() }
        }

    override fun observeCard(cardId: String): Flow<WalletCard?> =
        cardDao.observeCard(cardId).map { card ->
            card?.asExternalModel()
        }

    override suspend fun upsertCard(card: WalletCard) {
        validateCategoryIds(setOf(card.categoryId))
        database.withTransaction {
            cardDao.upsertCard(card.asEntity())
            syncMutationRecorder.recordCardUpserts(listOf(card.id))
        }
    }

    override suspend fun upsertCards(cards: List<WalletCard>) {
        if (cards.isEmpty()) {
            return
        }

        validateCategoryIds(cards.map { it.categoryId }.toSet())
        database.withTransaction {
            cardDao.upsertCards(cards.map { it.asEntity() })
            syncMutationRecorder.recordCardUpserts(cards.map { card -> card.id })
        }
    }

    override suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>) {
        val existingIds = cardDao.getIdsForCategory(categoryId)
        require(
            existingIds.size == cardIdsInOrder.size &&
                existingIds.toSet() == cardIdsInOrder.toSet()
        ) {
            "Card order update must include every card in the category exactly once."
        }

        database.withTransaction {
            val updatedAt = Instant.now()
            cardIdsInOrder.forEachIndexed { index, cardId ->
                cardDao.updateCardPosition(
                    cardId = cardId,
                    categoryId = categoryId,
                    position = index,
                    updatedAt = updatedAt
                )
            }
            syncMutationRecorder.recordCardUpserts(cardIdsInOrder)
        }
    }

    override suspend fun deleteCard(cardId: String) {
        database.withTransaction {
            cardDao.deleteCard(cardId)
            syncMutationRecorder.recordCardDeletes(listOf(cardId))
        }
    }

    private suspend fun validateCategoryIds(categoryIds: Set<String>) {
        if (categoryIds.isEmpty()) {
            return
        }

        val existingIds = categoryDao.getExistingIds(categoryIds.toList())
        require(existingIds.toSet() == categoryIds) {
            "Every card must reference an existing category."
        }
    }
}
