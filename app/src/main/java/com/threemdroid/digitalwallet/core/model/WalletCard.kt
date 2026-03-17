package com.threemdroid.digitalwallet.core.model

import java.time.Instant
import java.time.LocalDate

data class WalletCard(
    val id: String,
    val name: String,
    val categoryId: String,
    val codeValue: String,
    val codeType: CardCodeType,
    val cardNumber: String?,
    val expirationDate: LocalDate?,
    val notes: String?,
    val isFavorite: Boolean,
    val position: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
