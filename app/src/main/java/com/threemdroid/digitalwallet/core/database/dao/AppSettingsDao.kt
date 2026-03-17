package com.threemdroid.digitalwallet.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.threemdroid.digitalwallet.core.database.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = :settingsId LIMIT 1")
    fun observeSettings(settingsId: Int = AppSettingsEntity.SINGLETON_ID): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = :settingsId LIMIT 1")
    suspend fun getSettings(settingsId: Int = AppSettingsEntity.SINGLETON_ID): AppSettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAllSettings()
}
