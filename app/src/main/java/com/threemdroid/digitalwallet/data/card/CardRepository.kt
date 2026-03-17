package com.threemdroid.digitalwallet.data.card

import com.threemdroid.digitalwallet.core.model.WalletCard
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun observeAllCards(): Flow<List<WalletCard>>

    fun observeCards(categoryId: String): Flow<List<WalletCard>>

    fun observeCard(cardId: String): Flow<WalletCard?>

    suspend fun upsertCard(card: WalletCard)

    suspend fun upsertCards(cards: List<WalletCard>)

    suspend fun updateCardOrder(categoryId: String, cardIdsInOrder: List<String>)

    suspend fun deleteCard(cardId: String)
}
