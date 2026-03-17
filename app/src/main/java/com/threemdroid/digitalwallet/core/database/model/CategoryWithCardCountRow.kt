package com.threemdroid.digitalwallet.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.threemdroid.digitalwallet.core.database.entity.CategoryEntity

data class CategoryWithCardCountRow(
    @Embedded
    val category: CategoryEntity,
    @ColumnInfo(name = "card_count")
    val cardCount: Int
)
