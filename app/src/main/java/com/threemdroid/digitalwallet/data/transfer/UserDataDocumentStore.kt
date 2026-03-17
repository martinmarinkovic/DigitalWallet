package com.threemdroid.digitalwallet.data.transfer

import android.net.Uri

interface UserDataDocumentStore {
    suspend fun readText(uri: Uri): String

    suspend fun writeText(uri: Uri, text: String)
}
