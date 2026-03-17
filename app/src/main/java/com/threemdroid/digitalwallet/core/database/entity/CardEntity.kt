package com.threemdroid.digitalwallet.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.threemdroid.digitalwallet.core.model.CardCodeType
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "wallet_cards",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["category_id", "position"]),
        Index(value = ["is_favorite"])
    ]
)
data class CardEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "code_value")
    val codeValue: String,
    @ColumnInfo(name = "code_type")
    val codeType: CardCodeType,
    @ColumnInfo(name = "card_number")
    val cardNumber: String?,
    @ColumnInfo(name = "expiration_date")
    val expirationDate: LocalDate?,
    val notes: String?,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean,
    val position: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
