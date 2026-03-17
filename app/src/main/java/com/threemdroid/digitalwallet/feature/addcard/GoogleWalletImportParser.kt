package com.threemdroid.digitalwallet.feature.addcard

import com.threemdroid.digitalwallet.core.model.CardCodeType
import javax.inject.Inject

data class GoogleWalletImportDraft(
    val codeType: CardCodeType?,
    val codeValue: String?,
    val cardNumber: String?,
    val cardName: String?,
    val notes: String?
)

class GoogleWalletImportTextParser @Inject constructor() {
    fun parse(rawInput: String): GoogleWalletImportDraft {
        val trimmedInput = rawInput.trim()
        val lines = trimmedInput
            .lines()
            .map { line -> line.trim() }
            .filter { line -> line.isNotBlank() }
        val cardNumber = lines.extractCardNumber()
        val codeValue = lines.extractCodeValue()
        val cardName = lines.extractCardName()
        val notes = trimmedInput.takeIf { value ->
            value.isNotBlank() &&
                (value.contains("http://", ignoreCase = true) ||
                    value.contains("https://", ignoreCase = true) ||
                    value.lines().count { line -> line.isNotBlank() } > 1 ||
                    codeValue == null)
        }

        return GoogleWalletImportDraft(
            codeType = codeValue?.let { CardCodeType.OTHER },
            codeValue = codeValue,
            cardNumber = cardNumber,
            cardName = cardName,
            notes = notes
        )
    }
}

private fun List<String>.extractCardNumber(): String? {
    val keywordMatches = asSequence()
        .filterNot { line -> line.containsAny(googleWalletImportExpiryKeywords) }
        .filter { line -> line.containsAny(googleWalletImportCardNumberKeywords) }
        .flatMap { line -> line.extractCardNumberCandidates().asSequence() }
        .map { candidate -> candidate.normalizeImportValue() }
        .filter { candidate -> candidate.isLikelyGoogleWalletCardNumber() }

    val fallbackMatches = asSequence()
        .filterNot { line -> line.containsAny(googleWalletImportExpiryKeywords) }
        .flatMap { line -> line.extractCardNumberCandidates().asSequence() }
        .map { candidate -> candidate.normalizeImportValue() }
        .filter { candidate -> candidate.isLikelyGoogleWalletCardNumber() }

    return (keywordMatches + fallbackMatches)
        .distinct()
        .firstOrNull()
}

private fun List<String>.extractCodeValue(): String? {
    val explicitKeywordMatch = asSequence()
        .filterNot { line -> line.contains("http://", ignoreCase = true) || line.contains("https://", ignoreCase = true) }
        .filter { line -> line.containsAny(googleWalletImportCodeKeywords) }
        .flatMap { line -> line.extractCodeCandidates().asSequence() }
        .map { candidate -> candidate.normalizeImportValue() }
        .filter { candidate -> candidate.isLikelyGoogleWalletCodeValue() }
        .firstOrNull()

    if (explicitKeywordMatch != null) {
        return explicitKeywordMatch
    }

    return asSequence()
        .filterNot { line ->
            line.contains("http://", ignoreCase = true) ||
                line.contains("https://", ignoreCase = true) ||
                line.containsAny(googleWalletImportCardNumberKeywords)
        }
        .flatMap { line -> line.extractCodeCandidates().asSequence() }
        .map { candidate -> candidate.normalizeImportValue() }
        .filter { candidate -> candidate.isLikelyGoogleWalletCodeValue() }
        .firstOrNull()
}

private fun List<String>.extractCardName(): String? =
    firstOrNull { line ->
        line.length in 3..60 &&
            line.none(Char::isDigit) &&
            line.count { character -> character.isLetter() } >= 3 &&
            !line.contains("http://", ignoreCase = true) &&
            !line.contains("https://", ignoreCase = true) &&
            !line.containsAny(googleWalletImportExcludedNameKeywords)
    }

private fun String.extractCardNumberCandidates(): List<String> {
    val sanitized = replace(googleWalletImportLabelRegex, " ")
    val segments = buildList {
        add(sanitized)
        addAll(sanitized.split(':', '#').drop(1))
    }

    return segments.flatMap { segment ->
        googleWalletImportCardNumberPattern.findAll(segment).map { match ->
            match.value
        }.toList()
    }
}

private fun String.extractCodeCandidates(): List<String> {
    val sanitized = replace(googleWalletImportLabelRegex, " ")
    val segments = buildList {
        add(sanitized)
        addAll(sanitized.split(':', '#').drop(1))
    }

    return segments.flatMap { segment ->
        googleWalletImportCodePattern.findAll(segment).map { match ->
            match.value
        }.toList()
    }
}

private fun String.normalizeImportValue(): String =
    replace(Regex("\\s+"), "")
        .trim('-')
        .trim()

private fun String.isLikelyGoogleWalletCardNumber(): Boolean =
    length in 4..24 &&
        count { character -> character.isDigit() } >= 4 &&
        !matches(googleWalletImportDatePattern)

private fun String.isLikelyGoogleWalletCodeValue(): Boolean =
    length in 6..80 &&
        !contains("http://", ignoreCase = true) &&
        !contains("https://", ignoreCase = true) &&
        count { character -> character.isLetterOrDigit() } >= 6

private fun String.containsAny(keywords: Set<String>): Boolean =
    keywords.any { keyword ->
        contains(keyword, ignoreCase = true)
    }

private val googleWalletImportCardNumberKeywords = setOf(
    "member",
    "membership",
    "card",
    "number",
    "no",
    "no.",
    "id"
)

private val googleWalletImportCodeKeywords = setOf(
    "barcode",
    "qr",
    "code"
)

private val googleWalletImportExpiryKeywords = setOf(
    "exp",
    "expires",
    "expiry",
    "valid",
    "until",
    "through"
)

private val googleWalletImportExcludedNameKeywords = setOf(
    "google wallet",
    "barcode",
    "qr",
    "code",
    "member",
    "membership",
    "card",
    "number",
    "share"
)

private val googleWalletImportLabelRegex =
    Regex("(?i)member(ship)?|card|number|no\\.?|id|barcode|qr|code")

private val googleWalletImportCardNumberPattern =
    Regex("(?i)[A-Z0-9][A-Z0-9\\-\\s]{3,23}")

private val googleWalletImportCodePattern =
    Regex("(?i)[A-Z0-9][A-Z0-9\\-\\s]{5,79}")

private val googleWalletImportDatePattern =
    Regex("(\\d{4}[-/]\\d{2}[-/]\\d{2})|(\\d{2}[-/]\\d{2}[-/]\\d{4})")
