package com.threemdroid.digitalwallet.core.model

import java.time.Instant

data class SearchHistoryEntry(
    val id: Long = 0L,
    val query: String,
    val createdAt: Instant
)
