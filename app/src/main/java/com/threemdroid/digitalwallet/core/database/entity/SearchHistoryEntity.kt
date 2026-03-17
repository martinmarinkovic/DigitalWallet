package com.threemdroid.digitalwallet.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["query"], unique = true),
        Index(value = ["created_at"])
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val query: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)
