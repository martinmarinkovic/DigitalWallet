package com.threemdroid.digitalwallet.data.transfer

import com.threemdroid.digitalwallet.core.model.Category
import com.threemdroid.digitalwallet.core.model.WalletCard
import javax.inject.Inject

class CardCsvExportFormatter @Inject constructor() {

    fun format(
        categories: List<Category>,
        cards: List<WalletCard>
    ): String {
        val categoriesById = categories.associateBy { category -> category.id }
        val rows = buildList {
            add(
                listOf(
                    "card_name",
                    "category_name",
                    "barcode_type",
                    "barcode_or_qr_value",
                    "card_number",
                    "expiration_date",
                    "notes",
                    "favorite",
                    "category_position",
                    "card_position",
                    "created_at",
                    "updated_at"
                )
            )

            cards
                .sortedWith(
                    compareBy<WalletCard> { card ->
                        categoriesById[card.categoryId]?.position ?: Int.MAX_VALUE
                    }
                        .thenBy { card -> card.position }
                        .thenBy { card -> card.createdAt }
                )
                .forEach { card ->
                    val category = categoriesById[card.categoryId]
                    add(
                        listOf(
                            card.name,
                            category?.name.orEmpty(),
                            card.codeType.name,
                            card.codeValue,
                            card.cardNumber.orEmpty(),
                            card.expirationDate?.toString().orEmpty(),
                            card.notes.orEmpty(),
                            card.isFavorite.toString(),
                            category?.position?.toString().orEmpty(),
                            card.position.toString(),
                            card.createdAt.toString(),
                            card.updatedAt.toString()
                        )
                    )
                }
        }

        return rows.joinToString(separator = "\n") { row ->
            row.joinToString(separator = ",") { cell -> cell.toCsvCell() }
        }
    }

    private fun String.toCsvCell(): String {
        val sanitized = sanitizeForSpreadsheet(this)
        val escaped = sanitized.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun sanitizeForSpreadsheet(value: String): String =
        if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@")) {
            "'$value"
        } else {
            value
        }
}
