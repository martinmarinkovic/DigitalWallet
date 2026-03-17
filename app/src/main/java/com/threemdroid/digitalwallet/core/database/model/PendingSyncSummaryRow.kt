package com.threemdroid.digitalwallet.core.database.model

import androidx.room.ColumnInfo
import java.time.Instant

data class PendingSyncSummaryRow(
    @ColumnInfo(name = "pending_count")
    val pendingCount: Int,
    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Instant?
)
