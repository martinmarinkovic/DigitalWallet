package com.threemdroid.digitalwallet.core.database.mapper

import com.threemdroid.digitalwallet.core.database.entity.AppSettingsEntity
import com.threemdroid.digitalwallet.core.database.entity.CardEntity
import com.threemdroid.digitalwallet.core.database.entity.CategoryEntity
import com.threemdroid.digitalwallet.core.database.entity.CloudSyncStateEntity
import com.threemdroid.digitalwallet.core.database.entity.SearchHistoryEntity
import com.threemdroid.digitalwallet.core.database.model.CategoryWithCardCountRow
import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.CategoryWithCardCount
import com.threemdroid.digitalwallet.core.model.CloudSyncStatus
import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import com.threemdroid.digitalwallet.core.model.WalletCard

fun CategoryEntity.asExternalModel(): Category =
    Category(
        id = id,
        name = name,
        color = color,
        isDefault = isDefault,
        isFavorites = isFavorites,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun Category.asEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        color = color,
        isDefault = isDefault,
        isFavorites = isFavorites,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun CategoryWithCardCountRow.asExternalModel(): CategoryWithCardCount =
    CategoryWithCardCount(
        category = category.asExternalModel(),
        cardCount = cardCount
    )

fun CardEntity.asExternalModel(): WalletCard =
    WalletCard(
        id = id,
        name = name,
        categoryId = categoryId,
        codeValue = codeValue,
        codeType = codeType,
        cardNumber = cardNumber,
        expirationDate = expirationDate,
        notes = notes,
        isFavorite = isFavorite,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun WalletCard.asEntity(): CardEntity =
    CardEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        codeValue = codeValue,
        codeType = codeType,
        cardNumber = cardNumber,
        expirationDate = expirationDate,
        notes = notes,
        isFavorite = isFavorite,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun SearchHistoryEntity.asExternalModel(): SearchHistoryEntry =
    SearchHistoryEntry(
        id = id,
        query = query,
        createdAt = createdAt
    )

fun SearchHistoryEntry.asEntity(): SearchHistoryEntity =
    SearchHistoryEntity(
        id = id,
        query = query,
        createdAt = createdAt
    )

fun AppSettingsEntity.asExternalModel(): AppSettings =
    AppSettings(
        themeMode = themeMode,
        autoBrightnessEnabled = autoBrightnessEnabled,
        reminderEnabled = reminderEnabled,
        reminderTiming = reminderTiming,
        cloudSyncEnabled = cloudSyncEnabled
    )

fun AppSettings.asEntity(): AppSettingsEntity =
    AppSettingsEntity(
        themeMode = themeMode,
        autoBrightnessEnabled = autoBrightnessEnabled,
        reminderEnabled = reminderEnabled,
        reminderTiming = reminderTiming,
        cloudSyncEnabled = cloudSyncEnabled
    )

fun CloudSyncStateEntity.asExternalModel(): CloudSyncStatus =
    CloudSyncStatus(
        phase = phase,
        lastSyncAttemptAt = lastSyncAttemptAt,
        lastSuccessfulSyncAt = lastSuccessfulSyncAt,
        lastErrorMessage = lastErrorMessage
    )
