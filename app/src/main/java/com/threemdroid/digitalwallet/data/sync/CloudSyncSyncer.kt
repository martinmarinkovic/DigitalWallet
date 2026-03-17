package com.threemdroid.digitalwallet.data.sync

import com.threemdroid.digitalwallet.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class CloudSyncSyncer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var syncJob: Job? = null

    fun start() {
        if (syncJob != null) {
            return
        }

        syncJob = scope.launch {
            combine(
                settingsRepository.observeSettings().map { settings ->
                    settings.cloudSyncEnabled
                },
                syncRepository.observePendingSyncFingerprint()
            ) { cloudSyncEnabled, pendingFingerprint ->
                CloudSyncPlan(
                    cloudSyncEnabled = cloudSyncEnabled,
                    pendingFingerprint = pendingFingerprint
                )
            }
                .distinctUntilChanged()
                .collect { plan ->
                    syncRepository.updateSyncEnabled(plan.cloudSyncEnabled)
                    if (plan.cloudSyncEnabled) {
                        syncRepository.syncPendingChanges()
                    }
                }
        }
    }
}

private data class CloudSyncPlan(
    val cloudSyncEnabled: Boolean,
    val pendingFingerprint: PendingSyncFingerprint
)
