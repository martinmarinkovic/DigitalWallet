package com.threemdroid.digitalwallet.core.model

import java.time.Instant

data class CloudSyncStatus(
    val phase: CloudSyncPhase = CloudSyncPhase.DISABLED,
    val pendingChangeCount: Int = 0,
    val lastSyncAttemptAt: Instant? = null,
    val lastSuccessfulSyncAt: Instant? = null,
    val lastErrorMessage: String? = null
)

enum class CloudSyncPhase {
    DISABLED,
    IDLE,
    SYNCING,
    BACKEND_UNAVAILABLE,
    FAILED
}
