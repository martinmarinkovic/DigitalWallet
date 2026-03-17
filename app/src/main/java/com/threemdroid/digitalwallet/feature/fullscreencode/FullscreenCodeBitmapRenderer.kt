package com.threemdroid.digitalwallet.feature.fullscreencode

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.threemdroid.digitalwallet.core.model.CardCodeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FullscreenCodeBitmapRenderer {
    suspend fun render(
        codeValue: String,
        codeType: CardCodeType,
        width: Int,
        height: Int
    ): Bitmap? =
        withContext(Dispatchers.Default) {
            runCatching {
                val bitMatrix = MultiFormatWriter().encode(
                    codeValue,
                    codeType.toBarcodeFormat(),
                    width,
                    height,
                    mapOf(EncodeHintType.MARGIN to 1)
                )
                bitMatrix.toBitmap()
            }.getOrNull()
        }
}

internal fun CardCodeType.toFullscreenPresentation(): FullscreenCodePresentation =
    when (this) {
        CardCodeType.QR_CODE,
        CardCodeType.AZTEC -> FullscreenCodePresentation.MATRIX

        CardCodeType.PDF_417,
        CardCodeType.CODE_128,
        CardCodeType.CODE_39,
        CardCodeType.EAN_13,
        CardCodeType.EAN_8,
        CardCodeType.UPC_A,
        CardCodeType.UPC_E,
        CardCodeType.ITF,
        CardCodeType.OTHER -> FullscreenCodePresentation.LINEAR
    }

private fun CardCodeType.toBarcodeFormat(): BarcodeFormat =
    when (this) {
        CardCodeType.QR_CODE -> BarcodeFormat.QR_CODE
        CardCodeType.AZTEC -> BarcodeFormat.AZTEC
        CardCodeType.PDF_417 -> BarcodeFormat.PDF_417
        CardCodeType.CODE_128 -> BarcodeFormat.CODE_128
        CardCodeType.CODE_39 -> BarcodeFormat.CODE_39
        CardCodeType.EAN_13 -> BarcodeFormat.EAN_13
        CardCodeType.EAN_8 -> BarcodeFormat.EAN_8
        CardCodeType.UPC_A -> BarcodeFormat.UPC_A
        CardCodeType.UPC_E -> BarcodeFormat.UPC_E
        CardCodeType.ITF -> BarcodeFormat.ITF
        CardCodeType.OTHER -> BarcodeFormat.CODE_128
    }

private fun BitMatrix.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[(y * width) + x] =
                if (this[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
