package dev.sanastasov.bybon.workout.ui.history

import android.content.ContentResolver
import android.net.Uri

fun interface ContentResolverReader {
    fun read(uri: Uri): String
}

class AndroidContentResolverReader(
    private val contentResolver: ContentResolver,
) : ContentResolverReader {

    override fun read(uri: Uri): String = contentResolver.openInputStream(uri)
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Unable to read CSV")
}
