package com.threemdroid.digitalwallet.data.transfer

import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.core.model.WalletCard
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

class UserDataBackupJsonCodec @Inject constructor() {

    fun encode(snapshot: UserDataBackupSnapshot): String =
        JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", snapshot.exportedAt.toString())
            .put(
                "categories",
                JSONArray().apply {
                    snapshot.categories
                        .sortedWith(compareBy<Category> { it.position }.thenBy { it.createdAt })
                        .forEach { category ->
                            put(
                                JSONObject()
                                    .put("id", category.id)
                                    .put("name", category.name)
                                    .put("color", category.color)
                                    .put("isDefault", category.isDefault)
                                    .put("isFavorites", category.isFavorites)
                                    .put("position", category.position)
                                    .put("createdAt", category.createdAt.toString())
                                    .put("updatedAt", category.updatedAt.toString())
                            )
                        }
                }
            )
            .put(
                "cards",
                JSONArray().apply {
                    snapshot.cards
                        .sortedWith(
                            compareBy<WalletCard> { it.categoryId }
                                .thenBy { it.position }
                                .thenBy { it.createdAt }
                        )
                        .forEach { card ->
                            put(
                                JSONObject()
                                    .put("id", card.id)
                                    .put("name", card.name)
                                    .put("categoryId", card.categoryId)
                                    .put("codeValue", card.codeValue)
                                    .put("codeType", card.codeType.name)
                                    .put("cardNumber", card.cardNumber?.let { value -> value } ?: JSONObject.NULL)
                                    .put(
                                        "expirationDate",
                                        card.expirationDate?.toString() ?: JSONObject.NULL
                                    )
                                    .put("notes", card.notes?.let { value -> value } ?: JSONObject.NULL)
                                    .put("isFavorite", card.isFavorite)
                                    .put("position", card.position)
                                    .put("createdAt", card.createdAt.toString())
                                    .put("updatedAt", card.updatedAt.toString())
                            )
                        }
                }
            )
            .put(
                "settings",
                JSONObject()
                    .put("themeMode", snapshot.settings.themeMode.name)
                    .put("autoBrightnessEnabled", snapshot.settings.autoBrightnessEnabled)
                    .put("reminderEnabled", snapshot.settings.reminderEnabled)
                    .put("reminderTiming", snapshot.settings.reminderTiming.name)
                    .put("cloudSyncEnabled", snapshot.settings.cloudSyncEnabled)
            )
            .put(
                "searchHistory",
                JSONArray().apply {
                    snapshot.searchHistory
                        .sortedWith(compareByDescending<SearchHistoryEntry> { it.createdAt }.thenByDescending { it.id })
                        .forEach { entry ->
                            put(
                                JSONObject()
                                    .put("id", entry.id)
                                    .put("query", entry.query)
                                    .put("createdAt", entry.createdAt.toString())
                            )
                        }
                }
            )
            .toString(2)

    fun decode(json: String): UserDataBackupSnapshot {
        val root = JSONObject(json)
        require(root.optString("format") == BACKUP_FORMAT) {
            "Unsupported backup format."
        }
        require(root.optInt("schemaVersion", -1) == SCHEMA_VERSION) {
            "Unsupported backup version."
        }

        val snapshot = UserDataBackupSnapshot(
            exportedAt = Instant.parse(root.requiredString("exportedAt")),
            categories = root.optJSONArray("categories").toCategoryList(),
            cards = root.optJSONArray("cards").toCardList(),
            settings = root.optJSONObject("settings").toAppSettings(),
            searchHistory = root.optJSONArray("searchHistory").toSearchHistoryList()
        )

        validate(snapshot)
        return snapshot
    }

    private fun JSONArray?.toCategoryList(): List<Category> {
        if (this == null) {
            return emptyList()
        }

        return List(length()) { index ->
            val item = getJSONObject(index)
            Category(
                id = item.requiredString("id"),
                name = item.requiredString("name"),
                color = item.requiredString("color"),
                isDefault = item.optBoolean("isDefault", false),
                isFavorites = item.optBoolean("isFavorites", false),
                position = item.optInt("position", index),
                createdAt = Instant.parse(item.requiredString("createdAt")),
                updatedAt = Instant.parse(item.requiredString("updatedAt"))
            )
        }
    }

    private fun JSONArray?.toCardList(): List<WalletCard> {
        if (this == null) {
            return emptyList()
        }

        return List(length()) { index ->
            val item = getJSONObject(index)
            WalletCard(
                id = item.requiredString("id"),
                name = item.requiredString("name"),
                categoryId = item.requiredString("categoryId"),
                codeValue = item.requiredString("codeValue"),
                codeType = item.optString("codeType").toCardCodeType(),
                cardNumber = item.optNullableString("cardNumber"),
                expirationDate = item.optNullableString("expirationDate")?.let(LocalDate::parse),
                notes = item.optNullableString("notes"),
                isFavorite = item.optBoolean("isFavorite", false),
                position = item.optInt("position", index),
                createdAt = Instant.parse(item.requiredString("createdAt")),
                updatedAt = Instant.parse(item.requiredString("updatedAt"))
            )
        }
    }

    private fun JSONObject?.toAppSettings(): AppSettings {
        if (this == null) {
            return AppSettings()
        }

        return AppSettings(
            themeMode = optString("themeMode").toThemeMode(),
            autoBrightnessEnabled = optBoolean("autoBrightnessEnabled", true),
            reminderEnabled = optBoolean("reminderEnabled", true),
            reminderTiming = optString("reminderTiming").toReminderTiming(),
            cloudSyncEnabled = optBoolean("cloudSyncEnabled", false)
        )
    }

    private fun JSONArray?.toSearchHistoryList(): List<SearchHistoryEntry> {
        if (this == null) {
            return emptyList()
        }

        return List(length()) { index ->
            val item = getJSONObject(index)
            SearchHistoryEntry(
                id = item.optLong("id", 0L),
                query = item.requiredString("query"),
                createdAt = Instant.parse(item.requiredString("createdAt"))
            )
        }
    }

    private fun validate(snapshot: UserDataBackupSnapshot) {
        val categoryIds = snapshot.categories.map { category -> category.id }
        require(categoryIds.distinct().size == categoryIds.size) {
            "Backup contains duplicate category ids."
        }

        val cardIds = snapshot.cards.map { card -> card.id }
        require(cardIds.distinct().size == cardIds.size) {
            "Backup contains duplicate card ids."
        }

        val availableCategoryIds = categoryIds.toSet()
        require(snapshot.cards.all { card -> card.categoryId in availableCategoryIds }) {
            "Backup contains cards without a valid category."
        }

        val searchQueries = snapshot.searchHistory.map { entry -> entry.query }
        require(searchQueries.distinct().size == searchQueries.size) {
            "Backup contains duplicate search-history queries."
        }
    }

    private fun JSONObject.requiredString(key: String): String =
        optString(key).takeIf { value -> value.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: $key")

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) {
            null
        } else {
            optString(key).takeIf { value -> value.isNotEmpty() }
        }

    private fun String.toCardCodeType(): CardCodeType =
        runCatching { CardCodeType.valueOf(this) }.getOrDefault(CardCodeType.OTHER)

    private fun String.toThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(this) }.getOrDefault(ThemeMode.SYSTEM)

    private fun String.toReminderTiming(): ReminderTiming =
        runCatching { ReminderTiming.valueOf(this) }.getOrDefault(ReminderTiming.ON_DAY)

    companion object {
        private const val BACKUP_FORMAT = "digital_wallet_backup"
        private const val SCHEMA_VERSION = 1
    }
}
