package com.threemdroid.digitalwallet.core.model

enum class CardCodeType {
    QR_CODE,
    AZTEC,
    PDF_417,
    CODE_128,
    CODE_39,
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    ITF,
    OTHER
}

fun CardCodeType.displayLabel(): String =
    when (this) {
        CardCodeType.QR_CODE -> "QR Code"
        CardCodeType.AZTEC -> "Aztec"
        CardCodeType.PDF_417 -> "PDF417"
        CardCodeType.CODE_128 -> "Code 128"
        CardCodeType.CODE_39 -> "Code 39"
        CardCodeType.EAN_13 -> "EAN-13"
        CardCodeType.EAN_8 -> "EAN-8"
        CardCodeType.UPC_A -> "UPC-A"
        CardCodeType.UPC_E -> "UPC-E"
        CardCodeType.ITF -> "ITF"
        CardCodeType.OTHER -> "Other"
    }
