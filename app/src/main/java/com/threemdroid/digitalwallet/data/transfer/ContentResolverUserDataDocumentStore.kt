package com.threemdroid.digitalwallet.data.transfer

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ContentResolverUserDataDocumentStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UserDataDocumentStore {

    override suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            reader.readText()
        } ?: throw IllegalStateException("Unable to open the selected file.")
    }

    override suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(text)
        } ?: throw IllegalStateException("Unable to write the selected file.")
    }
}
