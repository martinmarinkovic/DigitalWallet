package com.threemdroid.digitalwallet.feature.addcard

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.threemdroid.digitalwallet.core.model.CardCodeType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

interface PhotoScanExtractor {
    suspend fun extractDetails(imageUri: Uri): PhotoScanExtractionResult
}

data class PhotoScanExtractionResult(
    val codeType: CardCodeType?,
    val codeValue: String?,
    val cardNumber: String?,
    val cardName: String?
)

class MlKitPhotoScanExtractor @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PhotoScanExtractor {
    private val barcodeScanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_PDF417,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_ITF
                )
                .build()
        )
    }

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun extractDetails(imageUri: Uri): PhotoScanExtractionResult {
        val inputImage = withContext(Dispatchers.IO) {
            InputImage.fromFilePath(context, imageUri)
        }
        val barcodes = barcodeScanner.process(inputImage).awaitResult()
        val recognizedText = textRecognizer.process(inputImage).awaitResult()

        return withContext(Dispatchers.Default) {
            val primaryBarcode = barcodes.firstOrNull { barcode ->
                !barcode.rawValue.isNullOrBlank() || !barcode.displayValue.isNullOrBlank()
            }
            val codeValue = primaryBarcode?.rawValue
                ?.trim()
                ?.takeIf { value -> value.isNotBlank() }
                ?: primaryBarcode?.displayValue
                    ?.trim()
                    ?.takeIf { value -> value.isNotBlank() }

            PhotoScanExtractionResult(
                codeType = primaryBarcode?.format.toCardCodeType(),
                codeValue = codeValue,
                cardNumber = recognizedText.extractCardNumber(),
                cardName = recognizedText.extractCardName()
            )
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { throwable ->
            continuation.resumeWithException(throwable)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }

private fun Text.extractCardNumber(): String? {
    val lines = allLinesSorted()
    val keywordCandidates = lines
        .filterNot { line -> line.containsAny(photoScanExpiryKeywords) }
        .filter { line -> line.containsAny(photoScanCardNumberKeywords) }
        .flatMap { line -> line.extractCardNumberCandidates() }
    val fallbackCandidates = lines
        .filterNot { line -> line.containsAny(photoScanExpiryKeywords) }
        .flatMap { line -> line.extractCardNumberCandidates() }

    return (keywordCandidates + fallbackCandidates)
        .map { candidate -> candidate.normalizePhotoScanCardNumber() }
        .filter { candidate -> candidate.isLikelyPhotoScanCardNumber() }
        .distinct()
        .firstOrNull()
}

private fun Text.extractCardName(): String? =
    allLinesSorted()
        .map { line -> line.trim() }
        .firstOrNull { line ->
            line.length in 3..40 &&
                line.none(Char::isDigit) &&
                line.count { character -> character.isLetter() } >= 3 &&
                line.wordCount() <= 4 &&
                !line.containsAny(photoScanNameExclusionKeywords)
        }

private fun Text.allLinesSorted(): List<String> =
    textBlocks
        .flatMap { block -> block.lines }
        .sortedBy { line -> line.boundingBox?.top ?: Int.MAX_VALUE }
        .map { line -> line.text.trim() }
        .filter { line -> line.isNotBlank() }

private fun String.extractCardNumberCandidates(): List<String> {
    val cleanedLine = replace(cardNumberLabelRegex, " ")
    val segments = buildList {
        add(cleanedLine)
        addAll(cleanedLine.split(':', '#').drop(1))
    }

    return segments.flatMap { segment ->
        photoScanCardNumberPattern.findAll(segment).map { match ->
            match.value
        }.toList()
    }
}

private fun String.normalizePhotoScanCardNumber(): String =
    replace(Regex("\\s+"), "")
        .trim('-')
        .trim()

private fun String.isLikelyPhotoScanCardNumber(): Boolean =
    length in 6..24 &&
        count { character -> character.isDigit() } >= 4 &&
        !matches(photoScanDatePattern)

private fun String.wordCount(): Int =
    split(Regex("\\s+"))
        .count { word -> word.isNotBlank() }

private fun String.containsAny(keywords: Set<String>): Boolean =
    keywords.any { keyword ->
        contains(keyword, ignoreCase = true)
    }

private fun Int?.toCardCodeType(): CardCodeType? =
    when (this) {
        Barcode.FORMAT_QR_CODE -> CardCodeType.QR_CODE
        Barcode.FORMAT_AZTEC -> CardCodeType.AZTEC
        Barcode.FORMAT_PDF417 -> CardCodeType.PDF_417
        Barcode.FORMAT_CODE_128 -> CardCodeType.CODE_128
        Barcode.FORMAT_CODE_39 -> CardCodeType.CODE_39
        Barcode.FORMAT_EAN_13 -> CardCodeType.EAN_13
        Barcode.FORMAT_EAN_8 -> CardCodeType.EAN_8
        Barcode.FORMAT_UPC_A -> CardCodeType.UPC_A
        Barcode.FORMAT_UPC_E -> CardCodeType.UPC_E
        Barcode.FORMAT_ITF -> CardCodeType.ITF
        else -> null
    }

private val photoScanCardNumberKeywords = setOf(
    "member",
    "membership",
    "card",
    "number",
    "no",
    "no.",
    "id"
)

private val photoScanExpiryKeywords = setOf(
    "exp",
    "expires",
    "expiry",
    "valid",
    "until",
    "through"
)

private val photoScanNameExclusionKeywords = setOf(
    "member",
    "membership",
    "card",
    "number",
    "barcode",
    "qr",
    "code",
    "valid",
    "expires",
    "expiry"
)

private val cardNumberLabelRegex =
    Regex("(?i)member(ship)?|card|number|no\\.?|id")

private val photoScanCardNumberPattern =
    Regex("(?i)[A-Z0-9][A-Z0-9\\-\\s]{5,23}")

private val photoScanDatePattern =
    Regex("(\\d{4}[-/]\\d{2}[-/]\\d{2})|(\\d{2}[-/]\\d{2}[-/]\\d{4})")
