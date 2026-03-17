package com.threemdroid.digitalwallet.data

import androidx.room.Room
import com.threemdroid.digitalwallet.core.database.DigitalWalletDatabase
import com.threemdroid.digitalwallet.core.model.AppSettings
import com.threemdroid.digitalwallet.core.model.CardCodeType
import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.ReminderTiming
import com.threemdroid.digitalwallet.core.model.SearchHistoryEntry
import com.threemdroid.digitalwallet.core.model.ThemeMode
import com.threemdroid.digitalwallet.core.model.WalletCard
import java.time.Instant
import java.time.LocalDate
import org.junit.After
import org.junit.Before
import org.robolectric.RuntimeEnvironment

abstract class BaseRepositoryTest {
    protected lateinit var database: DigitalWalletDatabase

    @Before
    fun setUpDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DigitalWalletDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownDatabase() {
        database.close()
    }

    protected fun category(
        id: String,
        name: String,
        position: Int,
        color: String = "#336699",
        isDefault: Boolean = false,
        isFavorites: Boolean = false,
        createdAt: Instant = Instant.parse("2026-03-13T10:00:00Z"),
        updatedAt: Instant = createdAt
    ): Category =
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

    protected fun card(
        id: String,
        categoryId: String,
        position: Int,
        name: String = "Card $id",
        codeValue: String = "CODE-$id",
        codeType: CardCodeType = CardCodeType.QR_CODE,
        cardNumber: String? = "NUM-$id",
        expirationDate: LocalDate? = LocalDate.parse("2026-12-31"),
        notes: String? = "Notes for $id",
        isFavorite: Boolean = false,
        createdAt: Instant = Instant.parse("2026-03-13T10:00:00Z"),
        updatedAt: Instant = createdAt
    ): WalletCard =
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

    protected fun searchHistoryEntry(
        id: Long = 0L,
        query: String,
        createdAt: Instant = Instant.parse("2026-03-13T10:00:00Z")
    ): SearchHistoryEntry =
        SearchHistoryEntry(
            id = id,
            query = query,
            createdAt = createdAt
        )

    protected fun appSettings(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        autoBrightnessEnabled: Boolean = true,
        reminderEnabled: Boolean = true,
        reminderTiming: ReminderTiming = ReminderTiming.ON_DAY,
        cloudSyncEnabled: Boolean = false
    ): AppSettings =
        AppSettings(
            themeMode = themeMode,
            autoBrightnessEnabled = autoBrightnessEnabled,
            reminderEnabled = reminderEnabled,
            reminderTiming = reminderTiming,
            cloudSyncEnabled = cloudSyncEnabled
        )
}
