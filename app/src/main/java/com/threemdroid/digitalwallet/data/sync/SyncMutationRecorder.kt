package com.threemdroid.digitalwallet.data.sync

interface SyncMutationRecorder {
    suspend fun recordCategoryUpserts(categoryIds: Collection<String>)

    suspend fun recordCategoryDeletes(categoryIds: Collection<String>)

    suspend fun recordCardUpserts(cardIds: Collection<String>)

    suspend fun recordCardDeletes(cardIds: Collection<String>)

    suspend fun recordAppSettingsUpsert()

    companion object {
        val NO_OP: SyncMutationRecorder = object : SyncMutationRecorder {
            override suspend fun recordCategoryUpserts(categoryIds: Collection<String>) = Unit

            override suspend fun recordCategoryDeletes(categoryIds: Collection<String>) = Unit

            override suspend fun recordCardUpserts(cardIds: Collection<String>) = Unit

            override suspend fun recordCardDeletes(cardIds: Collection<String>) = Unit

            override suspend fun recordAppSettingsUpsert() = Unit
        }
    }
}
