package com.threemdroid.digitalwallet.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["position"]),
        Index(value = ["is_favorites"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "is_favorites")
    val isFavorites: Boolean,
    val position: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
